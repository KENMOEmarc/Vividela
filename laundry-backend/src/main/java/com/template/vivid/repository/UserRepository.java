package com.template.vivid.repository;

import com.template.vivid.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository JPA pour l'entité User.
 *
 * SPRING DATA JPA :
 *   En étendant JpaRepository<User, Long>, Spring génère automatiquement
 *   au runtime une implémentation complète avec :
 *
 *   CRUD de base fourni automatiquement :
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  save(user)           → INSERT ou UPDATE (selon l'id)   │
 *   │  findById(id)         → SELECT WHERE id = ?             │
 *   │  findAll()            → SELECT * FROM users             │
 *   │  deleteById(id)       → DELETE WHERE id = ?             │
 *   │  count()              → SELECT COUNT(*) FROM users      │
 *   │  existsById(id)       → SELECT 1 WHERE id = ?          │
 *   └──────────────────────────────────────────────────────────┘
 *
 * MÉTHODES DÉRIVÉES (Query by method name) :
 *   Spring Data interprète le nom de la méthode et génère la requête SQL.
 *   Ex: findByEmail → SELECT * FROM users WHERE email = ?
 *
 * JPQL (@Query) :
 *   Pour les requêtes complexes non dérivables du nom, on écrit du JPQL
 *   (Java Persistence Query Language) qui opère sur les entités, pas les tables.
 *
 * @Repository : marque ce composant comme couche d'accès aux données.
 *   Spring traduit les exceptions JPA en DataAccessException standardisées.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son email (insensible à la casse).
     *
     * JPQL généré :
     *   SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)
     *
     * Optional<T> : évite les NullPointerException. Le code appelant
     * doit explicitement gérer le cas "utilisateur non trouvé".
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Recherche par nom d'utilisateur (insensible à la casse).
     */
    Optional<User> findByUserNameIgnoreCase(String userName);

    /**
     * Vérifie si un email est déjà utilisé.
     * Plus efficace que findByEmail() car génère un SELECT EXISTS.
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Vérifie si un nom d'utilisateur est déjà pris.
     */
    boolean existsByUserNameIgnoreCase(String userName);

    /**
     * Recherche flexible : accepte email OU username comme identifiant.
     *
     * Utilisé lors de la connexion pour permettre à l'utilisateur de
     * s'authentifier avec l'un ou l'autre.
     *
     * @Query avec JPQL personnalisé car la logique OR ne peut pas être
     * exprimée par un simple nom de méthode.
     */
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.email) = LOWER(:identifier)
           OR LOWER(u.userName) = LOWER(:identifier)
        """)
    Optional<User> findByEmailOrUserNameIgnoreCase(@Param("identifier") String identifier);

    /**
     * Récupère tous les utilisateurs possédant l'un des rôles donnés.
     * Utilisé notamment pour notifier le personnel (ADMIN/EMPLOYEE) des
     * alertes de stock, qui ne concernent pas un client en particulier.
     */
    java.util.List<User> findByRoleIn(java.util.Collection<String> roles);

    /**
     * Recherche par numéro de téléphone.
     * Utilisée comme repli lorsqu'une commande est créée sans username connu
     * (cas fréquent en boutique où le caissier recherche par téléphone).
     * Voir revue de code — "findCustomer — recherche par username uniquement".
     */
    Optional<User> findByPhone(String phone);

    /**
     * AJOUT : vérifie l'unicité du téléphone à la création d'un client. Voir
     * revue de code, règle manquante n°2 (section Utilisateurs / Clients) —
     * resolveCustomer() (OrderServiceImpl) utilise le téléphone comme
     * critère de recherche, un doublon rendrait la résolution ambiguë.
     */
    boolean existsByPhone(String phone);
}
