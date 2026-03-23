package com.personalproject.api_gateway.dto;

public class VerificationCodeException extends RuntimeException{
    public VerificationCodeException(String message) {
        super(message);
    }
}
