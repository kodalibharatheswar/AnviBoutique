package com.anvistudio.boutique.repository;

import com.anvistudio.boutique.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds products by category (useful for filtering the customer view).
     */
    List<Product> findByCategory(String category);

    /**
     * Finds the latest products, useful for the New Arrivals section.
     * This method now resolves correctly against the updated Product entity.
     */
    List<Product> findTop8ByOrderByDateCreatedDesc();
}