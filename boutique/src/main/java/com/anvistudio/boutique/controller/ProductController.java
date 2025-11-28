package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Required for populating filter options

/**
 * Handles endpoints related to the product catalog and individual product pages.
 */
@Controller
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }


    // --- Helper method to define all available categories ---
    private String[] getAllCategories() {
        // Must match the list used in AdminController and navigation dropdowns
        return new String[]{
                "Sarees", "Lehengas", "Kurtis", "Long Frocks", "Mom & Me", "Crop Top – Skirts",
                "Handlooms", "Casual Frocks", "Ready To Wear", "Dupattas", "Kids wear",
                "Dress Material", "Blouses", "Fabrics"
        };
    }

    // --- Helper method to define available filter colors ---
    private String[] getFilterColors() {
        return new String[]{"Black", "Blue", "Green", "Red", "Pink", "Yellow", "Maroon", "Purple", "White", "Gray", "Brown", "Orange"};
    }


    /**
     * Displays the product listing page with filtering and sorting options.
     */
    @GetMapping("/products")
    public String viewProductCatalog(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sortBy", required = false, defaultValue = "latest") String sortBy,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "color", required = false) String color, // NEW PARAMETER
            Model model) {

        // Use the comprehensive filtering service method
        List<Product> products = productService.getFilteredProducts(
                category, sortBy, minPrice, maxPrice, status, color);

        model.addAttribute("products", products);
        model.addAttribute("currentCategory", category != null && !category.isEmpty() ? category : "All Products");

        // Pass filter states back to the view for form persistence
        model.addAttribute("selectedSortBy", sortBy);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedColor", color);
        model.addAttribute("minPriceValue", minPrice);
        model.addAttribute("maxPriceValue", maxPrice);

        // Pass filter lists to the view
        model.addAttribute("allCategories", getAllCategories());
        model.addAttribute("filterColors", getFilterColors()); // Pass filter color list

        return "products";
    }

    /**
     * NEW: Displays the individual product detail page.
     * URL example: /products/123
     */
    @GetMapping("/products/{id}")
    public String viewProductDetail(@PathVariable Long id, Model model) {
        Optional<Product> productOptional = productService.getProductById(id);

        if (productOptional.isEmpty() || !productOptional.get().getIsAvailable()) {
            // Throw a 404 error if the product doesn't exist
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
        }

        Product product = productOptional.get();
        model.addAttribute("product", product);

        // Suggest related products based on category (using the first 4 from the same category)
        List<Product> relatedProducts = productService.getProductsByCategoryOrAll(product.getCategory());
        // Filter out the current product and take up to 4 related items
        relatedProducts.removeIf(p -> p.getId().equals(id));
        model.addAttribute("relatedProducts", relatedProducts.subList(0, Math.min(relatedProducts.size(), 4)));

        return "product_detail"; // We need to create this new template
    }

    // Helper class for passing color options to Thymeleaf
    public static class FilterOption {
        private String name;
        private String hex;

        public FilterOption(String name, String hex) {
            this.name = name;
            this.hex = hex;
        }

        public String getName() { return name; }
        public String getHex() { return hex; }
    }
}