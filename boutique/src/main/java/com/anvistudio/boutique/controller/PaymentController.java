package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.CartItem;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.service.CartService; // NEW IMPORT
import com.anvistudio.boutique.service.OrderService; // NEW IMPORT
import com.anvistudio.boutique.service.StripeService;
import com.anvistudio.boutique.service.UserService; // NEW IMPORT

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional; // Required for transactional safety

import java.util.List;

/**
 * Handles the payment checkout flow integration with Stripe.
 */
@Controller
public class PaymentController {

    private final StripeService stripeService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    public PaymentController(StripeService stripeService, CartService cartService, OrderService orderService, UserService userService) {
        this.stripeService = stripeService;
        this.cartService = cartService;
        this.orderService = orderService;
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
     * Endpoint hit when the user clicks 'Proceed to Checkout' on the cart page.
     * This creates the Stripe Session and redirects the user to Stripe's hosted payment page.
     */
    @GetMapping("/checkout")
    public String initiateCheckout(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {

        if (userDetails == null || "anonymousUser".equals(userDetails.getUsername())) {
            return "redirect:/login"; // Force login before checkout
        }

        try {
            Session session = stripeService.createCheckoutSession(userDetails.getUsername());
            // Redirect the user to the Stripe-hosted checkout page
            return "redirect:" + session.getUrl();

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("cartError", "Error: " + e.getMessage());
            return "redirect:/cart";
        } catch (StripeException e) {
            System.err.println("Stripe API Error during session creation: " + e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", "Payment processing failed. Please try again. Code: " + e.getCode());
            return "redirect:/cart";
        } catch (Exception e) {
            System.err.println("Internal Server Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", "An unexpected error occurred. Please try again.");
            return "redirect:/cart";
        }
    }

    /**
     * Endpoint that Stripe redirects to upon successful payment.
     * CRITICAL: Implements the Order Fulfillment logic here.
     */
    @GetMapping("/payment/success")
    @Transactional // Ensure order creation and cart clearing are atomic
    public String paymentSuccess(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("session_id") String sessionId,
                                 RedirectAttributes redirectAttributes) {

        // 1. Authenticate and retrieve user
        User user;
        try {
            user = getAuthenticatedUser(userDetails);
        } catch (Exception e) {
            // Should not happen as payment requires authentication, but handle fallback
            redirectAttributes.addFlashAttribute("errorMessage", "Authentication required to finalize order.");
            return "redirect:/login";
        }

        Long userId = user.getId();

        try {
            // FUTURE: In a production app, you MUST retrieve the Stripe Session object
            // and verify its status and amount against the Cart total to prevent fraud.

            // 2. Retrieve current cart items before clearing
            List<CartItem> cartItems = cartService.getCartItems(userId);

            if (cartItems.isEmpty()) {
                // Handle case where user already fulfilled the order (e.g., refreshing page)
                redirectAttributes.addFlashAttribute("successMessage", "Your order was already confirmed! See your order history.");
                return "redirect:/customer/orders";
            }

            // 3. Fulfill the Order (Create permanent Order entity)
            orderService.createOrderFromCart(userId, cartItems);

            // 4. Clear the user's cart
            cartService.clearCart(userId);

            // 5. Success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment successful! Your order has been placed. Order tracking details will be sent to " + user.getUsername() + ".");

            // Redirect to the Orders page
            return "redirect:/customer/orders";

        } catch (IllegalStateException e) {
            // Error handling (e.g., cart was empty)
            redirectAttributes.addFlashAttribute("errorMessage", "Order fulfillment failed: " + e.getMessage());
            return "redirect:/cart";
        } catch (Exception e) {
            System.err.println("Order fulfillment critical error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "A critical error occurred while finalizing your order. Please contact support with Stripe Session ID: " + sessionId);
            return "redirect:/cart";
        }
    }

    /**
     * Endpoint that Stripe redirects to if the user cancels or the payment fails.
     */
    @GetMapping("/payment/cancel")
    public String paymentCancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("cartError", "Payment cancelled or failed. You can try again from your cart.");
        return "redirect:/cart";
    }
}


/*
package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

*/
/**
 * Handles the payment checkout flow integration with Stripe.
 *//*

@Controller
public class PaymentController {

    private final StripeService stripeService;

    public PaymentController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    */
/**
     * Endpoint hit when the user clicks 'Proceed to Checkout' on the cart page.
     * This creates the Stripe Session and redirects the user to Stripe's hosted payment page.
     *//*

    @GetMapping("/checkout")
    public String initiateCheckout(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {

        if (userDetails == null || "anonymousUser".equals(userDetails.getUsername())) {
            return "redirect:/login"; // Force login before checkout
        }

        try {
            Session session = stripeService.createCheckoutSession(userDetails.getUsername());
            // Redirect the user to the Stripe-hosted checkout page
            return "redirect:" + session.getUrl();

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("cartError", "Error: " + e.getMessage());
            return "redirect:/cart";
        } catch (StripeException e) {
            System.err.println("Stripe API Error during session creation: " + e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", "Payment processing failed. Please try again. Code: " + e.getCode());
            return "redirect:/cart";
        } catch (Exception e) {
            System.err.println("Internal Server Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", "An unexpected error occurred. Please try again.");
            return "redirect:/cart";
        }
    }

    */
/**
     * Endpoint that Stripe redirects to upon successful payment.
     * In a production environment, this is where you would fulfill the order (create Order entity, empty cart).
     *//*

    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam("session_id") String sessionId, Model model) {
        // NOTE: In a production app, you MUST verify the payment success via Stripe Webhooks
        // to prevent users from simply navigating to this URL.

        model.addAttribute("sessionId", sessionId);

        // TODO: Order Fulfillment logic should go here:
        // 1. Mark cart contents as a confirmed Order entity
        // 2. Clear the user's cart
        // 3. Send order confirmation email

        return "payment_success";
    }

    */
/**
     * Endpoint that Stripe redirects to if the user cancels or the payment fails.
     *//*

    @GetMapping("/payment/cancel")
    public String paymentCancel(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("cartError", "Payment cancelled or failed. You can try again from your cart.");
        return "redirect:/cart";
    }
}*/
