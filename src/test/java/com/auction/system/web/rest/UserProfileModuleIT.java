package com.auction.system.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.auction.system.IntegrationTest;
import com.auction.system.domain.OtpVerification;
import com.auction.system.repository.OtpVerificationRepository;
import com.auction.system.repository.UserRepository;
import com.auction.system.service.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
class UserProfileModuleIT {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void testFullUserAndSellerLifecycle() throws Exception {
        // 1. Register
        RegisterRequestDTO reg = new RegisterRequestDTO();
        reg.setFullName("Nguyễn Văn Test");
        reg.setEmail("testuser@auctionhub.vn");
        reg.setPhone("0905999888");
        reg.setPassword("Password123!");
        reg.setBirthDate(LocalDate.of(2000, 1, 15));

        MvcResult regRes = mockMvc
            .perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(reg)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statusCode").value(201))
            .andExpect(jsonPath("$.data.email").value("testuser@auctionhub.vn"))
            .andReturn();

        Long userId = objectMapper.readTree(regRes.getResponse().getContentAsString()).path("data").path("userId").asLong();

        // 2. Verify OTP
        OtpVerification otp = otpRepository
            .findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc("testuser@auctionhub.vn", "REGISTER_EMAIL")
            .orElseThrow();

        VerifyOtpRequestDTO verifyReq = new VerifyOtpRequestDTO();
        verifyReq.setEmail("testuser@auctionhub.vn");
        verifyReq.setOtpCode(otp.getOtpCode());

        mockMvc
            .perform(post("/api/auth/verify-email-otp").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(verifyReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.emailVerified").value(true));

        // 3. Login with email
        LoginRequestDTO loginReq = new LoginRequestDTO();
        loginReq.setEmailOrPhone("testuser@auctionhub.vn");
        loginReq.setPassword("Password123!");

        MvcResult loginRes = mockMvc
            .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(loginReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
            .andReturn();

        String accessToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 4. Get Profile
        mockMvc
            .perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Test"))
            .andExpect(jsonPath("$.data.phone").value("0905999888"));

        // 5. Update Profile
        UpdateProfileDTO updateReq = new UpdateProfileDTO();
        updateReq.setFullName("Nguyễn Văn Updated");
        updateReq.setBirthDate(LocalDate.of(1999, 5, 20));

        mockMvc
            .perform(
                put("/api/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(updateReq))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Updated"));

        // 6. Add Address
        AddressDTO addrReq = new AddressDTO();
        addrReq.setAddressText("470 Trần Đại Nghĩa, Đà Nẵng");
        addrReq.setIsDefault(true);

        MvcResult addrRes = mockMvc
            .perform(
                post("/api/users/me/addresses")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(addrReq))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.addressText").value("470 Trần Đại Nghĩa, Đà Nẵng"))
            .andReturn();

        Long addressId = objectMapper.readTree(addrRes.getResponse().getContentAsString()).path("data").path("addressId").asLong();

        // 7. Delete Address
        mockMvc
            .perform(delete("/api/users/me/addresses/" + addressId).header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());

        // 8. Submit Seller Verification
        SellerVerificationDTO sellerReq = new SellerVerificationDTO();
        sellerReq.setIdCardNumber("048200012345");
        sellerReq.setIdCardFrontUrl("https://example.com/front.jpg");
        sellerReq.setIdCardBackUrl("https://example.com/back.jpg");

        mockMvc
            .perform(
                post("/api/users/me/seller-verification")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(sellerReq))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        // 9. Public Seller Profile
        mockMvc
            .perform(get("/api/sellers/" + userId + "/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Updated"));
    }
}
