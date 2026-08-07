package com.vivid.exception;

import org.springframework.http.HttpStatus;

/**
 * Ressource métier introuvable (commande, produit, article, ticket, etc.).
 * <p>
 * Avant l'introduction de cette exception, ces cas étaient signalés via
 * {@link IllegalArgumentException}, systématiquement mappée sur 400 Bad
 * Request par {@link GlobalExceptionHandler} — alors qu'il s'agit
 * sémantiquement d'un 404 Not Found. Utiliser ce type dédié permet :
 * - une réponse HTTP correcte (404) ;
 * - un traitement explicite côté frontend (if (status === 404) ...)
 * plutôt qu'un parsing du message d'erreur.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
