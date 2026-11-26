package com.auction.system.repository;

import com.auction.system.domain.LoginSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    List<LoginSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LoginSession> findByRefreshToken(String refreshToken);

    Optional<LoginSession> findByIdAndUserId(Long id, Long userId);
}
