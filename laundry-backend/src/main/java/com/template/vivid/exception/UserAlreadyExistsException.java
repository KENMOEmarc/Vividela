package com.template.vivid.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée quand un email ou username est déjà utilisé en base.
 *
 * @ResponseStatus(CONFLICT) : si cette exception remonte jusqu'au DispatcherServlet
 * sans être interceptée, Spring retourne automatiquement un HTTP 409 Conflict.
 * Dans notre cas, elle est interceptée par GlobalExceptionHandler qui formate
 * la réponse selon notre structure ApiResponse standardisée.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
