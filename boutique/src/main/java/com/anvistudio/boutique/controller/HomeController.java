package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.ContactMessage;
import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.service.ContactService;
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class HomeController {

    private final ProductService productService;
    private final ContactService contactService;
    private final UserService userService;

    public HomeController(ProductService productService, ContactService contactService, UserService userService) {
        this.productService = productService;
        this.contactService = contactService;
        this.userService = userService;
    }

    /**
     * Main homepage view, now handles both authenticated and unauthenticated users.
     */
    @GetMapping("/")
    public String home(@AuthenticationPrincipal(expression = "null") UserDetails userDetails, Model model) {

        // Check if the user is authenticated (not anonymous)
        if (userDetails != null && !"anonymousUser".equals(userDetails.getUsername())) {
            // Fetch Customer details if authenticated
            Optional<Customer> customerOptional = userService.getCustomerDetailsByUsername(userDetails.getUsername());
            if (customerOptional.isPresent()) {
                // Pass full Customer object to the model for first name access
                model.addAttribute("customer", customerOptional.get());
            }
            else {
                // Fallback username if customer details are missing (e.g., if only User exists)
                model.addAttribute("username", userDetails.getUsername());
            }

            // Also pass flash messages from redirects (Cart/Wishlist actions)
            // Note: These are automatically available via Model if set by RedirectAttributes
        }

        // --- NEW: Add flash attributes from footer subscription/other redirects ---
        // This is necessary because RedirectAttributes are consumed by the first GET request.
        // We ensure they are visible on the model regardless of the fragment context.
        if (model.containsAttribute("successMessage")) {
            model.addAttribute("successMessage", model.getAttribute("successMessage"));
        }
        if (model.containsAttribute("errorMessage")) {
            model.addAttribute("errorMessage", model.getAttribute("errorMessage"));
        }
        // --- END NEW ---

        // Fetch products for display on the home page for everyone
        model.addAttribute("products", productService.getDisplayableProducts());

        return "index";
    }

    @GetMapping("/about")
    public String aboutUs() {
        return "about";
    }

    @GetMapping("/contact")
    public String showContactForm(Model model) {
        model.addAttribute("contactMessage", new ContactMessage());
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContactForm(@ModelAttribute ContactMessage contactMessage, RedirectAttributes redirectAttributes) {
        try {
            contactService.saveMessage(contactMessage);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your message! We will get back to you shortly.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "There was an error submitting your message. Please try again.");
        }
        return "redirect:/contact";
    }

    // =========================================================================
    // NEW: Policy Mappings (CRITICAL FIX for accessing static policy pages)
    // =========================================================================

    @GetMapping("/policy_return")
    public String returnPolicy() {
        // Automatically resolves to src/main/resources/templates/policy_return.html
        return "policy_return";
    }

    @GetMapping("/policy_privacy")
    public String privacyPolicy() {
        // Automatically resolves to src/main/resources/templates/policy_privacy.html
        return "policy_privacy";
    }

    @GetMapping("/policy_terms")
    public String termsAndConditions() {
        // Automatically resolves to src/main/resources/templates/policy_terms.html
        return "policy_terms";
    }

    @GetMapping("/policy_shipping")
    public String shippingPolicy() {
        // Automatically resolves to src/main/resources/templates/policy_shipping.html
        return "policy_shipping";
    }


    /**
     * NEW: Handler for the Customization Request page.
     */
    @GetMapping("/custom-request")
    public String customRequestPage() {
        return "custom_request"; // Maps to the new template
    }

    // --- NEW REQUIREMENT: Public pages for unauthenticated Wishlist/Cart ---

    /**
     * Displays a special page for unauthenticated users clicking the Wishlist.
     */
    @GetMapping("/wishlist-unauth")
    public String showUnauthWishlist() {
        return "unauth_wishlist"; // NEW TEMPLATE (will be created/renamed)
    }

    /**
     * Displays a special page for unauthenticated users clicking the Cart.
     */
    @GetMapping("/cart-unauth")
    public String showUnauthCart() {
        return "unauth_cart"; // NEW TEMPLATE (will be created/renamed)
    }
}