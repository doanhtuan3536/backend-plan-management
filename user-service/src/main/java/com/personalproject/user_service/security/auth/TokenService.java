package com.personalproject.user_service.security.auth;


import com.personalproject.user_service.models.Account;
import com.personalproject.user_service.repository.RefreshTokenRepository;
import com.personalproject.user_service.security.jwt.JwtUtility;
import com.personalproject.user_service.security.refreshtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    @Value("${app.security.jwt.refresh-token.expiration}")
    private int refreshTokenExpiration;

    @Autowired
    RefreshTokenRepository refreshTokenRepo;

    @Autowired
    JwtUtility jwtUtil;

    @Autowired
    PasswordEncoder passwordEncoder;

    public AuthResponse generateTokens(Account user) {
        String accessToken = jwtUtil.generateAccessToken(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setUserId(user.getUserId());

        String randomUUID = UUID.randomUUID().toString();

        response.setRefreshToken(randomUUID);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(passwordEncoder.encode(randomUUID));

        long refreshTokenExpirationInMillis = System.currentTimeMillis() + refreshTokenExpiration * 60000;
        refreshToken.setExpiryTime(new Date(refreshTokenExpirationInMillis));

        RefreshToken save = refreshTokenRepo.save(refreshToken);
        response.setRefreshTokenId((long)save.getId());

        return response;
    }
//    public String generateTokenForService(com.doanth.auth_service.model.Service service) {
//        String accessToken = jwtUtil.generateAccessTokenForService(service);
//
////        AuthResponse response = new AuthResponse();
////        response.setAccessToken(accessToken);
////        response.setUserId(user.getUserId());
//
////        String randomUUID = UUID.randomUUID().toString();
//
////        response.setRefreshToken(randomUUID);
//
////        RefreshToken refreshToken = new RefreshToken();
////        refreshToken.setUser(user);
////        refreshToken.setToken(passwordEncoder.encode(randomUUID));
//
////        long refreshTokenExpirationInMillis = System.currentTimeMillis() + refreshTokenExpiration * 60000;
////        refreshToken.setExpiryTime(new Date(refreshTokenExpirationInMillis));
//
////        refreshTokenRepo.save(refreshToken);
//
//        return accessToken;
//    }

    public AuthResponse refreshTokens(RefreshTokenRequest request) throws RefreshTokenNotFoundException,  RefreshTokenExpiredException {
        String rawRefreshToken = request.getRefreshToken();

        List<RefreshToken> listRefreshTokens = refreshTokenRepo.findByUsername(request.getUsername());

        RefreshToken foundRefreshToken = null;

        for (RefreshToken token : listRefreshTokens) {
            if (passwordEncoder.matches(rawRefreshToken, token.getToken())) {
                foundRefreshToken = token;
            }
        }

        if (foundRefreshToken == null)
            throw new RefreshTokenNotFoundException("Refresh token not found");

        Date currentTime = new Date();

        if (foundRefreshToken.getExpiryTime().before(currentTime))
            throw new RefreshTokenExpiredException("Refresh token expired");

        AuthResponse response = generateTokens(foundRefreshToken.getUser());
        response.setFullName(foundRefreshToken.getUser().getFullName());
        response.setRole(foundRefreshToken.getUser().getType());
        response.setUserId(foundRefreshToken.getUser().getUserId());
        response.setUsername(foundRefreshToken.getUser().getUsername());
        response.setAvatar(foundRefreshToken.getUser().getAvatar());
        response.setBio(foundRefreshToken.getUser().getBio());
        response.setEmail(foundRefreshToken.getUser().getEmail());

        refreshTokenRepo.delete(foundRefreshToken);

        return response;
    }
}
