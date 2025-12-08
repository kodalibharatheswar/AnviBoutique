package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Product;
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.model.User; // NEW
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
    private final ContactService contactService;

    public AdminController(ProductService productService, UserService userService, ContactService contactService) {
        this.productService = productService;
        this.userService = userService;
        this.contactService = contactService;
    }


    // --- NEW: Admin Login/Credential Check Pre-Filter (No change needed) ---
    private String checkAdminCredentials(UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();

        // Only enforce for the hardcoded admin login ID
        if ("admin".equals(username)) {
            // Check if the user has updated credentials in the DB
            if (!userService.isAdminCredentialsUpdated(username)) {

                // If not updated, redirect them immediately to the profile page to force the update.
                redirectAttributes.addFlashAttribute("forceUpdateMessage", "SECURITY ALERT: Please update your default login credentials immediately.");
                return "redirect:/admin/profile";
            }
        }
        return null; // Credentials are fine, proceed to the target method
    }


    private String[] getAllCategories() {
        // ... (Category list remains the same)
        return new String[]{
                "Sarees", "Lehengas", "Kurtis", "Long Frocks", "Mom & Me", "Crop Top – Skirts",
                "Handlooms", "Casual Frocks", "Ready To Wear", "Dupattas", "Kids wear",
                "Dress Material", "Blouses", "Fabrics"
        };
    }



    // --- Dashboard & Product Listing (Filterable) ---
    @GetMapping("/dashboard")
    public String adminDashboard(
            @AuthenticationPrincipal UserDetails userDetails, // Added for security check
            @RequestParam(value = "category", required = false) String category,
            Model model, RedirectAttributes redirectAttributes) {

        // Run the security check first
        String securityRedirect = checkAdminCredentials(userDetails, model, redirectAttributes);
        if (securityRedirect != null) {
            return securityRedirect;
        }

        model.addAttribute("products", productService.getProductsByCategoryOrAll(category));
        model.addAttribute("newProduct", new Product());
        model.addAttribute("currentCategory", category);
        model.addAttribute("allCategories", getAllCategories()); // Pass categories for filtering/adding

        return "admin_dashboard"; // This must be the correct template for the admin UI
    }



    // --- Dashboard & Product Listing (Filterable) ---
    /*@GetMapping("/dashboard")
    public String adminDashboard(
            @RequestParam(value = "category", required = false) String category,
            Model model) {

        model.addAttribute("products", productService.getProductsByCategoryOrAll(category));
        model.addAttribute("newProduct", new Product());
        model.addAttribute("currentCategory", category);
        model.addAttribute("allCategories", getAllCategories()); // Pass categories for filtering/adding

        return "admin_dashboard"; // This must be the correct template for the admin UI
    }*/

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



    // --- Contact Message Management ---

    @GetMapping("/contacts")
    public String viewContactMessages(
            @AuthenticationPrincipal UserDetails userDetails, // Added for security check
            Model model, RedirectAttributes redirectAttributes) {

        // Run the security check first
        String securityRedirect = checkAdminCredentials(userDetails, model, redirectAttributes);
        if (securityRedirect != null) {
            return securityRedirect;
        }

        model.addAttribute("messages", contactService.getAllMessages());
        return "admin_contact_messages";
    }



    // --- NEW: Contact Message Management ---
    /*@GetMapping("/contacts")
    public String viewContactMessages(Model model) {
        model.addAttribute("messages", contactService.getAllMessages());
        return "admin_contact_messages"; // NEW TEMPLATE
    }*/

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


    // --- Admin Profile Update methods (Modified to fetch phone) ---
    @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        String username = userDetails.getUsername();

        // Fetch User object to get the recovery phone number
        Optional<User> adminUserOptional = userService.findUserByUsername(username);
        String recoveryPhone = adminUserOptional.map(User::getRecoveryPhoneNumber).orElse("");


        // Check if a message was flashed to the model (only happens on forced update redirect)
        if (!model.containsAttribute("forceUpdateMessage")) {
            // If the user navigated here normally, retrieve the update status from the DB
            boolean updated = userService.isAdminCredentialsUpdated(username);

            if (!updated && "admin".equals(username)) {
                model.addAttribute("forceUpdateMessage", "SECURITY ALERT: Please update your default login credentials immediately.");
            }
        }

        model.addAttribute("currentUsername", username);
        model.addAttribute("recoveryPhone", recoveryPhone); // NEW: Pass recovery phone

        return "admin_profile";
    }



    // --- Admin Profile Update methods (unchanged) ---
   /* @GetMapping("/profile")
    public String showAdminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "admin_profile";
    }*/




    /**
     * The POST handler for updating the admin profile.
     * MODIFIED: Added recoveryPhoneNumber parameter.
     */
    @PostMapping("/updateProfile")
    public String updateAdminProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("recoveryPhoneNumber") String recoveryPhoneNumber, // NEW PARAMETER
            RedirectAttributes redirectAttributes) {

        String currentUsername = userDetails.getUsername();

        try {
            // Pass the new phone number to the service layer
            userService.updateAdminCredentials(currentUsername, newUsername, newPassword, recoveryPhoneNumber);

            // NOTE: The user is logged out after updating credentials for security,
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
