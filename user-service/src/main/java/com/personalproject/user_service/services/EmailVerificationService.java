package com.personalproject.user_service.services;

import com.personalproject.user_service.AccountNotFoundException;
import com.personalproject.user_service.dto.VerificationCodeResponse;
import com.personalproject.user_service.models.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class EmailVerificationService {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${verification.code.expiration:300}")
    private long verificationCodeExpiration;
    @Value("${spring.mail.password}")
    private String password;

    private static final String VERIFICATION_CODE_PREFIX = "verification:code:";
    private static final String VERIFICATION_REQUEST_CODE_ATTEMPTS_PREFIX = "verification:code:attempts:";
    private static final String VERIFIED_EMAIL_PREFIX = "verified:email:";
    private static final String ATTEMPTS_VERIFY_PREFIX = "verification:attempts:verify:";

    private @Value("${spring.mail.username}") String emailFrom;

    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int MAX_REQUEST_VERIFICATION_CODE = 5;
    private static final int MAX_VERIFY_ATTEMPTS = 10;
    private static final long ATTEMPT_WINDOW_HOURS = 24;

    public String generateVerificationCode() {
        return String.valueOf((int)((Math.random() * 900000) + 100000));
    }

    public VerificationCodeResponse sendVerificationCode(String email) throws AccountNotFoundException, VerificationRequestTooManyException {
        System.out.println(email);
        Account user = accountService.findByEmail(email);

        String attemptKey = VERIFICATION_REQUEST_CODE_ATTEMPTS_PREFIX + email;
        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        if(attemptsStr == null){
            redisTemplate.opsForValue().set(attemptKey, "0", 24, TimeUnit.HOURS);
        }
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_REQUEST_VERIFICATION_CODE) {
            throw new VerificationRequestTooManyException("Too many verification requests. Please try again after 24 hours.");
        }
        try {
            String code = generateVerificationCode();
            String redisKey = VERIFICATION_CODE_PREFIX + email;
            redisTemplate.opsForValue().set(
                    redisKey,
                    code,
                    verificationCodeExpiration,
                    TimeUnit.SECONDS
            );

            redisTemplate.opsForValue().increment(attemptKey);
            redisTemplate.expire(attemptKey, 24, TimeUnit.HOURS);
            System.out.println(emailFrom);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("Verification code for changing password");
            message.setText(String.format(
                    "Your verification code: %s\n" +
                            "Expired in %d minutes.\n" +
                            "You have %d trys in 24 hours.\n" +
                            "If you are not changing your password, please ignore this email.",
                    code,
                    verificationCodeExpiration / 60,
                    5 - attempts - 1
            ));

            mailSender.send(message);
            VerificationCodeResponse response = new VerificationCodeResponse();
            response.setEmail(email);
            response.setSuccess(true);
            response.setVerificationCode(code);
            return response;
        } catch (MailAuthenticationException e) {
            System.err.println("Authentication failed for email: " + email);
            throw new RuntimeException("Failed to send verification code", e);
        } catch (MailSendException e) {
            System.err.println("Failed to send email to: " + email);
            if (e.getFailedMessages() != null) {
                e.getFailedMessages().forEach((k, v) ->
                        System.err.println("Failed message details: " + k + " - " + v));
            }
            throw new RuntimeException("Failed to send verification code", e);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to send verification code", e);
        }
    }

    public void verifyCode(String email, String code) {
        try {
            String verifyAttemptKey = ATTEMPTS_VERIFY_PREFIX + email;
            String attemptsStr = redisTemplate.opsForValue().get(verifyAttemptKey);
            int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
            if(attemptsStr == null){
                redisTemplate.opsForValue().set(verifyAttemptKey, "0", 1, TimeUnit.HOURS);
            }
            if (attempts >= MAX_VERIFY_ATTEMPTS) {
                throw new VerificationAttempsException("Too many verifying attempts. Please try again later.");
            }

            String redisKey = VERIFICATION_CODE_PREFIX + email;
            String storedCode = redisTemplate.opsForValue().get(redisKey);

            if (storedCode != null && storedCode.equals(code)) {
                String attemptKey = VERIFICATION_REQUEST_CODE_ATTEMPTS_PREFIX + email;
                redisTemplate.delete(attemptKey);
                redisTemplate.delete(redisKey);
                redisTemplate.delete(verifyAttemptKey);

            } else {
                redisTemplate.opsForValue().increment(verifyAttemptKey);
                redisTemplate.expire(verifyAttemptKey, 1, TimeUnit.HOURS);

                throw new VerificationAttempsException(
                        String.format("Verification code is incorrect. You have %d attempts left.",
                                MAX_VERIFY_ATTEMPTS - attempts - 1)
                );
            }

        } catch (Exception e) {
//            e.printStackTrace();
            throw new RuntimeException("Failed to verify code", e);
        }
    }



}
