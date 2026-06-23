package com.buscam2.Buscam2.controller;

import com.buscam2.Buscam2.dto.Dto.*;
import com.buscam2.Buscam2.entity.User;
import com.buscam2.Buscam2.service.AuthService;
import com.buscam2.Buscam2.service.TrajetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ================================================================
 * CONTROLLER AUTHENTIFICATION
 * ================================================================
 *
 * @RestController : combine @Controller + @ResponseBody
 *                  (les méthodes retournent du JSON, pas des vues)
 * @RequestMapping : préfixe de toutes les URLs de ce controller
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     * Inscription d'un nouvel utilisateur
     *
     * @Valid : déclenche la validation des annotations (@NotBlank, @Email...)
     *          Retourne 400 Bad Request si validation échoue
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 Created
                .body(ApiResponse.success(authResponse, "Inscription réussie ! Bienvenue sur BusCam 🚌"));
    }

    /**
     * POST /api/v1/auth/login
     * Connexion d'un utilisateur existant
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Connexion réussie"));
    }

    /**
     * GET /api/v1/auth/me
     * Retourne les infos de l'utilisateur connecté
     *
     * @AuthenticationPrincipal : injecte automatiquement l'utilisateur
     *                             authentifié (extrait du token JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(UserResponse.fromUser(currentUser), "Profil récupéré")
        );
    }
}


/**
 * ================================================================
 * CONTROLLER TRAJETS
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/trajets")
@RequiredArgsConstructor
class TrajetController {

    private final TrajetService trajetService;

    /**
     * GET /api/v1/trajets
     * Liste tous les trajets disponibles (PUBLIC)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrajetResponse>>> getTrajets() {
        return ResponseEntity.ok(
                ApiResponse.success(trajetService.getTrajetsDisponibles(), "Trajets récupérés")
        );
    }

    /**
     * GET /api/v1/trajets/{id}
     * Détail d'un trajet (PUBLIC)
     *
     * @PathVariable : extrait {id} depuis l'URL
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrajetResponse>> getTrajet(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(trajetService.getTrajetById(id), "Trajet trouvé")
        );
    }

    /**
     * POST /api/v1/trajets/rechercher
     * Recherche de trajets selon critères (PUBLIC)
     */
    @PostMapping("/rechercher")
    public ResponseEntity<ApiResponse<List<TrajetResponse>>> rechercherTrajets(
            @Valid @RequestBody RechercheTrajetRequest request
    ) {
        List<TrajetResponse> trajets = trajetService.rechercherTrajets(request);
        String message = trajets.isEmpty()
                ? "Aucun trajet trouvé pour ces critères"
                : trajets.size() + " trajet(s) trouvé(s)";
        return ResponseEntity.ok(ApiResponse.success(trajets, message));
    }

    /**
     * POST /api/v1/trajets
     * Crée un nouveau trajet (ADMIN SEULEMENT)
     *
     * @PreAuthorize : vérifie le rôle AVANT d'exécuter la méthode
     *                 Retourne 403 Forbidden si pas ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrajetResponse>> createTrajet(
            @Valid @RequestBody CreateTrajetRequest request
    ) {
        TrajetResponse trajet = trajetService.createTrajet(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(trajet, "Trajet créé avec succès"));
    }

    /**
     * DELETE /api/v1/trajets/{id}/annuler
     * Annule un trajet (ADMIN)
     */
    @PutMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TrajetResponse>> annulerTrajet(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(trajetService.annulerTrajet(id), "Trajet annulé")
        );
    }
}


/**
 * ================================================================
 * CONTROLLER RÉSERVATIONS
 * ================================================================
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
class ReservationController {

    private final com.buscam.service.ReservationService reservationService;

    /**
     * POST /api/v1/reservations
     * Crée une réservation (CONNECTÉ)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReservationResponse reservation = reservationService.createReservation(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(reservation,
                        "Réservation confirmée ! Votre billet : " + reservation.getNumeroBillet()));
    }

    /**
     * GET /api/v1/reservations/mes-reservations
     * Liste mes réservations (CONNECTÉ)
     */
    @GetMapping("/mes-reservations")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getMesReservations(
            @AuthenticationPrincipal User currentUser
    ) {
        List<ReservationResponse> reservations = reservationService.getMesReservations(currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(reservations, reservations.size() + " réservation(s) trouvée(s)")
        );
    }

    /**
     * PUT /api/v1/reservations/{id}/annuler
     * Annule une réservation (PROPRIÉTAIRE ou ADMIN)
     */
    @PutMapping("/{id}/annuler")
    public ResponseEntity<ApiResponse<ReservationResponse>> annulerReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        ReservationResponse reservation = reservationService.annulerReservation(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(reservation, "Réservation annulée"));
    }
}
