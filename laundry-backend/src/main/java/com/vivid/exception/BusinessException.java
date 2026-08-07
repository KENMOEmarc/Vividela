package com.vivid.exception;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
    public abstract HttpStatus getHttpStatus();
}
