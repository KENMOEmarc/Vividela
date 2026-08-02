package com.template.vivid.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends BusinessException {
    public InvalidStateTransitionException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.CONFLICT;
    }
}
