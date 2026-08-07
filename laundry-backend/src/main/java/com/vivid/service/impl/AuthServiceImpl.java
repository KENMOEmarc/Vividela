package com.vivid.service.impl;

import com.vivid.exception.InvalidCredentialsException;
import com.vivid.model.payloads.responses.AuthResponse;
import com.vivid.model.payloads.requests.LoginRequest;
import com.vivid.model.payloads.requests.RegisterRequest;
import com.vivid.model.dto.UserDto;
import com.vivid.model.entity.User;
import com.vivid.model.mapper.UserMapper;
import com.vivid.repository.UserRepository;
import com.vivid.security.JwtTokenProvider;
import com.vivid.service.AuthService;
import com.vivid.service.TokenBlacklistService;
import com.vivid.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service d'authentification.
 * <p>
 * ORCHESTRATION :
 * AuthServiceImpl coordonne plusieurs composants :
 * ┌─────────────────────────────────────────────────────────┐
 * │  UserService          → inscription / recherche user    │
 * │  AuthenticationManager → vérification credentials      │
 * │  JwtTokenProvider     → génération / parsing JWT       │
 * │  TokenBlacklistService → blacklist MySQL (logout)      │
 * └─────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Inscription + connexion automatique.
     * <p>
     * On appelle UserService.register() pour créer l'utilisateur,
     * puis on génère directement un token JWT — l'utilisateur est
     * connecté immédiatement après son inscription.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        // Délègue la création de l'utilisateur au UserService
        UserDto user = userService.register(request);

        // Génère le JWT pour la connexion automatique post-inscription
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUserName(),
                user.getEmail()
        );

        log.info("Inscription réussie et token généré pour: {}", user.getEmail());

        return buildAuthResponse(token, user);
    }

    /**
     * Connexion avec vérification des credentials.
     * <p>
     * FLUX D'AUTHENTIFICATION SPRING SECURITY :
     * 1. authenticationManager.authenticate() reçoit un token username/password
     * 2. Délègue à DaoAuthenticationProvider (configuré dans SecurityConfig)
     * 3. DaoAuthenticationProvider appelle UserDetailsService.loadUserByUsername()
     * 4. Compare le mot de passe fourni avec le hash BCrypt en base
     * 5. Si OK → authentication réussie ; sinon → BadCredentialsException
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Tentative de connexion pour: {}", request.getIdentifier());

        try {
            // Spring Security authentifie et lève une exception si invalide
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getIdentifier(),
                            request.getPassword()
                    )
            );
        } catch (DisabledException e) {
            throw new InvalidCredentialsException("Ce compte a été désactivé");
        } catch (BadCredentialsException e) {
            // Message générique → ne révèle pas si c'est l'email ou le MDP qui est faux
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        // Authentification réussie → charge l'entité complète depuis la DB
        User user = userRepository.findByEmailOrUserNameIgnoreCase(request.getIdentifier())
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides"));

        // Génère le token JWT
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUserName(),
                user.getEmail()
        );

        log.info("Connexion réussie pour: {}", user.getEmail());

        return buildAuthResponse(token, UserMapper.toDto(user));
    }

    /**
     * Déconnexion : révoque le token dans la table revoked_tokens.
     * <p>
     * Même si le token est encore valide cryptographiquement,
     * JwtAuthenticationFilter refusera tout accès si le JTI est blacklisté.
     */
    @Override
    public void logout(String token) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String jti = jwtTokenProvider.extractJti(token);
            long ttlMs = jwtTokenProvider.getRemainingTtlMs(token);
            tokenBlacklistService.blacklist(jti, ttlMs);
            log.info("Token blacklisté (JTI: {}), TTL restant: {}ms", jti, ttlMs);
        }
    }

    /**
     * Construit la réponse d'authentification standardisée.
     * Factorisé car identique pour login et register.
     */
    private AuthResponse buildAuthResponse(String token, UserDto user) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)   // ms → secondes
                .user(UserMapper.toUserInfo(UserMapper.toUser(user)))
                .build();
    }
}
