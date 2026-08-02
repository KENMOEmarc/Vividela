package com.template.vivid.security;

import com.template.vivid.model.entity.User;
import com.template.vivid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implémentation de UserDetailsService — pont entre Spring Security et la base de données.
 * <p>
 * RÔLE :
 * Spring Security appelle loadUserByUsername() lors de chaque authentification
 * pour récupérer les informations de l'utilisateur (mot de passe haché, rôles,
 * état du compte) et les comparer aux credentials fournis.
 * <p>
 * FLUX D'UTILISATION :
 * 1. JwtAuthenticationFilter extrait l'email du token JWT
 * 2. Appelle loadUserByUsername(email) ici
 * 3. Retourne un UserDetails que Spring Security stocke dans le SecurityContext
 *
 * @Transactional(readOnly = true) :
 * - Lance une transaction en lecture seule → Hibernate peut optimiser (pas de dirty checking)
 * - Garantit la cohérence de lecture si d'autres opérations DB sont en cours
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son email ou son username.
     *
     * @param identifier Email ou username de l'utilisateur
     * @return UserDetails utilisé par Spring Security pour l'authentification
     * @throws UsernameNotFoundException si aucun utilisateur trouvé
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUserNameIgnoreCase(identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'identifiant: " + identifier
                ));

        /*
         * Conversion User (entité JPA) → UserDetails (interface Spring Security)
         *
         * org.springframework.security.core.userdetails.User.builder() :
         *   .username()    → identifiant principal (on utilise l'email)
         *   .password()    → hash BCrypt (Spring Security compare automatiquement)
         *   .authorities() → rôles/permissions (ROLE_USER par défaut)
         *   .disabled()    → compte désactivé → UsernameNotFoundException levée
         *   .accountLocked() → compte verrouillé
         */
        String authority = "ROLE_" + (user.getRole() != null ? user.getRole() : "CUSTOMER");

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .disabled(!user.getIsActive())
                .accountExpired(false)
                .credentialsExpired(false)
                .accountLocked(false)
                .build();
    }

    /**
     * Retrieves the user ID by email/username — used in controllers to get current user ID.
     */
    @Transactional(readOnly = true)
    public Long getUserIdByUsername(String identifier) {
        User user = userRepository.findByEmailOrUserNameIgnoreCase(identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé: " + identifier
                ));
        return user.getId();
    }

}