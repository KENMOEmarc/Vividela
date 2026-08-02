package com.template.vivid.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand le mot de passe et sa confirmation ne correspondent pas.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordMismatchException extends InvalidRequestException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}
