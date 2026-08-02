package com.template.vivid.model.payloads.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enveloppe de réponse API standardisée.
 *
 * TOUTES les réponses de l'API respectent cette structure unifiée,
 * qu'il s'agisse d'un succès, d'une erreur métier ou d'une erreur technique.
 *
 * FORMAT SUCCÈS :
 * {
 *   "success":   true,
 *   "message":   "Opération réussie",
 *   "data":      { ... },         ← payload métier (peut être null)
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * FORMAT ERREUR :
 * {
 *   "success":   false,
 *   "message":   "Email déjà utilisé",
 *   "errors":    ["email: doit être unique"],
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * AVANTAGES D'UNE RÉPONSE STANDARDISÉE :
 *   - Le frontend sait toujours où chercher le succès/erreur/données
 *   - Facilite la gestion des erreurs côté client
 *   - Simplifie les tests automatisés
 *   - Cohérence sur toutes les routes de l'API
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String  message;
    private T       data;           // Generic : peut être n'importe quel type

    /** Liste d'erreurs de validation (champ, message). */
    private List<String> errors;

    /** Horodatage de la réponse (UTC). */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Méthodes factory pour les cas fréquents ──────────────────────────

    /** Réponse de succès avec données. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    /** Réponse de succès sans données (ex: déconnexion). */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .build();
    }

    /** Réponse d'erreur. */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .build();
    }

    /** Réponse d'erreur avec liste de détails (validation). */
    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .errors(errors)
            .build();
    }
}
