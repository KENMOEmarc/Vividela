package com.template.vivid.model.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) pour la requête d'inscription.
 *
 * POURQUOI UN DTO ET PAS L'ENTITÉ DIRECTEMENT ?
 *   - Sépare la représentation API (ce que le client envoie) de la
 *     représentation DB (ce qu'on stocke) → évite d'exposer les champs
 *     sensibles ou internes (id, createdAt, password haché…)
 *   - Permet d'avoir des validations spécifiques à l'API (ex: confirmPassword)
 *   - Protège contre la "mass assignment" (injection de champs non prévus)
 *
 * FLUX : Client JSON → @RequestBody RegisterRequest → validation → Service → User (entité)
 *
 * VALIDATION JAKARTA :
 *   @Valid sur le @RequestBody déclenche la validation automatiquement.
 *   En cas d'erreur → GlobalExceptionHandler intercepte MethodArgumentNotValidException
 *   et renvoie une liste d'erreurs structurée.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    /**
     * Nom d'utilisateur : 3-50 caractères, lettres/chiffres/underscores uniquement.
     * Le regex ^[a-zA-Z0-9_]+$ empêche les caractères spéciaux qui pourraient
     * causer des problèmes d'injection ou d'encodage.
     */
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores"
    )
    private String userName;

    /**
     * Email validé par l'annotation @Email (format RFC 5322 simplifié).
     */
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne peut pas dépasser 150 caractères")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Size(min = 9, max = 20, message = "Le numéro de téléphone ne peut pas dépasser 20 caractères")
    private String phone;

    /**
     * Mot de passe en clair (reçu du client via HTTPS).
     * Le regex impose : min 8 chars, au moins une majuscule, une minuscule,
     * un chiffre et un caractère spécial → politique de sécurité forte.
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir entre 8 et 100 caractères")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
        message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&)"
    )
    private String password;

    /**
     * Confirmation du mot de passe : doit correspondre à password.
     * La vérification de correspondance est faite dans le service,
     * pas ici, pour garder les DTOs simples (pas de logique métier).
     */
    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    private String confirmPassword;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String firstName;

    @NotBlank(message = "Le nom de famille est obligatoire")
    @Size(max = 100, message = "Le nom de famille ne peut pas dépasser 100 caractères")
    private String lastName;

}
