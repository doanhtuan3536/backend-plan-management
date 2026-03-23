package com.personalproject.user_service.services;

public class VerificationRequestTooManyException extends Exception{
    public VerificationRequestTooManyException(String message) {
        super(message);
    }
}
