package com.template.vivid.config;

import com.template.vivid.security.JwtAuthenticationEntryPoint;
import com.template.vivid.security.JwtAuthenticationFilter;
import com.template.vivid.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // Active @PreAuthorize, @PostAuthorize sur les méthodes
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Endpoints accessibles sans authentification.
     * Tout le reste nécessite un token JWT valide
     * .
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/register",
            "/auth/login"
    };
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsServiceImpl userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Désactive CSRF (non pertinent pour une API REST stateless JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Active CORS avec la configuration définie dans CorsConfig.
                // OBLIGATOIRE en Spring Security 6 : sans cette ligne, les en-têtes CORS
                // ne sont pas ajoutés même si CorsConfigurationSource est déclaré en bean.
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Règles d'autorisation par URL et par rôle
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // Suppression : Admin uniquement (toutes les suppressions de l'application)
                        .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")

                        // Création / modification des utilisateurs : Admin, Manager ou Employé.
                        // La restriction fine "seul un Admin peut créer/promouvoir un Admin,
                        // seul un Admin/Manager peut créer/promouvoir un Manager" est appliquée
                        // dans UserServiceImpl, qui connaît le rôle du créateur ET le rôle cible.
                        .requestMatchers(HttpMethod.POST, "/users/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/users/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                        // Lecture : Admin, Manager ou Employé (GET /users/me est accessible à tous les rôles)
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/**").hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")

                        .anyRequest().authenticated()
                )

                // Force un 401 JSON cohérent (via JwtAuthenticationEntryPoint) pour
                // toute requête non authentifiée — token absent, expiré, révoqué
                // ou invalide. Sans cette déclaration explicite, Spring Security
                // pouvait retomber sur un comportement par défaut renvoyant un 403,
                // que l'intercepteur Axios du frontend ne traite pas comme une
                // session expirée (voir authApi.js) : l'utilisateur n'était alors
                // jamais déconnecté ni redirigé vers /login à l'expiration du token.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Pas de session HTTP : Spring Security ne crée pas de HttpSession
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Fournisseur d'authentification (UserDetails + BCrypt)
                .authenticationProvider(authenticationProvider())

                // Injecte notre filtre JWT AVANT le filtre standard username/password
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
