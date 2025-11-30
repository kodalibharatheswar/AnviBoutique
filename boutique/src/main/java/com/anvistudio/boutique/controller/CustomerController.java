package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.service.CartService; // NEW
import com.anvistudio.boutique.service.WishlistService; // NEW
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

/**
 * Controller for customer-specific pages, requiring ROLE_CUSTOMER access.
 */
@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService; // NEW FIELD
    private final WishlistService wishlistService; // NEW FIELD

    // CONSTRUCTOR UPDATED to inject all necessary services
    public CustomerController(ProductService productService, UserService userService,
                              CartService cartService, WishlistService wishlistService) {
        this.productService = productService;
        this.userService = userService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
    }

    /**
     * Shows the customer dashboard, displaying products and customer details.
     * @param userDetails The details of the currently logged-in customer (from Spring Security).
     */
    @GetMapping("/dashboard")
    public String customerDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        return "redirect:/";
    }
}