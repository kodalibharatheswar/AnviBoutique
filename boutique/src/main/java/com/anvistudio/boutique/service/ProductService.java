package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Retrieves all products (used for admin view).
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Retrieves the top 8 latest products for display (uses the sorting query).
     */
    public List<Product> getDisplayableProducts() {
        return productRepository.findTop8ByOrderByDateCreatedDesc();
    }

    /**
     * Admin function: Saves a new product or updates an existing one.
     */
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }
}