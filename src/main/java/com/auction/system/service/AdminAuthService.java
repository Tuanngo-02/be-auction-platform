package com.auction.system.service;

import com.auction.system.domain.Authority;
import com.auction.system.domain.User;
import com.auction.system.repository.UserRepository;
import com.auction.system.security.SecurityUtils;
import com.auction.system.service.dto.AdminActivationRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.*;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminAuthService {

    private static final String ENTITY_NAME = "adminAuth";
    private static final String ACTIVATION_PREFIX = "admin:activation:";
    private static final Duration ACTIVATION_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;

    public AdminAuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        RedissonClient redissonClient,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redissonClient = redissonClient;
        this.cacheManager = cacheManager;
    }

    public String createActivationToken(Long userId) {
        String token = "act_" + UUID.randomUUID().toString().replace("-", "");
        RBucket<Long> bucket = redissonClient.getBucket(ACTIVATION_PREFIX + token);
        bucket.set(userId, ACTIVATION_TTL);
        return token;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyActivationToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestServiceException("Token kích hoạt không hợp lệ", ENTITY_NAME, "invalidtoken");
        }
        RBucket<Long> bucket = redissonClient.getBucket(ACTIVATION_PREFIX + token.trim());
        Long userId = bucket.get();
        if (userId == null) {
            throw new BadRequestServiceException("Token kích hoạt đã hết hạn hoặc đã được sử dụng", ENTITY_NAME, "expiredtoken");
        }
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản Admin không tồn tại", ENTITY_NAME, "usernotfound"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName() != null ? user.getFullName() : user.getLogin());
        data.put("valid", true);
        return data;
    }

    public Map<String, Object> activateAdminAccount(AdminActivationRequestDTO req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestServiceException("Mật khẩu xác nhận không khớp", ENTITY_NAME, "passwordmismatch");
        }
        RBucket<Long> bucket = redissonClient.getBucket(ACTIVATION_PREFIX + req.getActivationToken().trim());
        Long userId = bucket.getAndDelete();
        if (userId == null) {
            throw new BadRequestServiceException("Token kích hoạt đã hết hạn hoặc đã được sử dụng", ENTITY_NAME, "expiredtoken");
        }
        User user = userRepository
            .findById(userId)
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản Admin không tồn tại", ENTITY_NAME, "usernotfound"));

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setAccountStatus("ACTIVE");
        user.setActivated(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        if (cacheManager != null) {
            if (user.getLogin() != null && cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE) != null) {
                cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).evictIfPresent(user.getLogin());
            }
            if (user.getEmail() != null && cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE) != null) {
                cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).evictIfPresent(user.getEmail());
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("accountStatus", user.getAccountStatus());
        return data;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMyPermissions() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestServiceException("Chưa đăng nhập", ENTITY_NAME, "unauthorized"));
        User user = userRepository
            .findOneWithAuthoritiesByLogin(login)
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản không tồn tại", ENTITY_NAME, "usernotfound"));

        List<String> authorityNames = user.getAuthorities().stream().map(Authority::getName).sorted().toList();
        List<String> roles = authorityNames
            .stream()
            .map(name -> name.startsWith("ROLE_") ? name.substring(5) : name)
            .toList();

        List<String> permissions = authorityNames.isEmpty()
            ? List.of()
            : entityManager
                  .createNativeQuery(
                      "SELECT DISTINCT p.permission_name " +
                      "FROM role_permission rp " +
                      "JOIN permission p ON p.id = rp.permission_id " +
                      "WHERE rp.role_name IN (:roles) " +
                      "ORDER BY p.permission_name"
                  )
                  .setParameter("roles", authorityNames)
                  .getResultList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("roles", roles);
        data.put("permissions", permissions);
        return data;
    }
}
