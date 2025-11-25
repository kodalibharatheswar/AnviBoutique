package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.service.CartService;
import com.anvistudio.boutique.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated.");
        }
        return userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB."));
    }

    /**
     * Displays the user's shopping cart page. Requires authentication.
     */
    @GetMapping("/cart")
    public String viewCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        // --- NEW LOGIC: Check if user is authenticated ---
        if (userDetails == null || "anonymousUser".equals(userDetails.getUsername())) {
            // Redirect to the new public view if not logged in
            return "redirect:/cart-unauth";
        }
        // Assuming user is authenticated past this point.

        User user = getAuthenticatedUser(userDetails);

        List<com.anvistudio.boutique.model.CartItem> items = cartService.getCartItems(user.getId());
        double total = cartService.getCartTotal(user.getId());

        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", total);

        return "cart"; // Maps to src/main/resources/templates/cart.html
    }

    /**
     * API Endpoint: Adds a product to the cart.
     * Maps to a POST request when a user clicks "Add to Cart" on a product page.
     */
    @PostMapping("/cart/add")
    public String addProductToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes) {

        try {
            // Check authentication status (already handled by SecurityConfig, but good practice)
            if (userDetails == null) {
                return "redirect:/login";
            }

            cartService.addProductToCart(userDetails.getUsername(), productId, quantity);
            redirectAttributes.addFlashAttribute("cartMessage", "Item added to cart successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("cartError", "Error adding item to cart: " + e.getMessage());
        }

        // Redirect back to the cart view page
        return "redirect:/cart";
    }

    /**
     * API Endpoint: Removes an item completely from the cart using the CartItem ID.
     */
    @PostMapping("/cart/remove/{itemId}")
    public String removeItemFromCart(@PathVariable Long itemId) {
        cartService.removeItem(itemId);
        return "redirect:/cart";
    }

    /**
     * API Endpoint: Updates the quantity of a specific item using the CartItem ID.
     */
    @PostMapping("/cart/update")
    public String updateCartItemQuantity(
            @RequestParam Long itemId,
            @RequestParam int quantity) {

        cartService.updateItemQuantity(itemId, quantity);
        return "redirect:/cart";
    }
}