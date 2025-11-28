package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.repository.ProductRepository;
import com.anvistudio.boutique.repository.CartItemRepository; // NEW
import com.anvistudio.boutique.repository.WishlistRepository; // NEW
import org.springframework.transaction.annotation.Transactional; // NEW
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional; // NEW
import java.util.Comparator; // NEW

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository; // NEW
    private final WishlistRepository wishlistRepository; // NEW

    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository, WishlistRepository wishlistRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.wishlistRepository = wishlistRepository;
    }

    /**
     * Retrieves a single product by its ID.
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /**
     * Retrieves all products (used for admin view).
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }


    /**
     * NEW: Retrieves products based on multiple filter and sort criteria.
     * **FIXED: Now accepts 6 arguments including color.**
     */
    public List<Product> getFilteredProducts(String category, String sortBy, Double minPrice, Double maxPrice, String status, String color) {
        List<Product> products;

        // 1. Base Retrieval (Filter by Category at the DB level)
        if (category != null && !category.trim().isEmpty()) {
            products = productRepository.findByCategory(category.trim());
        } else {
            products = productRepository.findAll();
        }

        // 2. Filter by Price (In-memory filtering)
        if (minPrice != null || maxPrice != null) {
            products.removeIf(p -> {
                double price = p.getPrice().doubleValue();
                if (minPrice != null && price < minPrice) return true;
                if (maxPrice != null && price > maxPrice) return true;
                return false;
            });
        }

        // 3. Filter by Color (In-memory filtering using the new productColor field)
        if (color != null && !color.trim().isEmpty()) {
            final String normalizedColor = color.trim().toLowerCase();
            products.removeIf(p -> p.getProductColor() == null || !p.getProductColor().toLowerCase().contains(normalizedColor));
        }


        // 4. Filter by Status (In-memory filtering based on stock)
        if (status != null && !status.isEmpty()) {
            switch (status) {
                case "inStock":
                    // Filter: Stock quantity greater than 0 AND is Available
                    products.removeIf(p -> p.getStockQuantity() <= 0 || !p.getIsAvailable());
                    break;
                case "lowStock":
                    // Filter: Stock quantity between 1 and 5
                    products.removeIf(p -> p.getStockQuantity() <= 0 || p.getStockQuantity() > 5 || !p.getIsAvailable());
                    break;
                case "onSale":
                    // Requires an 'isOnSale' field, placeholder logic
                    products.removeIf(p -> true);
                    break;
            }
        }

        // 5. Sort (In-memory sorting)
        if (sortBy != null && !sortBy.isEmpty()) {
            Comparator<Product> comparator;
            switch (sortBy) {
                case "priceAsc":
                    comparator = Comparator.comparing(Product::getPrice);
                    break;
                case "priceDesc":
                    comparator = Comparator.comparing(Product::getPrice).reversed();
                    break;
                case "oldest":
                    comparator = Comparator.comparing(Product::getDateCreated);
                    break;
                case "latest":
                default:
                    comparator = Comparator.comparing(Product::getDateCreated).reversed();
                    break;
            }
            products.sort(comparator);
        }

        // Final filter: Only show products marked as 'isAvailable' to customers
        products.removeIf(p -> !p.getIsAvailable());

        return products;
    }



    /**
     * Retrieves the top 8 latest products for display (uses the sorting query).
     */
    public List<Product> getDisplayableProducts() {
        return productRepository.findTop8ByOrderByDateCreatedDesc();
    }

    /**
     * Retrieves products based on category, or all products if category is null/empty.
     */
    public List<Product> getProductsByCategoryOrAll(String category) {
        if (category != null && !category.trim().isEmpty()) {
            return productRepository.findByCategory(category.trim());
        }
        return productRepository.findAll();
    }

    /**
     * Admin function: Saves a new product or updates an existing one.
     */
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    /**
     * NEW: Admin function to delete a product, performing necessary cleanup first.
     */
    @Transactional // Ensure all steps (cleanup and delete) succeed or fail together
    public void deleteProduct(Long id) {

        // 1. Cleanup: Remove product from all customer carts
        cartItemRepository.deleteByProductId(id);

        // 2. Cleanup: Remove product from all customer wishlists
        wishlistRepository.deleteByProductId(id);

        // 3. Delete the product itself
        productRepository.deleteById(id);
    }
}