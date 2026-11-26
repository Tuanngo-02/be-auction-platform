package com.auction.system.service;

import com.auction.system.domain.Address;
import com.auction.system.domain.Authority;
import com.auction.system.domain.LoginSession;
import com.auction.system.domain.User;
import com.auction.system.repository.AddressRepository;
import com.auction.system.repository.LoginSessionRepository;
import com.auction.system.repository.UserRepository;
import com.auction.system.security.SecurityUtils;
import com.auction.system.service.dto.*;
import com.auction.system.web.rest.errors.BadRequestAlertException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private static final String ENTITY_NAME = "userProfile";

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final LoginSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    public UserProfileService(
        UserRepository userRepository,
        AddressRepository addressRepository,
        LoginSessionRepository sessionRepository,
        PasswordEncoder passwordEncoder,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheManager = cacheManager;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile() {
        User user = getCurrentUser();
        UserProfileDTO dto = new UserProfileDTO();
        dto.setUserId(user.getId());
        dto.setFullName(user.getFullName() != null ? user.getFullName() : ((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "")).trim());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setBirthDate(user.getBirthDate());
        dto.setReputation(user.getReputation() != null ? user.getReputation() : "5.0");
        dto.setSellerStatus(user.getSellerVerificationStatus() != null ? user.getSellerVerificationStatus() : "UNVERIFIED");
        dto.setAccountStatus(user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
        dto.setEmailVerified(Boolean.TRUE.equals(user.getEmailVerified()) || user.isActivated());
        dto.setRoles(user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()));
        return dto;
    }

    public UserProfileDTO updateMyProfile(UpdateProfileDTO req) {
        User user = getCurrentUser();
        user.setFullName(req.getFullName().trim());
        String[] parts = req.getFullName().trim().split("\\s+", 2);
        user.setFirstName(parts[0]);
        user.setLastName(parts.length > 1 ? parts[1] : "");
        user.setBirthDate(req.getBirthDate());
        userRepository.save(user);
        clearUserCaches(user);
        return getMyProfile();
    }

    public void changePassword(PasswordChangeDTO req) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestAlertException("Mật khẩu hiện tại không đúng", ENTITY_NAME, "wrongpassword");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        clearUserCaches(user);
    }

    @Transactional(readOnly = true)
    public List<LoginSessionDTO> getActiveSessions() {
        User user = getCurrentUser();
        List<LoginSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<LoginSessionDTO> result = new ArrayList<>();
        for (int i = 0; i < sessions.size(); i++) {
            LoginSession s = sessions.get(i);
            LoginSessionDTO dto = new LoginSessionDTO();
            dto.setSessionId(s.getId());
            dto.setDeviceInfo(s.getDeviceInfo());
            dto.setIpAddress(s.getIpAddress());
            dto.setCurrent(i == 0);
            dto.setCreatedAt(s.getCreatedAt());
            result.add(dto);
        }
        return result;
    }

    public void revokeSession(Long sessionId) {
        User user = getCurrentUser();
        LoginSession session = sessionRepository
            .findByIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new BadRequestAlertException("Phiên đăng nhập không tồn tại", ENTITY_NAME, "sessionnotfound"));
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<AddressDTO> getUserAddresses() {
        User user = getCurrentUser();
        return addressRepository
            .findByUserIdOrderByIsDefaultDescIdDesc(user.getId())
            .stream()
            .map(a -> {
                AddressDTO dto = new AddressDTO();
                dto.setAddressId(a.getId());
                dto.setAddressText(a.getAddressText());
                dto.setIsDefault(a.getIsDefault());
                return dto;
            })
            .collect(Collectors.toList());
    }

    public AddressDTO createUserAddress(AddressDTO req) {
        User user = getCurrentUser();
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            List<Address> existing = addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(user.getId());
            for (Address addr : existing) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }
        Address address = new Address();
        address.setUserId(user.getId());
        address.setAddressText(req.getAddressText().trim());
        address.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        Address saved = addressRepository.save(address);

        AddressDTO res = new AddressDTO();
        res.setAddressId(saved.getId());
        res.setAddressText(saved.getAddressText());
        res.setIsDefault(saved.getIsDefault());
        return res;
    }

    public void deleteUserAddress(Long addressId) {
        User user = getCurrentUser();
        Address address = addressRepository
            .findByIdAndUserId(addressId, user.getId())
            .orElseThrow(() -> new BadRequestAlertException("Địa chỉ không tồn tại", ENTITY_NAME, "addressnotfound"));
        addressRepository.delete(address);
    }

    public Map<String, Object> submitSellerVerification(SellerVerificationDTO req) {
        User user = getCurrentUser();
        user.setIdCardNumber(req.getIdCardNumber().trim());
        user.setIdCardFrontUrl(req.getIdCardFrontUrl().trim());
        user.setIdCardBackUrl(req.getIdCardBackUrl().trim());
        user.setSellerVerificationStatus("PENDING");
        userRepository.save(user);
        clearUserCaches(user);
        return Map.of("status", "PENDING");
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evictIfPresent(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evictIfPresent(user.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyActivities(int page, int size) {
        User user = getCurrentUser();
        List<LoginSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<Map<String, Object>> items = sessions
            .stream()
            .skip((long) page * size)
            .limit(size)
            .map(s -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("activityId", s.getId());
                item.put("action", "LOGIN");
                item.put("deviceInfo", s.getDeviceInfo());
                item.put("timestamp", s.getCreatedAt());
                return item;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", sessions.size());
        return result;
    }

    @Transactional(readOnly = true)
    public SellerPublicProfileDTO getSellerPublicProfile(Long sellerId) {
        User seller = userRepository
            .findById(sellerId)
            .orElseThrow(() -> new BadRequestAlertException("Người bán không tồn tại", ENTITY_NAME, "sellernotfound"));
        SellerPublicProfileDTO dto = new SellerPublicProfileDTO();
        dto.setSellerId(seller.getId());
        dto.setFullName(seller.getFullName() != null ? seller.getFullName() : seller.getLogin());
        dto.setReputation(seller.getReputation() != null ? seller.getReputation() : "5.0");
        dto.setTotalAuctions(0);
        dto.setVerified("VERIFIED".equalsIgnoreCase(seller.getSellerVerificationStatus()));
        return dto;
    }

    private User getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("Chưa đăng nhập", ENTITY_NAME, "unauthorized"));
        return userRepository
            .findOneWithAuthoritiesByLogin(login)
            .orElseThrow(() -> new BadRequestAlertException("Tài khoản không tồn tại", ENTITY_NAME, "usernotfound"));
    }
}
