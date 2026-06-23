package com.buscam2.Buscam2.config;

import com.buscam2.Buscam2.repository.UserRepository;
import com.buscam2.Buscam2.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ================================================================
 * CONFIGURATION SPRING SECURITY
 * ================================================================
 *
 * Cette classe configure :
 *   1. Quels endpoints sont publics vs protégés
 *   2. L'algorithme de hachage des mots de passe (BCrypt)
 *   3. Le filtre JWT (ajouté avant le filtre d'auth classique)
 *   4. CORS (autorise les requêtes du frontend React)
 *   5. Désactivation de CSRF (inutile avec JWT stateless)
 *
 * @EnableMethodSecurity : active @PreAuthorize("hasRole('ADMIN')")
 *                          sur les méthodes des controllers
 * ================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserRepository userRepository;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Définit les règles de sécurité HTTP.
     * C'est le "routeur" de Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Désactiver CSRF : inutile avec JWT (pas de cookies de session)
                .csrf(AbstractHttpConfigurer::disable)

                // Activer CORS avec notre configuration personnalisée
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Définir les règles d'autorisation par endpoint
                .authorizeHttpRequests(auth -> auth

                        // ---- ENDPOINTS PUBLICS (pas besoin de token) ----

                        // Authentification : inscription et connexion
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Recherche et liste des trajets (vitrine publique)
                        .requestMatchers(HttpMethod.GET, "/api/v1/trajets/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/trajets/rechercher").permitAll()

                        // ---- ENDPOINTS ADMIN SEULEMENT ----
                        .requestMatchers(HttpMethod.POST, "/api/v1/trajets").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/trajets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/trajets/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // ---- TOUT LE RESTE : connecté (n'importe quel rôle) ----
                        .anyRequest().authenticated()
                )

                // Politique de session : STATELESS = aucune session HTTP côté serveur
                // Le token JWT suffit pour authentifier chaque requête
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Notre fournisseur d'authentification personnalisé
                .authenticationProvider(authenticationProvider())

                // Ajouter le filtre JWT AVANT le filtre d'auth classique de Spring
                // (sinon Spring essaie de chercher une session qui n'existe pas)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Configuration CORS : autorise le frontend React à appeler notre API.
     *
     * CORS = Cross-Origin Resource Sharing
     * Sans ça, le navigateur bloque les requêtes entre localhost:5173 et localhost:8080
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées (depuis application.properties)
        // Ex: "http://localhost:5173,https://buscam.vercel.app"
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Headers autorisés (Authorization = notre token JWT)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

        // Expose le header Authorization dans la réponse (pour que React puisse le lire)
        configuration.setExposedHeaders(List.of("Authorization"));

        // Autorise l'envoi de cookies/credentials cross-origin
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    /**
     * UserDetailsService : comment Spring Security charge un utilisateur.
     * Ici on cherche par email dans notre BDD.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé avec l'email : " + username
                ));
    }

    /**
     * AuthenticationProvider : combine le UserDetailsService et le PasswordEncoder.
     * Vérifie que le mot de passe fourni correspond au hash en BDD.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService());
        //authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * AuthenticationManager : point d'entrée pour l'authentification.
     * Utilisé dans AuthService pour valider email + mot de passe.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCryptPasswordEncoder : hachage des mots de passe.
     *
     * BCrypt est un algorithme de hachage "salted" (résistant aux rainbow tables).
     * Le "strength" 10 = 2^10 = 1024 rounds de hachage (bon équilibre sécurité/performance)
     *
     * JAMAIS stocker un mot de passe en clair !
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
