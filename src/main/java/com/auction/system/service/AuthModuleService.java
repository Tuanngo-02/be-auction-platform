package com.auction.system.service;

import static com.auction.system.security.SecurityUtils.AUTHORITIES_CLAIM;
import static com.auction.system.security.SecurityUtils.JWT_ALGORITHM;
import static com.auction.system.security.SecurityUtils.USER_ID_CLAIM;

import com.auction.system.domain.Authority;
import com.auction.system.domain.LoginSession;
import com.auction.system.domain.OtpVerification;
import com.auction.system.domain.User;
import com.auction.system.repository.AuthorityRepository;
import com.auction.system.repository.LoginSessionRepository;
import com.auction.system.repository.OtpVerificationRepository;
import com.auction.system.repository.UserRepository;
import com.auction.system.security.AuthoritiesConstants;
import com.auction.system.security.DomainUserDetailsService.UserWithId;
import com.auction.system.service.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthModuleService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthModuleService.class);
    private static final String ENTITY_NAME = "auth";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final OtpVerificationRepository otpRepository;
    private final LoginSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MailService mailService;
    private final CacheManager cacheManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:86400}")
    private long tokenValidityInSeconds;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public AuthModuleService(
        UserRepository userRepository,
        AuthorityRepository authorityRepository,
        OtpVerificationRepository otpRepository,
        LoginSessionRepository sessionRepository,
        PasswordEncoder passwordEncoder,
        JwtEncoder jwtEncoder,
        AuthenticationManagerBuilder authenticationManagerBuilder,
        MailService mailService,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.authorityRepository = authorityRepository;
        this.otpRepository = otpRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.mailService = mailService;
        this.cacheManager = cacheManager;
    }

    public Map<String, Object> register(RegisterRequestDTO req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findOneByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestServiceException("Email đã được sử dụng", ENTITY_NAME, "emailexists");
        }
        if (userRepository.findOneByPhone(req.getPhone().trim()).isPresent()) {
            throw new BadRequestServiceException("Số điện thoại đã được sử dụng", ENTITY_NAME, "phoneexists");
        }

        User user = new User();
        user.setLogin(email);
        user.setEmail(email);
        user.setFullName(req.getFullName().trim());
        // Split full name into first/last name for JHipster compatibility
        String[] parts = req.getFullName().trim().split("\\s+", 2);
        user.setFirstName(parts[0]);
        user.setLastName(parts.length > 1 ? parts[1] : "");
        user.setPhone(req.getPhone().trim());
        user.setBirthDate(req.getBirthDate());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setActivated(false);
        user.setEmailVerified(false);
        user.setAccountStatus("ACTIVE");
        user.setSellerVerificationStatus("UNVERIFIED");
        user.setReputation("5.0");
        user.setLangKey("vi");

        Set<Authority> authorities = new HashSet<>();
        authorityRepository.findById(AuthoritiesConstants.BIDDER).ifPresent(authorities::add);
        if ("SELLER".equalsIgnoreCase(req.getRole())) {
            authorityRepository.findById(AuthoritiesConstants.SELLER).ifPresent(authorities::add);
        }
        user.setAuthorities(authorities);

        User saved = userRepository.save(user);

        // Khởi tạo ví nội bộ mặc định cho người dùng mới
        entityManager
            .createNativeQuery("INSERT INTO wallet (user_id, coin_balance) VALUES (:uid, 0)")
            .setParameter("uid", saved.getId())
            .executeUpdate();

        String otpCode = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setOtpType("REGISTER_EMAIL");
        otp.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        LOG.info("[DEV OTP] Mã xác thực đăng ký cho {} là: {}", email, otpCode);
        sendOtpEmailIfConfigured(
            email,
            "[AuctionHub] Mã OTP xác thực đăng ký tài khoản",
            "Xin chào " + saved.getFullName() + ",\n\nMã OTP xác thực email của bạn là: " + otpCode + "\nMã có hiệu lực trong 5 phút."
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", saved.getId());
        result.put("email", saved.getEmail());
        result.put("emailVerified", false);
        return result;
    }

    public Map<String, Object> verifyEmailOtp(VerifyOtpRequestDTO req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        OtpVerification otp = otpRepository
            .findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(email, "REGISTER_EMAIL")
            .orElseThrow(() -> new BadRequestServiceException("Mã OTP không tồn tại hoặc đã hết hạn", ENTITY_NAME, "otpinvalid"));

        if (otp.getExpiresAt().isBefore(Instant.now()) || !otp.getOtpCode().equals(req.getOtpCode().trim())) {
            throw new BadRequestServiceException("Mã OTP không đúng hoặc đã hết hạn", ENTITY_NAME, "otpinvalid");
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        User user = userRepository
            .findOneByEmailIgnoreCase(email)
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản không tồn tại", ENTITY_NAME, "usernotfound"));
        user.setActivated(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        clearUserCaches(user);

        return Map.of("emailVerified", true);
    }

    public Map<String, Object> resendVerifyEmailOtp(String emailOrPhone) {
        if (emailOrPhone == null || emailOrPhone.isBlank()) {
            throw new BadRequestServiceException("Vui lòng nhập email hoặc số điện thoại", ENTITY_NAME, "emailrequired");
        }
        String identifier = emailOrPhone.trim();
        User user = userRepository
            .findOneByEmailIgnoreCase(identifier)
            .or(() -> userRepository.findOneByLogin(identifier.toLowerCase(Locale.ROOT)))
            .or(() -> userRepository.findOneByPhone(identifier))
            .orElseThrow(() -> new BadRequestServiceException("Tài khoản không tồn tại", ENTITY_NAME, "usernotfound"));

        if (user.isActivated()) {
            throw new BadRequestServiceException("Email đã được xác minh trước đó", ENTITY_NAME, "alreadyverified");
        }

        String otpCode = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setEmail(user.getEmail());
        otp.setOtpCode(otpCode);
        otp.setOtpType("REGISTER_EMAIL");
        otp.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        LOG.info("[DEV OTP] Gửi lại mã xác thực đăng ký cho {} là: {}", user.getEmail(), otpCode);
        sendOtpEmailIfConfigured(
            user.getEmail(),
            "[AuctionHub] Mã OTP xác thực đăng ký tài khoản",
            "Xin chào " +
            (user.getFullName() != null ? user.getFullName() : user.getLogin()) +
            ",\n\nMã OTP xác thực email của bạn là: " +
            otpCode +
            "\nMã có hiệu lực trong 5 phút."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", user.getEmail());
        data.put("otpExpiresIn", 300);
        return data;
    }

    public AuthTokenDTO login(LoginRequestDTO req, HttpServletRequest httpReq) {
        String identifier = req.getEmailOrPhone().trim();
        userRepository
            .findOneByEmailIgnoreCase(identifier)
            .or(() -> userRepository.findOneByLogin(identifier.toLowerCase(Locale.ROOT)))
            .or(() -> userRepository.findOneByPhone(identifier))
            .ifPresent(u -> {
                if (!u.isActivated() && passwordEncoder.matches(req.getPassword(), u.getPassword())) {
                    throw new BadRequestServiceException(
                        "Email chưa được xác minh, hãy xác minh trước khi đăng nhập",
                        ENTITY_NAME,
                        "emailnotverified"
                    );
                }
            });

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(identifier, req.getPassword());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Long userId = null;
        if (authentication.getPrincipal() instanceof UserWithId userWithId) {
            userId = userWithId.getId();
        }
        String accessToken = createJwtToken(authentication, userId);
        String refreshToken = UUID.randomUUID().toString();

        if (userId != null) {
            LoginSession session = new LoginSession();
            session.setUserId(userId);
            String userAgent = httpReq != null ? httpReq.getHeader("User-Agent") : "Unknown";
            session.setDeviceInfo(userAgent != null && userAgent.length() > 250 ? userAgent.substring(0, 250) : userAgent);
            session.setIpAddress(httpReq != null ? httpReq.getRemoteAddr() : "127.0.0.1");
            session.setRefreshToken(refreshToken);
            session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
            sessionRepository.save(session);

            entityManager
                .createNativeQuery("INSERT INTO audit_log (actor_id, action, action_time) VALUES (:uid, 'LOGIN', CURRENT_TIMESTAMP)")
                .setParameter("uid", userId)
                .executeUpdate();
        }

        return new AuthTokenDTO(accessToken, refreshToken, tokenValidityInSeconds);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestServiceException("Refresh token không hợp lệ", ENTITY_NAME, "invalidrefreshtoken");
        }
        LoginSession session = sessionRepository
            .findByRefreshToken(refreshToken)
            .orElseThrow(() -> new BadRequestServiceException("Refresh token không hợp lệ", ENTITY_NAME, "invalidrefreshtoken"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            sessionRepository.delete(session);
            throw new BadRequestServiceException("Refresh token đã hết hạn", ENTITY_NAME, "expiredrefreshtoken");
        }

        User user = userRepository
            .findById(session.getUserId())
            .orElseThrow(() -> new BadRequestServiceException("User không tồn tại", ENTITY_NAME, "usernotfound"));

        if (!user.isActivated() || "LOCKED".equalsIgnoreCase(user.getAccountStatus()) || "SUSPENDED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BadRequestServiceException("Tài khoản đã bị khóa hoặc chưa kích hoạt", ENTITY_NAME, "accountinactive");
        }

        List<GrantedAuthority> authorities = user
            .getAuthorities()
            .stream()
            .map(a -> new SimpleGrantedAuthority(a.getName()))
            .collect(Collectors.toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(user.getLogin(), null, authorities);
        String newAccessToken = createJwtToken(auth, user.getId());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("expiresIn", tokenValidityInSeconds);
        return data;
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionRepository
                .findByRefreshToken(refreshToken)
                .ifPresent(session -> {
                    entityManager
                        .createNativeQuery("INSERT INTO audit_log (actor_id, action, action_time) VALUES (:uid, 'LOGOUT', CURRENT_TIMESTAMP)")
                        .setParameter("uid", session.getUserId())
                        .executeUpdate();
                    sessionRepository.delete(session);
                });
        }
    }

    public Map<String, Object> forgotPasswordSendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestServiceException("Vui lòng nhập email", ENTITY_NAME, "emailrequired");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        userRepository
            .findOneByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() -> new BadRequestServiceException("Email chưa được đăng ký", ENTITY_NAME, "emailnotfound"));

        String otpCode = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setEmail(normalizedEmail);
        otp.setOtpCode(otpCode);
        otp.setOtpType("FORGOT_PASSWORD");
        otp.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        otpRepository.save(otp);

        LOG.info("[DEV OTP] Mã OTP khôi phục mật khẩu cho {} là: {}", normalizedEmail, otpCode);
        sendOtpEmailIfConfigured(
            normalizedEmail,
            "[AuctionHub] Mã OTP khôi phục mật khẩu",
            "Xin chào,\n\nMã OTP khôi phục mật khẩu của bạn là: " + otpCode + "\nMã có hiệu lực trong 5 phút."
        );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", normalizedEmail);
        data.put("otpExpiresIn", 300);
        return data;
    }

    public Map<String, Object> forgotPasswordVerifyOtp(VerifyOtpRequestDTO req) {
        String email = req.getEmail().trim().toLowerCase(Locale.ROOT);
        OtpVerification otp = otpRepository
            .findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(email, "FORGOT_PASSWORD")
            .orElseThrow(() -> new BadRequestServiceException("Mã OTP không tồn tại", ENTITY_NAME, "otpinvalid"));

        if (otp.getExpiresAt().isBefore(Instant.now()) || !otp.getOtpCode().equals(req.getOtpCode().trim())) {
            throw new BadRequestServiceException("Mã OTP không đúng hoặc đã hết hạn", ENTITY_NAME, "otpinvalid");
        }

        String resetToken = "rst_" + UUID.randomUUID().toString().replace("-", "");
        otp.setResetToken(resetToken);
        otpRepository.save(otp);

        return Map.of("resetToken", resetToken);
    }

    public void resetPassword(ResetPasswordRequestDTO req) {
        OtpVerification otp = otpRepository
            .findByResetTokenAndUsedFalse(req.getResetToken())
            .orElseThrow(() ->
                new BadRequestServiceException("Reset token không hợp lệ hoặc đã sử dụng", ENTITY_NAME, "invalidresettoken")
            );

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestServiceException("Reset token đã hết hạn", ENTITY_NAME, "expiredresettoken");
        }

        User user = userRepository
            .findOneByEmailIgnoreCase(otp.getEmail())
            .orElseThrow(() -> new BadRequestServiceException("User không tồn tại", ENTITY_NAME, "usernotfound"));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        clearUserCaches(user);

        otp.setUsed(true);
        otpRepository.save(otp);
    }

    private String createJwtToken(Authentication authentication, Long userId) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        Instant now = Instant.now();
        Instant validity = now.plus(tokenValidityInSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(authentication.getName())
            .claim(AUTHORITIES_CLAIM, authorities);
        if (userId != null) {
            builder.claim(USER_ID_CLAIM, userId);
        }

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, builder.build())).getTokenValue();
    }

    private void clearUserCaches(User user) {
        if (cacheManager == null) {
            return;
        }
        if (user.getLogin() != null && cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE) != null) {
            cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).evictIfPresent(user.getLogin());
        }
        if (user.getEmail() != null && cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE) != null) {
            cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).evictIfPresent(user.getEmail());
        }
    }

    private void sendOtpEmailIfConfigured(String to, String subject, String content) {
        if (mailUsername == null || mailUsername.isBlank()) {
            LOG.warn("SPRING_MAIL_USERNAME chưa được cấu hình, bỏ qua gửi email OTP thật tới {}", to);
            return;
        }
        mailService.sendEmail(to, subject, content, false, false);
    }

    private String generateOtp() {
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
