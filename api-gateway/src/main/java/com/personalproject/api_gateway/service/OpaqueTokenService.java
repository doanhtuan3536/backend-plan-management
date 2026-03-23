package com.personalproject.api_gateway.service;

import com.personalproject.api_gateway.dto.AuthResponse;
import com.personalproject.api_gateway.models.TokenInfo;
import com.personalproject.api_gateway.models.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OpaqueTokenService {
    private final RedisTemplate<String, TokenInfo> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String OPAQUE_PREFIX = "opaque:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";

    @Value("${token.opaque.ttl-seconds:900}")
    private long opaqueTokenTtl;

    @Value("${token.refresh.ttl-seconds:604800}")
    private long refreshTokenTtl;

    // Constructor injection - this initializes the fields
    public OpaqueTokenService(
            RedisTemplate<String, TokenInfo> redisTemplate,
            StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public TokenPair createTokens(AuthResponse authResponse, HttpServletRequest request) {
        String opaqueToken = generateSecureToken();
        String refreshToken = authResponse.getRefreshToken();

        TokenInfo tokenInfo = TokenInfo.builder()
                .refreshToken(refreshToken)
                .accessToken(authResponse.getAccessToken())
                .userId(authResponse.getUserId())
                .username(authResponse.getUsername())
                .fullName(authResponse.getFullName())
                .role(authResponse.getRole())
                .createdAt(Instant.now())
                .lastAccessTime(Instant.now())
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .avatar(authResponse.getAvatar())
                .refreshTokenId(authResponse.getRefreshTokenId())
                .bio(authResponse.getBio())
                .email(authResponse.getEmail())
                .build();

        // Store in Redis
        redisTemplate.opsForValue()
                .set(OPAQUE_PREFIX + opaqueToken, tokenInfo, opaqueTokenTtl, TimeUnit.SECONDS);

        stringRedisTemplate.opsForValue()
                .set(REFRESH_PREFIX + refreshToken,
                        authResponse.getUserId().toString(),
                        refreshTokenTtl, TimeUnit.SECONDS);

        // Track all tokens for this user
        stringRedisTemplate.opsForSet()
                .add(USER_TOKENS_PREFIX + authResponse.getUserId(), opaqueToken);

        stringRedisTemplate.expire(
                USER_TOKENS_PREFIX + authResponse.getUserId(),
                refreshTokenTtl,
                TimeUnit.SECONDS);

        log.info("Created tokens for user: {}", authResponse.getUsername());

        return TokenPair.builder()
                .opaqueToken(opaqueToken)
                .refreshToken(refreshToken)
                .expiresIn(opaqueTokenTtl)
                .build();
    }

    public TokenInfo validateToken(String opaqueToken) throws OpaqueTokenExpiredException {
        String key = OPAQUE_PREFIX + opaqueToken;
        System.out.println(key);
        TokenInfo tokenInfo = redisTemplate.opsForValue().get(key);

        if (tokenInfo == null) {
            throw new OpaqueTokenExpiredException("Token invalid or expired");
        }

        // Refresh TTL on access
        tokenInfo.setLastAccessTime(Instant.now());
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println("Token TTL: " + ttlSeconds + " seconds");

        if (ttlSeconds != null && ttlSeconds < 60) {
            redisTemplate.opsForValue().set(key, tokenInfo, opaqueTokenTtl, TimeUnit.SECONDS);
            System.out.println("Token renewed, new TTL: " + opaqueTokenTtl + " seconds");
        } else if (ttlSeconds != null && ttlSeconds >= 60) {
            redisTemplate.opsForValue().set(key, tokenInfo, ttlSeconds, TimeUnit.SECONDS);
            System.out.println("Token updated, remaining TTL: " + ttlSeconds + " seconds");
        }

//        redisTemplate.opsForValue().set(key, tokenInfo);

        return tokenInfo;
    }

    public Long getTtlSeconds(String opaqueToken) {
        String key = OPAQUE_PREFIX + opaqueToken;
        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        System.out.println("Token TTL: " + ttlSeconds + " seconds");
        return ttlSeconds;
    }

    public String refreshAccessToken(String refreshToken) {
        String userIdStr = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);

        if (userIdStr == null) {
            throw new RuntimeException("Refresh token invalid");
        }

        Long userId = Long.parseLong(userIdStr);

        // Here you would call user service to get new access token
        // This is a placeholder - implement based on your user service API
        AuthResponse newAuthResponse = callUserServiceForNewToken(userId);

        String newOpaqueToken = generateSecureToken();
        TokenInfo newTokenInfo = TokenInfo.builder()
                .accessToken(newAuthResponse.getAccessToken())
                .userId(userId)
                .username(newAuthResponse.getUsername())
                .fullName(newAuthResponse.getFullName())
                .role(newAuthResponse.getRole())
                .createdAt(Instant.now())
                .lastAccessTime(Instant.now())
                .build();

        redisTemplate.opsForValue()
                .set(OPAQUE_PREFIX + newOpaqueToken, newTokenInfo, opaqueTokenTtl, TimeUnit.SECONDS);

        stringRedisTemplate.opsForSet()
                .add(USER_TOKENS_PREFIX + userId, newOpaqueToken);

        return newOpaqueToken;
    }

    public void revokeToken(String opaqueToken, String refreshToken) {
        try {
            TokenInfo tokenInfo = validateToken(opaqueToken);
            redisTemplate.delete(OPAQUE_PREFIX + opaqueToken);
            redisTemplate.delete(REFRESH_PREFIX + refreshToken);
            stringRedisTemplate.opsForSet()
                    .remove(USER_TOKENS_PREFIX + tokenInfo.getUserId(), opaqueToken);

            log.info("Token revoked: {}", opaqueToken);
        } catch (Exception e) {
            log.warn("Error revoking token: {}", e.getMessage());
        }
    }

    public void revokeAllUserTokens(Long userId) {
        Set<String> tokens = stringRedisTemplate.opsForSet()
                .members(USER_TOKENS_PREFIX + userId);

        if (tokens != null && !tokens.isEmpty()) {
            tokens.forEach(token ->
                    redisTemplate.delete(OPAQUE_PREFIX + token));
        }

        stringRedisTemplate.delete(USER_TOKENS_PREFIX + userId);

        log.info("All tokens revoked for user: {}", userId);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuthResponse callUserServiceForNewToken(Long userId) {
        // Implement call to user service
        // You can use RestTemplate or WebClient here
        throw new UnsupportedOperationException("Implement user service call");
    }
}