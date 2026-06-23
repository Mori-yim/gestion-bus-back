package com.buscam2.Buscam2.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ================================================================
 * ENTITÉ TRAJET (Trip)
 * ================================================================
 *
 * Représente un trajet de bus disponible à la réservation.
 * Ex : Douala → Yaoundé, départ 06h00, 45 places, 5000 FCFA
 *
 * Un Trajet contient plusieurs Réservations (OneToMany)
 * ================================================================
 */
@Entity
@Table(name = "trajets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ville de départ (ex: "Douala")
     */
    @Column(nullable = false)
    private String villeDepart;

    /**
     * Ville d'arrivée (ex: "Yaoundé")
     */
    @Column(nullable = false)
    private String villeArrivee;

    /**
     * Date et heure de départ
     * LocalDateTime inclut date + heure dans une seule valeur
     */
    @Column(nullable = false)
    private LocalDateTime dateDepart;

    /**
     * Date et heure d'arrivée estimée
     */
    @Column(nullable = false)
    private LocalDateTime dateArrivee;

    /**
     * Prix du billet en FCFA
     * BigDecimal pour les montants financiers (évite les erreurs de float)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    /**
     * Nombre total de places dans le bus
     */
    @Column(nullable = false)
    private Integer placesTotal;

    /**
     * Nombre de places encore disponibles
     * Décrément à chaque réservation confirmée
     */
    @Column(nullable = false)
    private Integer placesDisponibles;

    /**
     * Nom de la compagnie de bus (ex: "Général Express", "Binam Tours")
     */
    @Column(nullable = false)
    private String compagnie;

    /**
     * Numéro d'immatriculation ou identifiant du bus
     */
    private String numeroBus;

    /**
     * Statut du trajet
     * @Enumerated(STRING) stocke le nom (PROGRAMME, ANNULE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutTrajet statut = StatutTrajet.PROGRAMME;

    /**
     * Date de création de l'enregistrement
     * updatable = false : cette valeur ne change jamais après création
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Liste des réservations pour ce trajet
     *
     * @OneToMany : un trajet peut avoir plusieurs réservations
     * mappedBy = "trajet" : la FK est dans la table reservations
     * cascade = ALL : supprime les réservations si le trajet est supprimé
     * fetch = LAZY : ne charge les réservations que si on les demande (performance)
     */
    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================

    /**
     * Calcule la durée du trajet en heures (arrondie)
     */
    public long getDureeHeures() {
        return java.time.Duration.between(dateDepart, dateArrivee).toHours();
    }

    /**
     * Vérifie s'il reste des places disponibles
     */
    public boolean hasPlacesDisponibles() {
        return placesDisponibles > 0;
    }

    // ============================================================
    // ENUM STATUTS
    // ============================================================

    public enum StatutTrajet {
        PROGRAMME,    // Le trajet est planifié et disponible
        EN_COURS,     // Le bus est parti
        TERMINE,      // Arrivé à destination
        ANNULE        // Annulé (remboursement prévu)
    }
}
