package com.anvistudio.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Date; // Required for the new field

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal price; // Using BigDecimal for precise currency handling

    @Column(nullable = false)
    private String category; // e.g., Sarees, Lehengas, Kurtis

    private String imageUrl; // URL to the product image

    @Column(nullable = false)
    private Integer stockQuantity; // How many units are available

    // === FIX APPLIED HERE ===
    /**
     * Required field for `findTop8ByOrderByDateCreatedDesc()` query.
     * Set on creation and cannot be updated.
     */
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated = new Date(); // Automatically initialized
    // ========================
}