package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService; // Import the UserService
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // Required for form parameters
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // Required for flash messages

/**
 * Controller for admin-specific functionality, requiring ROLE_ADMIN access.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final UserService userService; // NEW: Dependency for profile update

    // Constructor updated to inject UserService
    public AdminController(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    // --- Dashboard & Product Management ---
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("newProduct", new Product());
        return "admin_dashboard";
    }

    @PostMapping("/addProduct")
    public String addProduct(@ModelAttribute Product newProduct) {
        productService.saveProduct(newProduct);
        return "redirect:/admin/dashboard?success";
    }

    // --- New Functionality: Admin Profile Update ---

    /**
     * Displays the Admin profile page with the form to change credentials.
     */
    @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Pass the current username to the template
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "admin_profile"; // Maps to src/main/resources/templates/admin_profile.html
    }

    /**
     * Handles the POST request to change the admin's username and password.
     */
    @PostMapping("/updateProfile")
    public String updateAdminProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {

        // Get the current username (the one used for authentication)
        String currentUsername = userDetails.getUsername();

        try {
            // Call the service method to update/persist credentials
            userService.updateAdminCredentials(currentUsername, newUsername, newPassword);

            // Set a message to display on the login page
            redirectAttributes.addFlashAttribute("message", "Credentials updated successfully. Please log in with your new details.");

            // CRITICAL: Redirect to login page after credential change
            return "redirect:/login?updated";
        } catch (IllegalStateException e) {
            // Handle case where the new username is already taken
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/profile";
        } catch (Exception e) {
            // Handle other unexpected errors
            redirectAttributes.addFlashAttribute("error", "Error updating credentials: An unexpected error occurred.");
            return "redirect:/admin/profile";
        }
    }
}