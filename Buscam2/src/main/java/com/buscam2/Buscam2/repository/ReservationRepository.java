package com.buscam2.Buscam2.repository;

import com.buscam2.Buscam2.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY RÉSERVATION
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Récupère toutes les réservations d'un utilisateur
     * Triées par date de création décroissante (les plus récentes en premier)
     */
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Récupère une réservation par son numéro de billet unique
     * Utilisé pour la validation à l'embarquement
     */
    Optional<Reservation> findByNumeroBillet(String numeroBillet);

    /**
     * Compte les réservations confirmées pour un trajet
     * Utile pour l'admin : voir combien de billets vendus
     */
    long countByTrajetIdAndStatut(Long trajetId, Reservation.StatutReservation statut);

    /**
     * Vérifie si un user a déjà réservé un trajet spécifique
     */
    boolean existsByUserIdAndTrajetId(Long userId, Long trajetId);
}
