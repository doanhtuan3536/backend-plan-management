package com.personalproject.api_gateway.serviceClient;

import com.personalproject.api_gateway.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthServiceClient {
    private String serviceURL = "http://user-service/api/v1/users";
    private final String serviceLoginUrl = serviceURL + "/login";
    private final String serviceLogoutUrl = serviceURL + "/logout";
    private final String serviceUploadAvatarUrl = serviceURL + "/upload/avatar";
    private final String refreshTokenUrl = serviceURL + "/token/refresh";
    private final String updateProfileUrl = serviceURL + "/profile";
    private final String sendVerificationCodeUrl = serviceURL + "/send-verification-code";
    private final String sendVerificationCodeVerifyUrl = serviceURL + "/verify-code";
    private final String changePasswordUrl = serviceURL + "/change-password";
    @Autowired
    private RestTemplate restTemplate;

    public VerificationCodeResponse sendVerificationCodeRequest(EmailRequest emailRequest, String accessToken) throws JwtValidationException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(emailRequest,headers);
        ResponseEntity<VerificationCodeResponse> response = null;
        try{
            response= restTemplate.exchange(
                    sendVerificationCodeUrl, HttpMethod.POST, request, VerificationCodeResponse.class
            );
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("Too many verification requests")){
                    throw new VerificationCodeException("Too many verification requests");
                }
                if (errorMsg.contains("No user found with given email")){
                    throw new UserNotFoundException("No user found with given email");
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }

    public void sendVerificationCode(VerificationCodeRequest verificationCodeRequest, String accessToken) throws JwtValidationException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(verificationCodeRequest,headers);
        ResponseEntity<VerificationCodeResponse> response = null;
        try{
            response= restTemplate.exchange(
                    sendVerificationCodeVerifyUrl, HttpMethod.POST, request, VerificationCodeResponse.class
            );

        }
        catch(HttpClientErrorException e){
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("Too many verifying attempts. Please try again later")){
                    throw new VerifyCodeTooManyException("Too many verifying attempts. Please try again later.");
                }
                if (errorMsg.contains("Verification code is incorrect")){
                    throw new VerifyCodeWrongException("Verification code is incorrect");
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }

    public void changePassword(ChangePasswordRequest passwordRequest, String accessToken) throws JwtValidationException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(passwordRequest,headers);
        ResponseEntity<?> response = null;
        try{
            response= restTemplate.exchange(
                    changePasswordUrl, HttpMethod.POST, request, Object.class
            );
        }
        catch(HttpClientErrorException e){
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("No user found with given id")){
                    throw new UserNotFoundException("No user found with given id.");
                }
                if (errorMsg.contains("Old password is wrong!")){
                    throw new PasswordNotMatchingException("Old password is wrong!");
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }

    public AuthResponse login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LoginRequest form = new LoginRequest();
        form.setUsername(username);
        form.setPassword(password);

        HttpEntity<LoginRequest> request = new HttpEntity<>(form, headers);
//        System.out.println(request);
        ResponseEntity<AuthResponse> response = null;

        try{
            response= restTemplate.exchange(
                    serviceLoginUrl, HttpMethod.POST, request, AuthResponse.class
            );
            System.out.println(response.getBody());
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            throw new RuntimeException("Authentication failed");
        }
    }
    public User getUserInfo(Long userId, String accessToken) throws JwtValidationException, UserNotFoundException {
        String url = serviceURL + "/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<User> response = null;
        try{
            response= restTemplate.exchange(
                    url, HttpMethod.GET, request, User.class
            );
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("No user found with given id")){
                    throw new UserNotFoundException(userId);
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }
    public User updateUser(Map<String, Object> updates,  String accessToken) throws JwtValidationException, UserNotFoundException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<?> request = new HttpEntity<>(updates, headers);
        ResponseEntity<User> response = null;
        try{
            response= restTemplate.exchange(
                    updateProfileUrl, HttpMethod.PUT, request, User.class
            );
            System.out.println(response);
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            System.out.println(e);
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("No user found with given id")){
                    throw new UserNotFoundException((long)(int)updates.get("userId"));
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }
    public UploadAvatarResponse uploadUserAvatar(Long userId, MultipartFile file , String accessToken) throws JwtValidationException, UserNotFoundException, IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Bearer " + accessToken);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        body.add("userId", userId.toString());

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);
        ResponseEntity<UploadAvatarResponse> response = null;
        try{
            response= restTemplate.exchange(
                    serviceUploadAvatarUrl, HttpMethod.POST, request, UploadAvatarResponse.class
            );
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            ErrorDTO error = e.getResponseBodyAs(ErrorDTO.class);
            error.getErrors().forEach((errorMsg)-> {
                if (errorMsg.contains("No user found with given id")){
                    throw new UserNotFoundException(userId);
                }
            });
            throw new JwtValidationException(error.getErrors().get(0));
        }
    }
    public String logout(Integer refreshTokenId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        LogoutRequest form = new LogoutRequest(refreshTokenId);
//        form.setRefreshToken(refreshtoken);

        HttpEntity<LogoutRequest> request = new HttpEntity<>(form, headers);
//        System.out.println(request);
        ResponseEntity<Void> response = null;

        try{
            response= restTemplate.exchange(
                    serviceLogoutUrl, HttpMethod.POST, request, Void.class
            );
            System.out.println(response);
            return "Logout successful";
        }
        catch(HttpClientErrorException e){
            throw new RuntimeException("Logout failed");
        }
    }
    public AuthResponse refreshToken(RefreshTokenRequest requestTokenRefresh) throws RefreshTokenException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RefreshTokenRequest> request = new HttpEntity<>(requestTokenRefresh, headers);
//        System.out.println(request);
        ResponseEntity<AuthResponse> response = null;

        try{
            response= restTemplate.exchange(
                    refreshTokenUrl, HttpMethod.POST, request, AuthResponse.class
            );
            System.out.println(response);
            return response.getBody();
        }
        catch(HttpClientErrorException e){
            String msg = e.getResponseBodyAsString();
            System.out.println(msg);
            throw new RefreshTokenException(msg);
        }
    }
}
