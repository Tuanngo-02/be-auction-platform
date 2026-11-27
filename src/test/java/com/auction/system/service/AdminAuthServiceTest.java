package com.auction.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.auction.system.domain.User;
import com.auction.system.repository.UserRepository;
import com.auction.system.service.dto.AdminActivationRequestDTO;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Long> bucket;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        adminAuthService = new AdminAuthService(userRepository, passwordEncoder, redissonClient, null);
    }

    @Test
    void createActivationToken_storesUserIdWithTtl() {
        when(redissonClient.<Long>getBucket(anyString())).thenReturn(bucket);

        String token = adminAuthService.createActivationToken(10L);

        assertThat(token).startsWith("act_");
        verify(bucket).set(eq(10L), eq(Duration.ofHours(24)));
    }

    @Test
    void verifyActivationToken_validToken_returnsAdminInfo() {
        User admin = new User();
        admin.setId(10L);
        admin.setEmail("auction.admin@auctionhub.vn");
        admin.setFullName("Auction Admin");

        when(redissonClient.<Long>getBucket("admin:activation:act_123")).thenReturn(bucket);
        when(bucket.get()).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));

        Map<String, Object> result = adminAuthService.verifyActivationToken("act_123");

        assertThat(result).containsEntry("email", "auction.admin@auctionhub.vn");
        assertThat(result).containsEntry("fullName", "Auction Admin");
        assertThat(result).containsEntry("valid", true);
    }

    @Test
    void verifyActivationToken_expiredOrBlank_throwsBadRequest() {
        assertThatThrownBy(() -> adminAuthService.verifyActivationToken(" "))
            .isInstanceOf(BadRequestServiceException.class);

        when(redissonClient.<Long>getBucket("admin:activation:expired")).thenReturn(bucket);
        when(bucket.get()).thenReturn(null);

        assertThatThrownBy(() -> adminAuthService.verifyActivationToken("expired"))
            .isInstanceOf(BadRequestServiceException.class);
    }

    @Test
    void activateAdminAccount_passwordMismatch_throwsBadRequest() {
        AdminActivationRequestDTO req = new AdminActivationRequestDTO();
        req.setActivationToken("act_123");
        req.setNewPassword("Admin@123");
        req.setConfirmPassword("Different@123");

        assertThatThrownBy(() -> adminAuthService.activateAdminAccount(req))
            .isInstanceOf(BadRequestServiceException.class);
        verifyNoInteractions(redissonClient);
    }

    @Test
    void activateAdminAccount_validToken_updatesPasswordAndStatus() {
        User admin = new User();
        admin.setId(10L);
        admin.setAccountStatus("PENDING_ACTIVATION");
        admin.setActivated(false);

        AdminActivationRequestDTO req = new AdminActivationRequestDTO();
        req.setActivationToken("act_123");
        req.setNewPassword("Admin@123");
        req.setConfirmPassword("Admin@123");

        when(redissonClient.<Long>getBucket("admin:activation:act_123")).thenReturn(bucket);
        when(bucket.getAndDelete()).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("Admin@123")).thenReturn("hashed");

        Map<String, Object> result = adminAuthService.activateAdminAccount(req);

        assertThat(result).containsEntry("userId", 10L);
        assertThat(result).containsEntry("accountStatus", "ACTIVE");
        assertThat(admin.getPassword()).isEqualTo("hashed");
        assertThat(admin.isActivated()).isTrue();
        assertThat(admin.getEmailVerified()).isTrue();
        verify(userRepository).save(admin);
    }
}
