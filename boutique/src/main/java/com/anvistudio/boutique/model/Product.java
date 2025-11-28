package com.anvistudio.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

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

    // Existing: Description (Set to TEXT)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String category;

    private String imageUrl;

    // *** NEW FIELD ADDED ***
    // 9. Product Color (Used for filtering)
    private String productColor;
    // ***********************

    @Column(nullable = false)
    private Integer stockQuantity;

    // Existing: Date Created
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateCreated = new Date();

    // ===================================
    // === NEW FIELDS FOR RICH PRODUCT INFO ===
    // ===================================

    // 1. SKU (Unique Identifier)
    @Column(unique = true, nullable = true)
    private String sku;

    // 2. Size Options (e.g., S, M, L or 38, 40)
    @Column(columnDefinition = "VARCHAR(255)") // Can be short, comma-separated list
    private String sizeOptions;

    // 3. Size Guide Link
    private String sizeGuideUrl;

    // 4. Delivery Information
    private String estimatedDelivery; // e.g., "4-6 business days"

    // 5. Delivery and Return Policy Details
    @Column(columnDefinition = "TEXT")
    private String deliveryAndReturnPolicy;

    // 6. Detailed Fabric/Wash Care Information
    @Column(columnDefinition = "TEXT")
    private String additionalInformation;

    // 7. SEO Tags
    @Column(columnDefinition = "VARCHAR(512)")
    private String productTags; // Comma-separated SEO tags

    // 8. Availability/Visibility toggle
    @Column(nullable = false)
    private Boolean isAvailable = true; // Default: visible

}