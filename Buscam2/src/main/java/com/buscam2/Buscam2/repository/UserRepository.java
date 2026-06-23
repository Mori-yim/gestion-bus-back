package com.buscam2.Buscam2.repository;

import com.buscam2.Buscam2.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * REPOSITORY UTILISATEUR

 * JpaRepository<User, Long> fournit GRATUITEMENT :
 *   - save(user)         → INSERT ou UPDATE
 *   - findById(id)       → SELECT WHERE id = ?
 *   - findAll()          → SELECT * FROM users
 *   - delete(user)       → DELETE WHERE id = ?
 *   - count()            → SELECT COUNT(*) FROM users
 *
 * On ajoute seulement les méthodes dont on a besoin en plus.
 * Spring Data JPA génère le SQL automatiquement depuis le nom de la méthode !
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Cherche un utilisateur par email (pour la connexion)
     * Spring traduit ceci en : SELECT * FROM users WHERE email = ?
     *
     * @param email l'email à chercher
     * @return Optional<User> : contient le user si trouvé, vide sinon
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un email est déjà utilisé (lors de l'inscription)
     * SELECT COUNT(*) FROM users WHERE email = ? > 0
     */
    boolean existsByEmail(String email);
}
