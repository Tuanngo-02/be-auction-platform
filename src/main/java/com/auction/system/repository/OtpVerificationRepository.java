package com.auction.system.repository;

import com.auction.system.domain.OtpVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByEmailIgnoreCaseAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(String email, String otpType);

    Optional<OtpVerification> findByResetTokenAndUsedFalse(String resetToken);
}
