package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.service.ContactService; // NEW
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable; // NEW
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional; // NEW

/**
 * Controller for admin-specific functionality, requiring ROLE_ADMIN access.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserService userService;
    private final ContactService contactService; // NEW

    public AdminController(ProductService productService, UserService userService, ContactService contactService) {
        this.productService = productService;
        this.userService = userService;
        this.contactService = contactService; // In
    }

    // --- Product Management Helper Method for Categories ---
    private String[] getAllCategories() {
        // List of ALL categories for the dropdown filter (Manual list for now)
        return new String[]{
                "Sarees", "Lehengas", "Kurtis", "Long Frocks", "Mom & Me", "Crop Top – Skirts",
                "Handlooms", "Casual Frocks", "Ready To Wear", "Dupattas", "Kids wear",
                "Dress Material", "Blouses", "Fabrics"
        };
    }

    // --- Dashboard & Product Listing (Filterable) ---
    @GetMapping("/dashboard")
    public String adminDashboard(
            @RequestParam(value = "category", required = false) String category,
            Model model) {

        model.addAttribute("products", productService.getProductsByCategoryOrAll(category));
        model.addAttribute("newProduct", new Product());
        model.addAttribute("currentCategory", category);
        model.addAttribute("allCategories", getAllCategories()); // Pass categories for filtering/adding

        return "admin_dashboard"; // This must be the correct template for the admin UI
    }

    @PostMapping("/addProduct")
    public String addProduct(@ModelAttribute Product newProduct, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(newProduct);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error adding product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }


    // --- NEW: Product Delete Functionality ---
    @PostMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product ID " + id + " deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting product: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }


    // --- NEW: Edit Product Functionality ---

    /**
     * Displays the form to edit an existing product.
     */
    @GetMapping("/product/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        Optional<Product> productOptional = productService.getProductById(id);

        if (productOptional.isEmpty()) {
            // If product is not found, redirect back to dashboard
            return "redirect:/admin/dashboard";
        }

        model.addAttribute("product", productOptional.get());
        model.addAttribute("allCategories", getAllCategories()); // Pass categories for selection

        return "admin_edit_product"; // NEW TEMPLATE
    }


    // --- NEW: Contact Message Management ---

    @GetMapping("/contacts")
    public String viewContactMessages(Model model) {
        model.addAttribute("messages", contactService.getAllMessages());
        return "admin_contact_messages"; // NEW TEMPLATE
    }

    @PostMapping("/contact/delete/{id}")
    public String deleteContactMessage(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            contactService.deleteMessage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Message ID " + id + " deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting message: " + e.getMessage());
        }
        return "redirect:/admin/contacts";
    }

    /**
     * Handles the update of an existing product.
     */
    @PostMapping("/product/update")
    public String updateProduct(@ModelAttribute("product") Product updatedProduct, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(updatedProduct); // save() works for both insert and update
            redirectAttributes.addFlashAttribute("successMessage", "Product updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product: " + e.getMessage());
        }
        // Redirect back to the dashboard, potentially maintaining the current filter
        return "redirect:/admin/dashboard";
    }

    // --- Admin Profile Update methods (unchanged) ---
    @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "admin_profile";
    }

    @PostMapping("/updateProfile")
    public String updateAdminProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {

        String currentUsername = userDetails.getUsername();

        try {
            userService.updateAdminCredentials(currentUsername, newUsername, newPassword);
            redirectAttributes.addFlashAttribute("message", "Credentials updated successfully. Please log in with your new details.");
            return "redirect:/login?updated";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating credentials: An unexpected error occurred.");
            return "redirect:/admin/profile";
        }
    }
}
