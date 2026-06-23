package com.buscam2.Buscam2.dto;

import com.buscam2.Buscam2.entity.Reservation;
import com.buscam2.Buscam2.entity.Trajet;
import com.buscam2.Buscam2.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 * DTOs (Data Transfer Objects) - BusCam
 * ================================================================
 *
 * Les DTOs sont des objets intermédiaires entre l'API et les entités JPA.
 *
 * Pourquoi des DTOs ?
 *   1. SÉCURITÉ : ne jamais exposer directement l'entité User
 *      (le mot de passe haché ne doit jamais sortir en JSON !)
 *   2. FLEXIBILITÉ : adapter la forme des données selon le besoin
 *   3. VALIDATION : @NotNull, @Email, etc. directement sur les champs
 *
 * Convention : toutes les classes DTO sont ici dans un seul fichier
 * pour la lisibilité, en utilisant des classes statiques imbriquées.
 * ================================================================
 */
public class Dto {

    // ============================================================
    // AUTH DTOs
    // ============================================================

    /**
     * Données envoyées par le frontend lors de l'inscription
     * POST /api/v1/auth/register
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {

        @NotBlank(message = "Le prénom est obligatoire")
        private String firstName;

        @NotBlank(message = "Le nom est obligatoire")
        private String lastName;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
        private String password;

        @Pattern(regexp = "^(\\+237)?[6][5-9][0-9]{7}$",
                message = "Numéro camerounais invalide (ex: +237 6XX XXX XXX)")
        private String phone;
    }

    /**
     * Données envoyées lors de la connexion
     * POST /api/v1/auth/login
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Format d'email invalide")
        private String email;

        @NotBlank(message = "Le mot de passe est obligatoire")
        private String password;
    }

    /**
     * Réponse envoyée au frontend après connexion/inscription réussie
     * Contient le JWT token et les infos de l'utilisateur
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;         // Le JWT à stocker dans localStorage
        private String tokenType;     // Toujours "Bearer"
        private UserResponse user;    // Infos du user connecté

        /**
         * Factory method : crée une AuthResponse depuis un token et un User
         */
        public static AuthResponse of(String token, User user) {
            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(UserResponse.fromUser(user))
                    .build();
        }
    }

    // ============================================================
    // USER DTOs
    // ============================================================

    /**
     * Réponse utilisateur (sans le mot de passe !)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String role;
        private String createdAt;

        /**
         * Convertit une entité User en UserResponse (masque le mot de passe)
         */
        public static UserResponse fromUser(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .role(user.getRole().name())
                    .createdAt(user.getCreatedAt().toString())
                    .build();
        }
    }

    // ============================================================
    // TRAJET DTOs
    // ============================================================

    /**
     * Réponse trajet envoyée au frontend
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrajetResponse {
        private Long id;
        private String villeDepart;
        private String villeArrivee;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateDepart;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateArrivee;

        private BigDecimal prix;
        private Integer placesDisponibles;
        private Integer placesTotal;
        private String compagnie;
        private String numeroBus;
        private String statut;
        private long dureeHeures;

        public static TrajetResponse fromTrajet(Trajet trajet) {
            return TrajetResponse.builder()
                    .id(trajet.getId())
                    .villeDepart(trajet.getVilleDepart())
                    .villeArrivee(trajet.getVilleArrivee())
                    .dateDepart(trajet.getDateDepart())
                    .dateArrivee(trajet.getDateArrivee())
                    .prix(trajet.getPrix())
                    .placesDisponibles(trajet.getPlacesDisponibles())
                    .placesTotal(trajet.getPlacesTotal())
                    .compagnie(trajet.getCompagnie())
                    .numeroBus(trajet.getNumeroBus())
                    .statut(trajet.getStatut().name())
                    .dureeHeures(trajet.getDureeHeures())
                    .build();
        }
    }

    /**
     * Body de la requête de recherche de trajets
     * POST /api/v1/trajets/rechercher
     */
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RechercheTrajetRequest {

        @NotBlank(message = "La ville de départ est obligatoire")
        private String villeDepart;

        @NotBlank(message = "La ville d'arrivée est obligatoire")
        private String villeArrivee;

        @NotNull(message = "La date de départ est obligatoire")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private java.time.LocalDate dateDepart;

        @Min(value = 1, message = "Minimum 1 place")
        @Max(value = 10, message = "Maximum 10 places à la fois")
        @Builder.Default
        private int nombrePlaces = 1;
    }

    /**
     * Body pour créer un trajet (admin seulement)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTrajetRequest {

        @NotBlank
        private String villeDepart;

        @NotBlank
        private String villeArrivee;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateDepart;

        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateArrivee;

        @NotNull
        @DecimalMin(value = "0.01", message = "Le prix doit être positif")
        private BigDecimal prix;

        @NotNull
        @Min(value = 1)
        @Max(value = 100)
        private Integer placesTotal;

        @NotBlank
        private String compagnie;

        private String numeroBus;
    }

    // ============================================================
    // RÉSERVATION DTOs
    // ============================================================

    /**
     * Body pour créer une réservation
     * POST /api/v1/reservations
     */
    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReservationRequest {

        @NotNull(message = "L'ID du trajet est obligatoire")
        private Long trajetId;

        @Min(value = 1)
        @Max(value = 10)
        @Builder.Default
        private int nombrePlaces = 1;

        @NotNull(message = "Le mode de paiement est obligatoire")
        private Reservation.ModePaiement modePaiement;

        // Numéro de téléphone Mobile Money (optionnel, pour MTN/Orange)
        private String numeroMobileMoney;
    }

    /**
     * Réponse réservation complète (avec infos du trajet)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationResponse {
        private Long id;
        private String numeroBillet;
        private TrajetResponse trajet;
        private Integer nombrePlaces;
        private BigDecimal montantTotal;
        private String statut;
        private String modePaiement;
        private String createdAt;

        public static ReservationResponse fromReservation(Reservation reservation) {
            return ReservationResponse.builder()
                    .id(reservation.getId())
                    .numeroBillet(reservation.getNumeroBillet())
                    .trajet(TrajetResponse.fromTrajet(reservation.getTrajet()))
                    .nombrePlaces(reservation.getNombrePlaces())
                    .montantTotal(reservation.getMontantTotal())
                    .statut(reservation.getStatut().name())
                    .modePaiement(reservation.getModePaiement() != null
                            ? reservation.getModePaiement().name() : null)
                    .createdAt(reservation.getCreatedAt().toString())
                    .build();
        }
    }

    // ============================================================
    // RÉPONSE GÉNÉRIQUE API
    // ============================================================

    /**
     * Enveloppe standardisée pour toutes les réponses de l'API.
     * Permet au frontend d'avoir une structure cohérente.
     *
     * Exemple de réponse JSON :
     * {
     *   "success": true,
     *   "message": "Réservation créée avec succès",
     *   "data": { ... }
     * }
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
