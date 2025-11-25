package com.anvistudio.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users") // Use 'users' to avoid potential conflicts with 'user' reserved word
@Data // Lombok for getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // Will store BCrypt encoded password

    // Role can be "CUSTOMER" or "ADMIN"
    @Column(nullable = false)
    private String role;
}