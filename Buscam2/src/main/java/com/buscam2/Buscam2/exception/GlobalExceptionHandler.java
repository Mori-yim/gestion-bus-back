package com.buscam2.Buscam2.exception;

import com.buscam2.Buscam2.dto.Dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * ================================================================
 * GESTIONNAIRE GLOBAL DES EXCEPTIONS
 * ================================================================
 *
 * @RestControllerAdvice : intercepte toutes les exceptions levées
 * dans n'importe quel controller et les transforme en réponses JSON.
 *
 * Sans ça, Spring renvoie des erreurs HTML (peu pratique pour React).
 * Avec ça, toutes les erreurs sont au format { success, message, data }.
 * ================================================================
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation (@Valid)
     * Ex: email invalide, champ vide, mot de passe trop court...
     * Retourne 400 Bad Request avec le détail de chaque erreur
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        // Collecter toutes les erreurs de validation
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Erreur de validation : {}", errors);

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Données invalides")
                        .data(errors)
                        .build());
    }

    /**
     * Gère les mauvais credentials (email/mdp incorrect)
     * Retourne 401 Unauthorized
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email ou mot de passe incorrect"));
    }

    /**
     * Gère les erreurs de droits insuffisants
     * Retourne 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Accès refusé : droits insuffisants"));
    }

    /**
     * Gère les erreurs métier (ex: "Trajet complet", "Email déjà pris")
     * Retourne 400 Bad Request
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBusinessErrors(RuntimeException ex) {
        log.warn("Erreur métier : {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Gère les ressources introuvables (ex: "Trajet non trouvé")
     * Retourne 404 Not Found
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("non trouvé")) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ex.getMessage()));
        }
        // Erreur inattendue → 500 Internal Server Error
        log.error("Erreur inattendue : ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Une erreur interne s'est produite"));
    }
}
