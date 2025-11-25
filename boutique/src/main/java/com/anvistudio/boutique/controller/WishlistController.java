package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.model.Wishlist;
import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.service.WishlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    /**
     * Displays the user's wishlist page. Requires authentication.
     */
    @GetMapping("/wishlist")
    public String viewWishlist(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        // Check if the user is anonymous (unauthenticated)
        if (userDetails == null || "anonymousUser".equals(userDetails.getUsername())) {
            // Redirect to the new public view if not logged in
            return "redirect:/wishlist-unauth";
        }

        User user = userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB."));

        List<Wishlist> items = wishlistService.getWishlistItems(user.getId());
        model.addAttribute("wishlistItems", items);

        return "wishlist"; // Maps to src/main/resources/templates/wishlist.html
    }

    /**
     * API Endpoint: Adds a product to the wishlist.
     */
    @PostMapping("/wishlist/add/{productId}")
    public String addProductToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        try {
            wishlistService.addToWishlist(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("wishlistMessage", "Product added to your Wishlist!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("wishlistError", "Could not add item to wishlist: " + e.getMessage());
        }

        // Typically redirects back to the product detail page or current view
        return "redirect:/products/" + productId;
    }

    /**
     * API Endpoint: Removes a product from the wishlist.
     */
    @PostMapping("/wishlist/remove/{productId}")
    public String removeProductFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        // FIX: Use the new service method
        User user = userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB."));

        try {
            wishlistService.removeFromWishlist(user.getId(), productId);
            redirectAttributes.addFlashAttribute("wishlistMessage", "Item removed from Wishlist.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("wishlistError", "Could not remove item from wishlist: " + e.getMessage());
        }

        // Redirects back to the main wishlist view
        return "redirect:/wishlist";
    }
}