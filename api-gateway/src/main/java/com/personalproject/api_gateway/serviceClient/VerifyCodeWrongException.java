package com.personalproject.api_gateway.serviceClient;

public class VerifyCodeWrongException extends RuntimeException{
    public VerifyCodeWrongException(String message) {
        super(message);
    }
}
