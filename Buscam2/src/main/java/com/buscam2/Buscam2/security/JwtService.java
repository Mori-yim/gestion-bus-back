package com.buscam2.Buscam2.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ================================================================
 * SERVICE JWT (JSON Web Token)
 * ================================================================
 *
 * JWT = système d'authentification SANS SESSION côté serveur.
 *
 * Comment ça marche :
 * 1. User se connecte → serveur génère un token JWT signé
 * 2. Frontend stocke le token (localStorage)
 * 3. À chaque requête, frontend envoie : "Authorization: Bearer <token>"
 * 4. Serveur vérifie la signature → si valide → autorise la requête
 *
 * Structure d'un JWT : header.payload.signature
 *   - header  : type de token + algorithme (HS256)
 *   - payload : données (email, rôle, expiration)
 *   - signature: hash(header + payload + secret) → garantit l'authenticité
 * ================================================================
 */
@Service
public class JwtService {

    /**
     * Clé secrète pour signer les tokens (depuis application.properties)
     * DOIT être gardée SECRÈTE et ne JAMAIS être committée sur GitHub
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Durée de validité du token en millisecondes (ex: 86400000 = 24h)
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ============================================================
    // GÉNÉRATION DU TOKEN
    // ============================================================

    /**
     * Génère un JWT token pour l'utilisateur connecté
     *
     * @param userDetails l'utilisateur Spring Security
     * @return le token JWT sous forme de String
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Génère un JWT avec des claims personnalisées
     *
     * @param extraClaims données supplémentaires à inclure dans le token
     * @param userDetails l'utilisateur
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)                    // Claims personnalisées (ex: rôle)
                .setSubject(userDetails.getUsername())     // Email de l'utilisateur
                .setIssuedAt(new Date(System.currentTimeMillis()))       // Date de création
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Expiration
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)      // Signature HS256
                .compact();                                // Génère le String final
    }

    // ============================================================
    // VALIDATION DU TOKEN
    // ============================================================

    /**
     * Vérifie si un token est valide pour un utilisateur donné
     * - Le token appartient bien à cet utilisateur ?
     * - Le token n'est pas expiré ?
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Vérifie si le token est expiré
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ============================================================
    // EXTRACTION DES DONNÉES DU TOKEN
    // ============================================================

    /**
     * Extrait l'email (username) du token JWT
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrait la date d'expiration du token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Méthode générique pour extraire n'importe quelle claim du token
     *
     * @param token           le JWT
     * @param claimsResolver  fonction qui extrait la donnée voulue
     * @param <T>             type de la donnée extraite
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Décode et vérifie la signature du token pour extraire toutes les claims
     *
     * @throws JwtException si le token est invalide ou falsifié
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())  // Vérifie la signature avec la clé secrète
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Convertit la clé secrète (String Base64) en objet Key cryptographique
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
