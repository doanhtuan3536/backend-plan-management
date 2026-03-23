package com.personalproject.user_service.services;

import com.personalproject.user_service.AccountNotFoundException;
import com.personalproject.user_service.dto.ChangePasswordRequest;
import com.personalproject.user_service.exception.PasswordNotMatchingException;
import com.personalproject.user_service.models.Account;
import com.personalproject.user_service.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AccountService {
    @Autowired private AccountRepo accountRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Account findById(Long userId) throws AccountNotFoundException {
        Account user = accountRepo.findById(userId).orElseThrow(() -> new AccountNotFoundException("No user found with given id: " + userId));
        return user;
    }

    public Account findByEmail(String email) throws AccountNotFoundException {
        Account user = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException("No user found with given email: " + email));
        return user;
    }

    public void updateAvatar(Long userId, String url) throws AccountNotFoundException {
        Account user = accountRepo.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException("No user found with given id: " + userId));

        user.setAvatar(url);
        accountRepo.save(user);
    }
    public Account updateProfile(Map<String, Object> updates) throws AccountNotFoundException {
        Long userId = (long)(int) updates.get("userId");
        Account user = accountRepo.findById(userId).orElseThrow(() -> new AccountNotFoundException("No user found with given id: " + userId));

        updates.forEach((key, value) -> {
            switch (key) {
                case "fullName":
                    user.setFullName((String) value);
                    break;
                case "bio":
                    user.setBio((String) value);
                    break;
                case "username":
                    user.setUsername((String) value);
                    break;
            }
        });
        return accountRepo.save(user);
    }

    public void updatePassword(ChangePasswordRequest changePasswordRequest) throws AccountNotFoundException, PasswordNotMatchingException {
        Long userId = changePasswordRequest.getUserId();
        Account user = accountRepo.findById(userId).orElseThrow(() -> new AccountNotFoundException("No user found with given id: " + userId));

        String newPassword = changePasswordRequest.getNewPassword();

        if (!passwordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new PasswordNotMatchingException("Old password is wrong!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        accountRepo.save(user);
    }
}
