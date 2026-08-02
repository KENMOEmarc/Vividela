package com.template.vivid.model.enums;

/**
 * Rôles utilisateur de l'application.
 *
 * Stocké en base sous forme de chaîne (ex: "ADMIN") via @Enumerated(EnumType.STRING).
 * Spring Security expose ces rôles avec le préfixe "ROLE_" (ex: "ROLE_ADMIN").
 *
 * Hiérarchie des permissions :
 *   ADMIN    → accès complet (lecture, création, modification, suppression)
 *   MANAGER  → gestion de boutique (lecture, création, modification)
 *   EMPLOYEE → lecture + création + modification (pas de suppression)
 *   CUSTOMER → tableau de bord uniquement (pas d'accès à la gestion utilisateurs)
 */
public enum RoleType {
    ADMIN,
    MANAGER,
    EMPLOYEE,
    CUSTOMER
}
