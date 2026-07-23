package com.template.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lors d'une tentative de connexion avec des identifiants incorrects.
 *
 * SÉCURITÉ : Le message retourné au client est volontairement générique
 * ("Identifiants invalides") pour ne pas révéler si c'est l'email ou
 * le mot de passe qui est erroné (protection contre l'énumération de comptes).
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
