package com.buscam2.Buscam2.service;

import com.buscam2.Buscam2.dto.Dto.*;
import com.buscam2.Buscam2.entity.Reservation;
import com.buscam2.Buscam2.entity.Trajet;
import com.buscam2.Buscam2.entity.User;
import com.buscam2.Buscam2.repository.ReservationRepository;
import com.buscam2.Buscam2.repository.TrajetRepository;
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
 * SERVICE TRAJET
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
