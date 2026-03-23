package com.personalproject.api_gateway.serviceClient;

public class VerifyCodeTooManyException extends RuntimeException{
    public VerifyCodeTooManyException(String message) {
        super(message);
    }
}
