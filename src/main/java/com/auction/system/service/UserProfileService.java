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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        dto.setFullName(
            user.getFullName() != null
                ? user.getFullName()
                : (
                      (user.getFirstName() != null ? user.getFirstName() : "") +
                      " " +
                      (user.getLastName() != null ? user.getLastName() : "")
                  ).trim()
        );
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setBirthDate(user.getBirthDate());
        dto.setReputation(user.getReputation() != null ? user.getReputation() : "5.0");
        dto.setSellerStatus(user.getSellerVerificationStatus() != null ? user.getSellerVerificationStatus() : "UNVERIFIED");
        dto.setAccountStatus(user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");
        dto.setEmailVerified(Boolean.TRUE.equals(user.getEmailVerified()) || user.isActivated());
        dto.setRoles(user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()));

        if (user.getIdCardNumber() != null && user.getIdCardNumber().length() >= 6) {
            dto.setMaskedIdCard(user.getIdCardNumber().substring(0, 6) + "******");
        }

        // Lấy số dư ví và tiền cọc đang phong tỏa từ CSDL
        dto.setAvailableBalance(queryDecimal("SELECT COALESCE(SUM(coin_balance), 0) FROM wallet WHERE user_id = :uid", user.getId()));
        dto.setLockedDeposit(
            queryDecimal("SELECT COALESCE(SUM(amount), 0) FROM deposit WHERE bidder_id = :uid AND status = 'LOCKED'", user.getId())
        );

        // Lấy số lượng phiên tham gia, theo dõi, đơn hàng từ CSDL
        dto.setParticipatedCount(queryCount("SELECT COUNT(*) FROM auction_participation WHERE bidder_id = :uid", user.getId()));
        dto.setWatchlistCount(queryCount("SELECT COUNT(*) FROM watchlist WHERE user_id = :uid", user.getId()));
        dto.setOrdersCount(
            queryCount(
                "SELECT COUNT(*) FROM orders o JOIN auction_result ar ON o.auction_id = ar.auction_id " +
                "JOIN bid b ON ar.winning_bid_id = b.id WHERE b.bidder_id = :uid",
                user.getId()
            )
        );

        // Lấy địa chỉ mặc định từ bảng address
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescIdDesc(user.getId());
        if (!addresses.isEmpty()) {
            dto.setDefaultAddress(addresses.get(0).getAddressText());
        }

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
        recordAuditLog(user.getId(), "UPDATE_PROFILE");
        return getMyProfile();
    }

    public void changePassword(PasswordChangeDTO req) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestServiceException("Mật khẩu hiện tại không đúng", ENTITY_NAME, "wrongpassword");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        clearUserCaches(user);
        recordAuditLog(user.getId(), "CHANGE_PASSWORD");
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
            .orElseThrow(() -> new BadRequestServiceException("Phiên đăng nhập không tồn tại", ENTITY_NAME, "sessionnotfound"));
        sessionRepository.delete(session);
        recordAuditLog(user.getId(), "REVOKE_SESSION");
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
        recordAuditLog(user.getId(), "ADD_ADDRESS");

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
            .orElseThrow(() -> new BadRequestServiceException("Địa chỉ không tồn tại", ENTITY_NAME, "addressnotfound"));
        addressRepository.delete(address);
        recordAuditLog(user.getId(), "DELETE_ADDRESS");
    }

    public Map<String, Object> submitSellerVerification(SellerVerificationDTO req) {
        User user = getCurrentUser();
        user.setIdCardNumber(req.getIdCardNumber().trim());
        user.setIdCardFrontUrl(req.getIdCardFrontUrl().trim());
        user.setIdCardBackUrl(req.getIdCardBackUrl().trim());
        user.setSellerVerificationStatus("PENDING");
        userRepository.save(user);
        clearUserCaches(user);
        recordAuditLog(user.getId(), "SUBMIT_SELLER_VERIFICATION");
        return Map.of("status", "PENDING");
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMyActivities(int page, int size) {
        User user = getCurrentUser();
        List<Object[]> rows = entityManager
            .createNativeQuery("SELECT id, action, action_time FROM audit_log WHERE actor_id = :uid ORDER BY action_time DESC")
            .setParameter("uid", user.getId())
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();

        long total = queryCount("SELECT COUNT(*) FROM audit_log WHERE actor_id = :uid", user.getId());
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("activityId", ((Number) r[0]).longValue());
            item.put("action", r[1]);
            item.put("timestamp", r[2]);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        return result;
    }

    @Transactional(readOnly = true)
    public SellerPublicProfileDTO getSellerPublicProfile(Long sellerId) {
        User seller = userRepository
            .findById(sellerId)
            .orElseThrow(() -> new BadRequestServiceException("Người bán không tồn tại", ENTITY_NAME, "sellernotfound"));
        long totalAuctions = queryCount(
            "SELECT COUNT(*) FROM auction a JOIN product p ON a.product_id = p.id WHERE p.seller_id = :uid",
            sellerId
        );
        SellerPublicProfileDTO dto = new SellerPublicProfileDTO();
        dto.setSellerId(seller.getId());
        dto.setFullName(seller.getFullName() != null ? seller.getFullName() : seller.getLogin());
        dto.setReputation(seller.getReputation() != null ? seller.getReputation() : "5.0");
        dto.setTotalAuctions(totalAuctions);
        dto.setVerified("VERIFIED".equalsIgnoreCase(seller.getSellerVerificationStatus()));
        return dto;
    }

    private BigDecimal queryDecimal(String sql, Long userId) {
        Object res = entityManager.createNativeQuery(sql).setParameter("uid", userId).getSingleResult();
        if (res instanceof BigDecimal bd) return bd;
        if (res instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private long queryCount(String sql, Long userId) {
        Object res = entityManager.createNativeQuery(sql).setParameter("uid", userId).getSingleResult();
        return res instanceof Number n ? n.longValue() : 0L;
    }

    private void recordAuditLog(Long userId, String action) {
        entityManager
            .createNativeQuery("INSERT INTO audit_log (actor_id, action, action_time) VALUES (:uid, :act, CURRENT_TIMESTAMP)")
            .setParameter("uid", userId)
            .setParameter("act", action)
            .executeUpdate();
    }

    private void clearUserCaches(User user) {
        Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE)).evictIfPresent(user.getLogin());
        if (user.getEmail() != null) {
            Objects.requireNonNull(cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE)).evictIfPresent(user.getEmail());
        }
    }

    private User getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() ->
            new BadRequestServiceException("Chưa đăng nhập", ENTITY_NAME, "unauthorized")
        );
        return userRepository
            .findOneWithAuthoritiesByLogin(login)
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản không tồn tại", ENTITY_NAME, "usernotfound"));
    }
}
