package com.vivid.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    public abstract HttpStatus getHttpStatus();
}
