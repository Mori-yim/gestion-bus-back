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
 * SERVICE TRAJET
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrajetService {

    private final TrajetRepository trajetRepository;

    /**
     * Liste tous les trajets disponibles (page d'accueil)
     */
    public List<TrajetResponse> getTrajetsDisponibles() {
        return trajetRepository
                .findByStatutOrderByDateDepartAsc(Trajet.StatutTrajet.PROGRAMME)
                .stream()
                .map(TrajetResponse::fromTrajet)
                .collect(Collectors.toList());
    }

    /**
     * Recherche des trajets selon les critères du formulaire
     */
    public List<TrajetResponse> rechercherTrajets(RechercheTrajetRequest request) {
        // Chercher du début de la journée jusqu'à minuit
        LocalDateTime dateDebut = request.getDateDepart().atStartOfDay();
        LocalDateTime dateFin = request.getDateDepart().atTime(23, 59, 59);

        return trajetRepository.rechercherTrajets(
                        request.getVilleDepart(),
                        request.getVilleArrivee(),
                        dateDebut,
                        dateFin,
                        request.getNombrePlaces()
                )
                .stream()
                .map(TrajetResponse::fromTrajet)
                .collect(Collectors.toList());
    }

    /**
     * Détail d'un trajet par son ID
     */
    public TrajetResponse getTrajetById(Long id) {
        Trajet trajet = trajetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé avec l'ID : " + id));
        return TrajetResponse.fromTrajet(trajet);
    }

    /**
     * Crée un nouveau trajet (ADMIN uniquement)
     */
    @Transactional
    public TrajetResponse createTrajet(CreateTrajetRequest request) {
        Trajet trajet = Trajet.builder()
                .villeDepart(request.getVilleDepart())
                .villeArrivee(request.getVilleArrivee())
                .dateDepart(request.getDateDepart())
                .dateArrivee(request.getDateArrivee())
                .prix(request.getPrix())
                .placesTotal(request.getPlacesTotal())
                .placesDisponibles(request.getPlacesTotal()) // Au début, tout est disponible
                .compagnie(request.getCompagnie())
                .numeroBus(request.getNumeroBus())
                .build();

        Trajet saved = trajetRepository.save(trajet);
        log.info("Nouveau trajet créé : {} → {} le {}",
                saved.getVilleDepart(), saved.getVilleArrivee(), saved.getDateDepart());

        return TrajetResponse.fromTrajet(saved);
    }

    /**
     * Annule un trajet (ADMIN)
     */
    @Transactional
    public TrajetResponse annulerTrajet(Long id) {
        Trajet trajet = trajetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé : " + id));

        trajet.setStatut(Trajet.StatutTrajet.ANNULE);
        return TrajetResponse.fromTrajet(trajetRepository.save(trajet));
    }
}


/**
 * ================================================================
 * SERVICE RÉSERVATION
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;

    /**
     * Crée une nouvelle réservation.
     *
     * Cette méthode est @Transactional car elle modifie DEUX tables :
     *   1. Crée une ligne dans "reservations"
     *   2. Décrémente placesDisponibles dans "trajets"
     *
     * Si une erreur survient, les DEUX opérations sont annulées (rollback).
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, User currentUser) {

        // 1. Récupérer le trajet avec verrouillage pessimiste
        // (évite les conditions de course si 2 users réservent en même temps)
        Trajet trajet = trajetRepository.findById(request.getTrajetId())
                .orElseThrow(() -> new RuntimeException("Trajet non trouvé"));

        // 2. Vérifier que le trajet est encore disponible
        if (trajet.getStatut() != Trajet.StatutTrajet.PROGRAMME) {
            throw new IllegalStateException("Ce trajet n'est plus disponible");
        }

        // 3. Vérifier qu'il y a assez de places
        if (trajet.getPlacesDisponibles() < request.getNombrePlaces()) {
            throw new IllegalStateException(
                    "Pas assez de places disponibles. Restant : " + trajet.getPlacesDisponibles()
            );
        }

        // 4. Calculer le montant total
        BigDecimal montantTotal = trajet.getPrix()
                .multiply(BigDecimal.valueOf(request.getNombrePlaces()));

        // 5. Générer un numéro de billet unique
        String numeroBillet = genererNumeroBillet();

        // 6. Créer la réservation
        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .trajet(trajet)
                .nombrePlaces(request.getNombrePlaces())
                .montantTotal(montantTotal)
                .numeroBillet(numeroBillet)
                .modePaiement(request.getModePaiement())
                // Simulation : en production, attendre la confirmation du paiement
                .statut(Reservation.StatutReservation.CONFIRMEE)
                .build();

        // 7. Décrémenter les places disponibles du trajet
        trajet.setPlacesDisponibles(trajet.getPlacesDisponibles() - request.getNombrePlaces());
        trajetRepository.save(trajet);

        // 8. Sauvegarder la réservation
        Reservation saved = reservationRepository.save(reservation);
        log.info("Réservation créée : {} pour {} ({})",
                numeroBillet, currentUser.getEmail(), trajet.getVilleDepart() + "→" + trajet.getVilleArrivee());

        // Recharger avec le trajet (pour éviter LazyInitializationException)
        saved.setTrajet(trajet);
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
     * Annule une réservation (par le client ou l'admin)
     */
    @Transactional
    public ReservationResponse annulerReservation(Long reservationId, User currentUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));

        // Seul le propriétaire ou un admin peut annuler
        boolean isOwner = reservation.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Vous n'êtes pas autorisé à annuler cette réservation");
        }

        // Vérifier que la réservation peut encore être annulée
        if (reservation.getStatut() == Reservation.StatutReservation.ANNULEE) {
            throw new IllegalStateException("Cette réservation est déjà annulée");
        }

        if (reservation.getStatut() == Reservation.StatutReservation.UTILISEE) {
            throw new IllegalStateException("Impossible d'annuler une réservation déjà utilisée");
        }

        // Remettre les places disponibles
        Trajet trajet = reservation.getTrajet();
        trajet.setPlacesDisponibles(trajet.getPlacesDisponibles() + reservation.getNombrePlaces());
        trajetRepository.save(trajet);

        // Mettre à jour le statut
        reservation.setStatut(Reservation.StatutReservation.ANNULEE);
        Reservation updated = reservationRepository.save(reservation);

        log.info("Réservation {} annulée par {}", reservation.getNumeroBillet(), currentUser.getEmail());

        updated.setTrajet(trajet);
        return ReservationResponse.fromReservation(updated);
    }

    /**
     * Génère un numéro de billet unique au format : BC-YYYYMM-XXXXXXXX
     * Exemple : BC-202412-A1B2C3D4
     */
    private String genererNumeroBillet() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "BC-" + datePart + "-" + uniquePart;
    }
}
