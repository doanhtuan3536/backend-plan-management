package com.personalproject.user_service;

import com.personalproject.user_service.dto.*;
import com.personalproject.user_service.exception.PasswordNotMatchingException;
import com.personalproject.user_service.models.Account;
import com.personalproject.user_service.models.AccountType;
import com.personalproject.user_service.security.auth.AuthResponse;
import com.personalproject.user_service.security.auth.TokenService;
import com.personalproject.user_service.security.config.CustomUserDetails;
import com.personalproject.user_service.security.jwt.JwtUtility;
import com.personalproject.user_service.security.refreshtoken.RefreshTokenExpiredException;
import com.personalproject.user_service.security.refreshtoken.RefreshTokenNotFoundException;
import com.personalproject.user_service.security.refreshtoken.RefreshTokenRequest;
import com.personalproject.user_service.services.*;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountService accountService;
    @Autowired
    @Qualifier("userAuthManager")
    private AuthenticationManager userAuthManager;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private  EmailVerificationService emailVerificationService;
//    @Autowired
//    @Qualifier("serviceAuthManager")
//    private AuthenticationManager serviceAuthManager;
    //    private final JwtUtility jwtUtil;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private JwtUtility jwtUtil;
    @Autowired
    ModelMapper modelMapper;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId) throws AccountNotFoundException {
        Account user = accountService.findById(userId);
        System.out.println(user);
        User user1 = modelMapper.map(user, User.class);
        System.out.println(user1);
        return ResponseEntity.ok(user1);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginForm body) {
        try {
            String username = body.getUsername();
            String password = body.getPassword();

            Authentication authentication = userAuthManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

            AuthResponse token = tokenService.generateTokens(customUserDetails.getUser());
            token.setUserId(customUserDetails.getUser().getUserId());
            token.setUsername(username);
            token.setAvatar(customUserDetails.getUser().getAvatar());
            token.setRole(AccountType.valueOf(customUserDetails.getAuthorities().iterator().next().getAuthority().toUpperCase()));
            token.setFullName(customUserDetails.getUser().getFullName());
            token.setBio(customUserDetails.getUser().getBio());
            token.setEmail(customUserDetails.getUser().getEmail());
            System.out.println(token);
            return ResponseEntity.ok(token);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        Integer refreshTokenId = request.getRefreshTokenId();
        System.out.println(refreshTokenId);

        try {
//            String refreshTokenDecoded = passwordEncoder.encode(refreshToken);
//            System.out.println(refreshTokenDecoded);
            refreshTokenService.revokeRefreshToken(refreshTokenId);
        } catch (RefreshTokenNotFoundException e) {
//            throw new RuntimeException(e);
            System.out.println("Token not found");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/upload/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId) throws IOException, AccountNotFoundException {
        String imageUrl = cloudinaryService.uploadFile(file);
        accountService.updateAvatar(userId, imageUrl);
        return ResponseEntity.ok(Map.of("url", imageUrl));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody @Valid RefreshTokenRequest request) throws RefreshTokenExpiredException, RefreshTokenNotFoundException {
        System.out.println(request.getUsername());
        System.out.println(request.getRefreshToken());
        AuthResponse response = tokenService.refreshTokens(request);
        return ResponseEntity.ok(response);

    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> updates) throws AccountNotFoundException {
        System.out.println(updates);
        Account user = accountService.updateProfile(updates);
        User user1 = modelMapper.map(user, User.class);
        return ResponseEntity.ok(user);
    }



//    @PostMapping("/change-password")
//    public ResponseEntity<?> changePassword(@RequestBody Map<String, Object> updates){
//
//    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody EmailRequest email) throws AccountNotFoundException, VerificationRequestTooManyException {
        VerificationCodeResponse verificationCodeResponse = emailVerificationService.sendVerificationCode(email.getEmail());
        return ResponseEntity.ok(verificationCodeResponse);

    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerificationCodeRequest verificationCodeRequest) throws AccountNotFoundException {
        emailVerificationService.verifyCode(verificationCodeRequest.getEmail(), verificationCodeRequest.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> chanegPassword(@RequestBody ChangePasswordRequest changePasswordRequest) throws AccountNotFoundException, PasswordNotMatchingException {
        accountService.updatePassword(changePasswordRequest);
        return ResponseEntity.ok().build();
    }
}
