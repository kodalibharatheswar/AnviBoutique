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

        if (userDetails == null || "anonymousUser".equals(userDetails.getUsername())) {
            return "redirect:/wishlist-unauth";
        }

        User user = userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB."));

        List<Wishlist> items = wishlistService.getWishlistItems(user.getId());
        model.addAttribute("wishlistItems", items);

        return "wishlist";
    }

    /**
     * API Endpoint: Adds a product to the wishlist.
     * FIX: Matches the Path Variable used in index.html: @{/wishlist/add/{productId}}
     */
    @PostMapping("/wishlist/add/{productId}")
    public String addProductToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            wishlistService.addToWishlist(userDetails.getUsername(), productId);
            redirectAttributes.addFlashAttribute("wishlistMessage", "Product added to your Wishlist!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("wishlistError", "Could not add item to wishlist: " + e.getMessage());
        }

        // FIX: Redirect to /wishlist instead of /products/{productId}
        return "redirect:/wishlist";
    }

    /**
     * API Endpoint: Removes a product from the wishlist.
     */
    @PostMapping("/wishlist/remove/{productId}")
    public String removeProductFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {

        User user = userService.findUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB."));

        try {
            wishlistService.removeFromWishlist(user.getId(), productId);
            redirectAttributes.addFlashAttribute("wishlistMessage", "Item removed from Wishlist.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("wishlistError", "Could not remove item from wishlist: " + e.getMessage());
        }

        return "redirect:/wishlist";
    }
}