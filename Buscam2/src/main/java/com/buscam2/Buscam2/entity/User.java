package com.buscam2.Buscam2.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ENTITÉ UTILISATEUR
 *
 * Cette classe représente un utilisateur en base de données.
 * Elle implémente UserDetails de Spring Security pour l'authentification JWT.
 *
 * @Entity      : dit à JPA de créer une table "users" pour cette classe
 * @Table       : personnalise le nom de la table
 * @Getter/@Setter/@Builder : Lombok génère automatiquement les méthodes
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * Clé primaire auto-incrémentée par la BDD (séquence PostgreSQL)
     * IDENTITY = la BDD gère l'auto-incrément
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prénom de l'utilisateur
     * nullable = false : le champ est obligatoire en BDD
     */
    @Column(nullable = false)
    private String firstName;

    /**
     * Nom de famille
     */
    @Column(nullable = false)
    private String lastName;

    /**
     * Email = identifiant de connexion (unique dans la BDD)
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Mot de passe haché avec BCrypt (JAMAIS en clair !)
     */
    @Column(nullable = false)
    private String password;

    /**
     * Numéro de téléphone (optionnel, utilisé pour MTN/Orange Money)
     */
    @Column(length = 20)
    private String phone;

    /**
     * Rôle de l'utilisateur dans l'application
     * @Enumerated(STRING) : stocke "CLIENT" ou "ADMIN" (pas 0/1)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.CLIENT;

    /**
     * Date de création du compte (auto-remplie à la création)
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // MÉTHODES DE UserDetails (Spring Security)
    // Ces méthodes permettent à Spring Security de gérer
    // l'authentification de cet utilisateur

    /**
     * Retourne les rôles/permissions de l'utilisateur.
     * Spring Security utilise "ROLE_" comme préfixe convention.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * L'email sert de "username" pour Spring Security
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Ces méthodes retournent true = compte toujours valide.
     * En production, tu peux ajouter de la logique (ex: email vérifié)
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    /**
     * Prénom + Nom complet (utile pour l'affichage)
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ENUM RÔLES

    /**
     * Rôles possibles dans l'application :
     * - CLIENT : peut réserver des billets
     * - ADMIN  : gère les trajets, les bus, les réservations
     */
    public enum Role {
        CLIENT,
        ADMIN
    }
}
