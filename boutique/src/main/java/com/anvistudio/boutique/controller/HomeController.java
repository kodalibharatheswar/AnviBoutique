package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.ContactMessage;
import com.anvistudio.boutique.service.ContactService;
import com.anvistudio.boutique.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {

    private final ProductService productService;
    private final ContactService contactService;

    public HomeController(ProductService productService, ContactService contactService) {
        this.productService = productService;
        this.contactService = contactService;
    }

    @GetMapping("/")
    public String home(Model model) {
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