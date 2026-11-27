package com.auction.system.web.rest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.auction.system.IntegrationTest;
import com.auction.system.domain.Authority;
import com.auction.system.domain.OtpVerification;
import com.auction.system.domain.User;
import com.auction.system.repository.AuthorityRepository;
import com.auction.system.repository.OtpVerificationRepository;
import com.auction.system.repository.UserRepository;
import com.auction.system.security.AuthoritiesConstants;
import com.auction.system.service.AdminAuthService;
import com.auction.system.service.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
class AuthAndAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @Transactional
    void testAdminActivationFlowAndPermissions() throws Exception {
        Authority auctionAdminRole = authorityRepository
            .findById(AuthoritiesConstants.AUCTION_ADMIN)
            .orElseGet(() -> {
                Authority a = new Authority();
                a.setName(AuthoritiesConstants.AUCTION_ADMIN);
                return authorityRepository.save(a);
            });

        User pendingAdmin = new User();
        pendingAdmin.setLogin("pending_admin_it");
        pendingAdmin.setEmail("pending.admin.it@auctionhub.vn");
        pendingAdmin.setFullName("Admin Kiểm Duyệt IT");
        pendingAdmin.setPassword(passwordEncoder.encode("TempPass123!"));
        pendingAdmin.setActivated(false);
        pendingAdmin.setAccountStatus("PENDING_ACTIVATION");
        pendingAdmin.setAuthorities(Set.of(auctionAdminRole));
        pendingAdmin = userRepository.saveAndFlush(pendingAdmin);

        String token = adminAuthService.createActivationToken(pendingAdmin.getId());

        // 1. Verify activation token (Public)
        mockMvc
            .perform(get("/api/admin/auth/activation").param("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("pending.admin.it@auctionhub.vn"))
            .andExpect(jsonPath("$.data.valid").value(true));

        // 2. Activate admin account (Public)
        AdminActivationRequestDTO req = new AdminActivationRequestDTO();
        req.setActivationToken(token);
        req.setNewPassword("AdminNew@123");
        req.setConfirmPassword("AdminNew@123");

        mockMvc
            .perform(post("/api/admin/auth/activate").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));

        // 3. Token cannot be reused
        mockMvc
            .perform(get("/api/admin/auth/activation").param("token", token))
            .andExpect(status().isBadRequest());

        // 4. GET /api/me/permissions returns roles & permissions
        mockMvc
            .perform(get("/api/me/permissions").with(user(pendingAdmin.getLogin()).roles("AUCTION_ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roles[0]").value("AUCTION_ADMIN"))
            .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    @Transactional
    void testRbacAndRoleHierarchy() throws Exception {
        // Unauthenticated cannot access protected endpoints
        mockMvc.perform(get("/api/me/permissions")).andExpect(status().isUnauthorized());

        // Bidder cannot access admin endpoints
        mockMvc.perform(get("/api/admin/users").with(user("bidder_test").authorities(() -> "ROLE_BIDDER"))).andExpect(status().isForbidden());

        // SUPER_ADMIN inherits ROLE_ADMIN via RoleHierarchy
        mockMvc
            .perform(get("/api/admin/users").with(user("superadmin_test").authorities(() -> "ROLE_SUPER_ADMIN")))
            .andExpect(status().isOk());

        // Seller cannot submit seller-verification (only USER / BIDDER)
        mockMvc
            .perform(
                post("/api/users/me/seller-verification")
                    .with(user("seller_test").authorities(() -> "ROLE_SELLER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"idCardNumber\":\"048200001111\",\"idCardFrontUrl\":\"https://example.com/front.jpg\",\"idCardBackUrl\":\"https://example.com/back.jpg\"}")
            )
            .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void testRefreshTokenLogoutAndForgotPasswordFlow() throws Exception {
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setFullName("Auth Flow User");
        reg.setEmail("authflow@auctionhub.vn");
        reg.setPhone("0905777666");
        reg.setPassword("Password123!");

        mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(reg)))
            .andExpect(status().isCreated());

        // Login before OTP verify -> error.emailnotverified
        LoginRequestDTO unverifiedLogin = new LoginRequestDTO();
        unverifiedLogin.setEmailOrPhone("authflow@auctionhub.vn");
        unverifiedLogin.setPassword("Password123!");
        mockMvc
            .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(unverifiedLogin)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.emailnotverified"));

        // Resend verify email OTP
        mockMvc
            .perform(
                post("/api/auth/resend-verify-email-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(Map.of("emailOrPhone", "0905777666")))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value("authflow@auctionhub.vn"));

        OtpVerification regOtp = otpRepository
            .findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc("authflow@auctionhub.vn", "REGISTER_EMAIL")
            .orElseThrow();
        VerifyOtpRequestDTO verifyReq = new VerifyOtpRequestDTO();
        verifyReq.setEmail("authflow@auctionhub.vn");
        verifyReq.setOtpCode(regOtp.getOtpCode());

        mockMvc
            .perform(
                post("/api/auth/verify-email-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(verifyReq))
            )
            .andExpect(status().isOk());

        LoginRequestDTO loginReq = new LoginRequestDTO();
        loginReq.setEmailOrPhone("authflow@auctionhub.vn");
        loginReq.setPassword("Password123!");

        MvcResult loginRes = mockMvc
            .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode loginData = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("data");
        String accessToken = loginData.path("accessToken").asText();
        String refreshToken = loginData.path("refreshToken").asText();

        // Refresh token
        mockMvc
            .perform(
                post("/api/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", refreshToken)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        // Logout
        mockMvc
            .perform(
                post("/api/auth/logout")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", refreshToken)))
            )
            .andExpect(status().isOk());

        // Refresh token after logout must fail
        mockMvc
            .perform(
                post("/api/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", refreshToken)))
            )
            .andExpect(status().isBadRequest());

        // Forgot password flow
        mockMvc
            .perform(
                post("/api/auth/forgot-password/send-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(Map.of("email", "authflow@auctionhub.vn")))
            )
            .andExpect(status().isOk());

        OtpVerification fpOtp = otpRepository
            .findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc("authflow@auctionhub.vn", "FORGOT_PASSWORD")
            .orElseThrow();

        VerifyOtpRequestDTO fpVerify = new VerifyOtpRequestDTO();
        fpVerify.setEmail("authflow@auctionhub.vn");
        fpVerify.setOtpCode(fpOtp.getOtpCode());

        MvcResult fpRes = mockMvc
            .perform(
                post("/api/auth/forgot-password/verify-otp")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(fpVerify))
            )
            .andExpect(status().isOk())
            .andReturn();

        String resetToken = objectMapper.readTree(fpRes.getResponse().getContentAsString()).path("data").path("resetToken").asText();

        ResetPasswordRequestDTO resetReq = new ResetPasswordRequestDTO();
        resetReq.setResetToken(resetToken);
        resetReq.setNewPassword("NewPassword123!");

        mockMvc
            .perform(
                post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(resetReq))
            )
            .andExpect(status().isOk());
    }
}
