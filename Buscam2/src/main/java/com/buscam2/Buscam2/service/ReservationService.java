package com.buscam2.Buscam2.service;

import com.buscam.dto.Dto.*;
import com.buscam.entity.Reservation;
import com.buscam.entity.Trajet;
import com.buscam.entity.User;
import com.buscam.repository.ReservationRepository;
import com.buscam.repository.TrajetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ================================================================
 * SERVICE RÉSERVATION (Bean public Spring)
 * ================================================================
 *
 * Ce fichier expose ReservationService comme un @Service Spring
 * injecté dans ReservationController.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;

    /**
     * Crée une nouvelle réservation.
     *
     * @Transactional garantit que si une erreur survient,
     * les deux opérations (réservation + mise à jour des places)
     * sont annulées ensemble (rollback atomique).
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, User currentUser) {

        // 1. Récupérer le trajet
        Trajet trajet = trajetRepository.findById(request.getTrajetId())
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé avec l'ID : " + request.getTrajetId()));

        // 2. Vérifier la disponibilité
        if (trajet.getStatut() != Trajet.StatutTrajet.PROGRAMME) {
            throw new IllegalStateException("Ce trajet n'est plus disponible à la réservation");
        }

        // 3. Vérifier les places
        if (trajet.getPlacesDisponibles() < request.getNombrePlaces()) {
            throw new IllegalStateException(
                    "Pas assez de places. Disponibles : " + trajet.getPlacesDisponibles()
                            + ", demandées : " + request.getNombrePlaces()
            );
        }

        // 4. Calculer le montant total
        BigDecimal montantTotal = trajet.getPrix()
                .multiply(BigDecimal.valueOf(request.getNombrePlaces()));

        // 5. Générer un numéro de billet unique
        String numeroBillet = genererNumeroBillet();

        // 6. Construire et sauvegarder la réservation
        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .trajet(trajet)
                .nombrePlaces(request.getNombrePlaces())
                .montantTotal(montantTotal)
                .numeroBillet(numeroBillet)
                .modePaiement(request.getModePaiement())
                .statut(Reservation.StatutReservation.CONFIRMEE)
                .build();

        // 7. Décrémenter les places disponibles
        trajet.setPlacesDisponibles(trajet.getPlacesDisponibles() - request.getNombrePlaces());
        trajetRepository.save(trajet);

        // 8. Sauvegarder la réservation
        Reservation saved = reservationRepository.save(reservation);
        saved.setTrajet(trajet); // injecter pour éviter LazyInitializationException

        log.info("✅ Réservation {} créée pour {} | Trajet : {} → {}",
                numeroBillet, currentUser.getEmail(),
                trajet.getVilleDepart(), trajet.getVilleArrivee());

        return ReservationResponse.fromReservation(saved);
    }

    /**
     * Récupère toutes les réservations d'un utilisateur
     */
    public List<ReservationResponse> getMesReservations(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ReservationResponse::fromReservation)
                .collect(Collectors.toList());
    }

    /**
     * Annule une réservation existante
     */
    @Transactional
    public ReservationResponse annulerReservation(Long reservationId, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée avec l'ID : " + reservationId));

        // Vérification des droits
        boolean isOwner = reservation.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Vous n'êtes pas autorisé à annuler cette réservation");
        }

        // Vérification du statut
        if (reservation.getStatut() == Reservation.StatutReservation.ANNULEE) {
            throw new IllegalStateException("Cette réservation est déjà annulée");
        }
        if (reservation.getStatut() == Reservation.StatutReservation.UTILISEE) {
            throw new IllegalStateException("Impossible d'annuler une réservation déjà utilisée");
        }

        // Rendre les places
        Trajet trajet = reservation.getTrajet();
        trajet.setPlacesDisponibles(trajet.getPlacesDisponibles() + reservation.getNombrePlaces());
        trajetRepository.save(trajet);

        // Annuler
        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        Reservation updated = reservationRepository.save(reservation);
        updated.setTrajet(trajet);

        log.info("❌ Réservation {} annulée par {}", reservation.getNumeroBillet(), currentUser.getEmail());

        return ReservationResponse.fromReservation(updated);
    }

    /**
     * Génère un numéro de billet unique : BC-YYYYMM-XXXXXXXX
     * Exemple : BC-202412-A1B2C3D4
     */
    private String genererNumeroBillet() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BC-" + datePart + "-" + uniquePart;
    }
}
