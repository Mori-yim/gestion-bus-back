package com.buscam2.Buscam2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ENTITÉ RÉSERVATION
 *
 * Relie un Utilisateur à un Trajet.
 * Contient le prix payé, le statut, et le numéro de billet unique.
 *
 * Relations :
 *   - @ManyToOne User  : plusieurs réservations pour un même user
 *   - @ManyToOne Trajet: plusieurs réservations sur un même trajet
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * L'utilisateur qui a fait cette réservation
     *
     * @ManyToOne : plusieurs réservations peuvent appartenir au même user
     * @JoinColumn : nom de la colonne FK dans la table reservations
     * LAZY : ne charge pas le User complet si non nécessaire (performance)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Le trajet réservé
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    /**
     * Nombre de places réservées (généralement 1)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer nombrePlaces = 1;

    /**
     * Prix total payé au moment de la réservation
     * (snapshot du prix, qui peut changer pour les futurs trajets)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    /**
     * Numéro de billet unique au format : BC-2024-XXXXXXXX
     * Généré automatiquement lors de la création
     */
    @Column(nullable = false, unique = true, length = 20)
    private String numeroBillet;

    /**
     * Statut de la réservation
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutReservation statut = StatutReservation.EN_ATTENTE;

    /**
     * Mode de paiement utilisé
     */
    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;

    /**
     * Référence de transaction du paiement Mobile Money
     * (Ex: "MTN-1234567890", "ORANGE-9876543210")
     */
    private String referenceTransaction;

    /**
     * Date de création de la réservation
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Date de confirmation/annulation
     */
    private LocalDateTime updatedAt;

    // ============================================================
    // CALLBACK JPA : exécuté automatiquement avant mise à jour
    // ============================================================

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ENUMS

    public enum StatutReservation {
        EN_ATTENTE,   // Réservation créée, paiement non confirmé
        CONFIRMEE,    // Paiement confirmé, billet valide
        ANNULEE,      // Annulée par le client ou l'admin
        UTILISEE      // Client a embarqué, trajet terminé
    }

    public enum ModePaiement {
        MTN_MOBILE_MONEY,
        ORANGE_MONEY,
        CARTE_BANCAIRE,
        ESPECES
    }
}
