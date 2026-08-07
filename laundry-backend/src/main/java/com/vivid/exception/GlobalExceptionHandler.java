package com.vivid.exception;

import com.vivid.model.payloads.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestionnaire global d'exceptions pour tous les contrôleurs REST.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * Intercepte les exceptions levées dans n'importe quel @RestController
 * et les transforme en réponses JSON structurées (ApiResponse).
 * <p>
 * AVANTAGES :
 * - Code de gestion d'erreurs centralisé (DRY)
 * - Réponses cohérentes sur toute l'API
 * - Les contrôleurs restent propres (aucun try/catch)
 * - Facile à étendre pour de nouveaux types d'exception
 * <p>
 * HIÉRARCHIE DES HANDLERS :
 * Spring choisit le handler le plus spécifique.
 * Ex: UserAlreadyExistsException → son handler dédié,
 * pas handleGeneralException().
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erreurs de validation @Valid sur les @RequestBody DTOs.
     * <p>
     * Retourne la liste de tous les champs invalides avec leur message d'erreur.
     * Format : ["email: Format d'email invalide", "password: Le mot de passe doit..."]
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.debug("Erreurs de validation: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Données invalides", errors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = ex.getHttpStatus();
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            log.warn("Erreur métier ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            log.info("Erreur métier ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Email ou username déjà utilisé (unicité en base).
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExists(
            UserAlreadyExistsException ex) {

        log.info("Conflit d'inscription: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Identifiants de connexion incorrects.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
            InvalidCredentialsException ex) {

        log.info("Tentative de connexion échouée: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Mots de passe non correspondants lors de l'inscription.
     */
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handlePasswordMismatch(
            PasswordMismatchException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Utilisateur non trouvé en base.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UsernameNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Accès refusé : l'utilisateur est authentifié mais n'a pas les droits
     * nécessaires pour cette ressource (géré par @PreAuthorize).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex) {

        log.warn("Accès refusé: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé : permissions insuffisantes"));
    }

    /**
     * Données invalides fournies par un service (entité introuvable, règle métier, etc.).
     * <p>
     * BUGFIX : IllegalArgumentException était levée dans plusieurs services
     * (StockServiceImpl, ProductServiceImpl, OrderServiceImpl…) mais n'était pas
     * interceptée ici — elle tombait dans le handler générique handleGeneralException()
     * et retournait un 500, au lieu d'un 400 ou 404 métier approprié.
     * Certains contrôleurs compensaient avec des try/catch locaux ; désormais ce
     * handler centralise toutes ces erreurs et évite la duplication.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Requête invalide: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Ressource métier introuvable (commande, produit, article, ticket...).
     * Voir revue de code — "Distinction 400 vs 404 pour les ressources introuvables".
     * Nouveaux appels de service : préférer lever cette exception plutôt
     * qu'une IllegalArgumentException lorsque la cause est "introuvable".
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.info("Ressource introuvable: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Violation de contrainte d'intégrité en base (ex. collision sur une
     * colonne UNIQUE comme barcode/ticket_number, clé étrangère, etc.).
     * <p>
     * Avant ce handler dédié, ces erreurs tombaient dans le fallback générique
     * (500 Internal Server Error) sans message exploitable pour l'utilisateur.
     * On renvoie désormais un 409 Conflict avec un message générique invitant
     * à réessayer (on n'expose jamais le détail SQL brut au client).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violation de contrainte d'intégrité en base: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        "Cette opération est en conflit avec une donnée existante. Veuillez réessayer."));
    }

    /**
     * Règle métier violée (ex. suppression d'une commande déjà payée).
     * Voir revue de code — "deleteOrder sans garde-fou métier".
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.info("Règle métier violée: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * BUGFIX : transaction marquée comme rollback-only. Cela peut survenir lorsqu'une
     * opération imbriquée (ex. génération de reçu) échoue et marque la transaction parent
     * comme invalide. Avec la correction REQUIRES_NEW pour ensureReceiptForOrder, ce cas
     * devrait être rare, mais on le gère gracieusement ici pour les autres cas.
     * Voir : "Transaction silently rolled back because it has been marked as rollback-only"
     */
    @ExceptionHandler(UnexpectedRollbackException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedRollback(UnexpectedRollbackException ex) {
        log.error("Transaction marquée comme rollback-only (opération imbriquée échouée ?): {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "La transaction a échoué de manière inattendue. Veuillez réessayer. Si le problème persiste, contactez le support."));
    }

    /**
     * Fallback : toute exception non gérée par les handlers ci-dessus.
     * Retourne 500 avec un message générique (ne pas exposer les détails internes).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Erreur interne non gérée: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage()));
    }
}