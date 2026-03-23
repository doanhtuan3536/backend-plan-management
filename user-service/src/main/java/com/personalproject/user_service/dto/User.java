package com.personalproject.user_service.dto;

import com.personalproject.user_service.models.AccountStatus;
import com.personalproject.user_service.models.AccountType;
import com.personalproject.user_service.models.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    protected LocalDateTime createdAt;

    private Long userId;

    private String username;

    private String avatar;

    private String email;

    private String fullName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private String phoneNumber;

    private AccountType type;

    private AccountStatus status;

    private String bio;



    public User() {
    }

    public User(LocalDateTime createdAt, Long userId, String username, String avatar, String email, String fullName,
                LocalDate dateOfBirth, Gender gender, String phoneNumber, AccountType type, AccountStatus status, String bio) {
        this.createdAt = createdAt;
        this.userId = userId;
        this.username = username;
        this.avatar = avatar;
        this.email = email;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.type = type;
        this.status = status;
        this.bio = bio;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
