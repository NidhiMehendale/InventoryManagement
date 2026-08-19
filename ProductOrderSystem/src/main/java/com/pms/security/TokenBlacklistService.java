package com.pms.security;

import com.pms.entity.BlacklistedToken;
import com.pms.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Since JWTs are stateless, a proper "logout" requires tracking invalidated
 * tokens until their natural expiry. This service backs that behavior with
 * a DB-persisted blacklist table.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtUtil jwtUtil;

    public void blacklistToken(String token) {
        if (tokenBlacklistRepository.existsByToken(token)) {
            return;
        }
        Date expiry = jwtUtil.extractExpiration(token);
        BlacklistedToken blacklisted = BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiry.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                .build();
        tokenBlacklistRepository.save(blacklisted);
    }

    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }

    /** Periodically purge expired blacklist entries (every hour). */
    @Scheduled(fixedRate = 3_600_000)
    public void purgeExpiredTokens() {
        tokenBlacklistRepository.findAll().stream()
                .filter(t -> t.getExpiresAt().isBefore(LocalDateTime.now()))
                .forEach(tokenBlacklistRepository::delete);
    }
}
