package com.buscam2.Buscam2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ================================================================
 * FILTRE JWT - Intercepte CHAQUE requête HTTP
 * ================================================================
 *
 * Ce filtre s'exécute AVANT les controllers pour chaque requête.
 * Il lit le header "Authorization", extrait et valide le token JWT,
 * puis authentifie l'utilisateur dans le contexte Spring Security.
 *
 * Flow :
 *   Requête HTTP → JwtAuthFilter → Controller → Service → Réponse
 *
 * OncePerRequestFilter garantit que ce filtre s'exécute une seule
 * fois par requête (pas de double exécution).
 * ================================================================
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lire le header Authorization
        // Format attendu : "Bearer eyJhbGciOiJIUzI1NiJ9...."
        final String authHeader = request.getHeader("Authorization");

        // Si pas de header ou format incorrect → passer au filtre suivant
        // (la requête sera bloquée par Spring Security si l'endpoint est protégé)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraire le token (après "Bearer ")
        final String jwt = authHeader.substring(7);

        // 3. Extraire l'email depuis le token JWT
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token malformé ou signature invalide
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Si on a un email ET que l'utilisateur n'est pas encore authentifié
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 5. Charger l'utilisateur depuis la BDD
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Valider le token (correspond à cet user ? Non expiré ?)
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 7. Créer l'objet d'authentification Spring Security
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,                          // pas de credentials (on a le token)
                                userDetails.getAuthorities()  // rôles de l'utilisateur
                        );

                // 8. Ajouter les détails de la requête (IP, session...)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. Enregistrer l'authentification dans le contexte de sécurité
                // Après ça, @AuthenticationPrincipal fonctionnera dans les controllers
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Passer au filtre/controller suivant
        filterChain.doFilter(request, response);
    }
}
