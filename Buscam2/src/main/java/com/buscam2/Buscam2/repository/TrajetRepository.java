package com.buscam2.Buscam2.repository;

import com.buscam2.Buscam2.entity.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REPOSITORY TRAJET

 * Méthodes de recherche pour la fonctionnalité principale :
 * "Chercher un trajet Douala → Yaoundé le 25/12/2024"
 */
@Repository
public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    /**
     * Recherche des trajets par ville de départ et d'arrivée
     * (insensible à la casse avec IgnoreCase)
     *
     * Exemple : findByVilleDepartIgnoreCaseAndVilleArriveeIgnoreCase("douala", "Yaoundé")
     * → trouve "Douala → Yaoundé", "DOUALA → yaoundé", etc.
     */
    List<Trajet> findByVilleDepartIgnoreCaseAndVilleArriveeIgnoreCase(
            String villeDepart,
            String villeArrivee
    );

    /**
     * Recherche avancée avec date et places disponibles
     *
     * @Query permet d'écrire du JPQL (Java Persistence Query Language)
     * JPQL ressemble au SQL mais utilise les noms des classes Java, pas des tables
     *
     * :villeDepart, :villeArrivee, :dateDebut, :dateFin → paramètres nommés
     */
    @Query("""
            SELECT t FROM Trajet t
            WHERE LOWER(t.villeDepart) = LOWER(:villeDepart)
            AND LOWER(t.villeArrivee) = LOWER(:villeArrivee)
            AND t.dateDepart BETWEEN :dateDebut AND :dateFin
            AND t.placesDisponibles >= :nombrePlaces
            AND t.statut = 'PROGRAMME'
            ORDER BY t.dateDepart ASC
            """)
    List<Trajet> rechercherTrajets(
            @Param("villeDepart") String villeDepart,
            @Param("villeArrivee") String villeArrivee,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("nombrePlaces") int nombrePlaces
    );

    /**
     * Récupère tous les trajets programmés (pour la liste publique)
     */
    List<Trajet> findByStatutOrderByDateDepartAsc(Trajet.StatutTrajet statut);

    /**
     * Récupère les trajets disponibles dans une ville de départ
     */
    List<Trajet> findByVilleDepartIgnoreCaseAndStatut(
            String villeDepart,
            Trajet.StatutTrajet statut
    );
}
