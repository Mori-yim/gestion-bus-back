package com.buscam2.Buscam2.config;

import com.buscam2.Buscam2.entity.Trajet;
import com.buscam2.Buscam2.entity.User;
import com.buscam2.Buscam2.repository.TrajetRepository;
import com.buscam2.Buscam2.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 * DATA LOADER - Données de démo au démarrage
 * ================================================================
 *
 * CommandLineRunner s'exécute automatiquement après le démarrage
 * de Spring Boot. Ici on injecte des données de démonstration.
 *
 * Utile pour les tests et les démonstrations lors d'un entretien !
 * ================================================================
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    @Bean
    public CommandLineRunner loadData(
            UserRepository userRepository,
            TrajetRepository trajetRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            // Éviter de dupliquer les données si elles existent déjà
            if (userRepository.count() > 0) {
                log.info("Données déjà présentes, skip du DataLoader");
                return;
            }

            log.info("🚀 Chargement des données de démonstration...");

            // -------------------------------------------------------
            // CRÉER LES UTILISATEURS DE DÉMO
            // -------------------------------------------------------

            // Compte admin
            User admin = userRepository.save(User.builder()
                    .firstName("Admin")
                    .lastName("BusCam")
                    .email("admin@buscam.cm")
                    .password(passwordEncoder.encode("Admin123!"))
                    .phone("+237690000001")
                    .role(User.Role.ADMIN)
                    .build());

            // Compte client de démo
            User client = userRepository.save(User.builder()
                    .firstName("Jean")
                    .lastName("Kamga")
                    .email("jean.kamga@gmail.com")
                    .password(passwordEncoder.encode("Client123!"))
                    .phone("+237677123456")
                    .role(User.Role.CLIENT)
                    .build());

            log.info("✅ Utilisateurs créés : admin@buscam.cm / Client jean.kamga@gmail.com");

            // -------------------------------------------------------
            // CRÉER LES TRAJETS DE DÉMO
            // -------------------------------------------------------

            LocalDateTime base = LocalDateTime.now();

            // Trajet 1 : Douala → Yaoundé
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Douala")
                    .villeArrivee("Yaoundé")
                    .dateDepart(base.plusDays(1).withHour(6).withMinute(0))
                    .dateArrivee(base.plusDays(1).withHour(10).withMinute(0))
                    .prix(new BigDecimal("5000"))
                    .placesTotal(45)
                    .placesDisponibles(32)
                    .compagnie("Général Express")
                    .numeroBus("LT-456-CD")
                    .build());

            // Trajet 2 : Douala → Yaoundé (départ de soir)
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Douala")
                    .villeArrivee("Yaoundé")
                    .dateDepart(base.plusDays(1).withHour(18).withMinute(0))
                    .dateArrivee(base.plusDays(1).withHour(22).withMinute(30))
                    .prix(new BigDecimal("4500"))
                    .placesTotal(40)
                    .placesDisponibles(18)
                    .compagnie("Binam Tours")
                    .numeroBus("CE-789-YA")
                    .build());

            // Trajet 3 : Yaoundé → Douala
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Yaoundé")
                    .villeArrivee("Douala")
                    .dateDepart(base.plusDays(2).withHour(7).withMinute(0))
                    .dateArrivee(base.plusDays(2).withHour(11).withMinute(0))
                    .prix(new BigDecimal("5000"))
                    .placesTotal(45)
                    .placesDisponibles(40)
                    .compagnie("Vatican Express")
                    .numeroBus("DL-321-YA")
                    .build());

            // Trajet 4 : Douala → Bafoussam
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Douala")
                    .villeArrivee("Bafoussam")
                    .dateDepart(base.plusDays(1).withHour(8).withMinute(0))
                    .dateArrivee(base.plusDays(1).withHour(12).withMinute(0))
                    .prix(new BigDecimal("6000"))
                    .placesTotal(35)
                    .placesDisponibles(20)
                    .compagnie("Avenir Voyages")
                    .numeroBus("BF-654-DL")
                    .build());

            // Trajet 5 : Yaoundé → Ngaoundéré
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Yaoundé")
                    .villeArrivee("Ngaoundéré")
                    .dateDepart(base.plusDays(3).withHour(5).withMinute(30))
                    .dateArrivee(base.plusDays(3).withHour(15).withMinute(0))
                    .prix(new BigDecimal("12000"))
                    .placesTotal(50)
                    .placesDisponibles(45)
                    .compagnie("Touristique Express")
                    .numeroBus("ND-112-YA")
                    .build());

            // Trajet 6 : Douala → Limbé
            trajetRepository.save(Trajet.builder()
                    .villeDepart("Douala")
                    .villeArrivee("Limbé")
                    .dateDepart(base.plusDays(1).withHour(9).withMinute(0))
                    .dateArrivee(base.plusDays(1).withHour(11).withMinute(30))
                    .prix(new BigDecimal("2500"))
                    .placesTotal(30)
                    .placesDisponibles(12)
                    .compagnie("Coast Express")
                    .numeroBus("LB-445-DL")
                    .build());

            log.info("✅ 6 trajets de démo créés !");
            log.info("""
                    
                    ================================================
                    📋 COMPTES DE DÉMO :
                    
                    👔 ADMIN  : admin@buscam.cm / Admin123!
                    👤 CLIENT : jean.kamga@gmail.com / Client123!
                    ================================================
                    """);
        };
    }
}
