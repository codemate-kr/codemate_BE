package com.ryu.studyhelper.auth.token;

public class RefreshTokenReuseException extends RefreshTokenException {
    
    public RefreshTokenReuseException(String message) {
        super(message);
    }
    
    public RefreshTokenReuseException(String message, Throwable cause) {
        super(message, cause);
    }
}