package com.buscam2.Buscam2.service;

import com.buscam.dto.Dto.*;
import com.buscam.entity.User;
import com.buscam.repository.UserRepository;
import com.buscam.security.JwtService;
import com.buscam2.Buscam2.dto.Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ================================================================
 * SERVICE D'AUTHENTIFICATION
 * ================================================================
 *
 * Gère l'inscription et la connexion des utilisateurs.
 * Produit des tokens JWT en cas de succès.
 *
 * @Slf4j    : active le logger (log.info, log.error, etc.) via Lombok
 * @Service  : marque cette classe comme un bean Spring (injectable)
 * @Transactional : chaque méthode s'exécute dans une transaction BDD
 *                  (rollback automatique si une exception est levée)
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Inscrit un nouvel utilisateur et retourne un JWT.
     *
     * @param request les données d'inscription validées par @Valid
     * @return AuthResponse contenant le token JWT et les infos user
     * @throws IllegalArgumentException si l'email est déjà utilisé
     */
    @Transactional
    public AuthResponse register(Dto.@Valid RegisterRequest request) {

        // Vérifier que l'email n'est pas déjà pris
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Un compte existe déjà avec l'email : " + request.getEmail()
            );
        }

        // Construire l'entité User avec le mot de passe haché
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                // IMPORTANT : hacher le mot de passe AVANT de sauvegarder !
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.CLIENT)  // Nouveau user = CLIENT par défaut
                .build();

        // Sauvegarder en BDD
        User savedUser = userRepository.save(user);
        log.info("Nouvel utilisateur inscrit : {} ({})", savedUser.getFullName(), savedUser.getEmail());

        // Générer le JWT pour connexion automatique après inscription
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.of(token, savedUser);
    }

    /**
     * Connecte un utilisateur existant.
     *
     * @param request email + mot de passe
     * @return AuthResponse avec le nouveau token JWT
     * @throws org.springframework.security.core.AuthenticationException si échec
     */
    public AuthResponse login(LoginRequest request) {

        // AuthenticationManager vérifie email + mot de passe contre la BDD
        // Lance BadCredentialsException si les credentials sont incorrects
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Si on arrive ici, les credentials sont corrects
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Générer un nouveau token JWT
        String token = jwtService.generateToken(user);

        log.info("Connexion réussie pour : {}", user.getEmail());

        return AuthResponse.of(token, user);
    }
}
