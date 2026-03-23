package com.personalproject.user_service.exception;

public class PasswordNotMatchingException extends Exception{
    public PasswordNotMatchingException(String message) {
        super(message);
    }
}
