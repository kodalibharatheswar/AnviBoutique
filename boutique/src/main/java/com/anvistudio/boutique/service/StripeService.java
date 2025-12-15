package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.CartItem;
import com.anvistudio.boutique.model.User;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to interface with the Stripe API for payment processing.
 */
@Service
public class StripeService {

    private final CartService cartService;
    private final UserService userService;

    // Read Stripe currency from configuration
    @Value("${stripe.currency}")
    private String currency;

    // Read the public key for the frontend
    @Value("${stripe.api.publishableKey}")
    private String publishableKey;

    // NEW INJECTION: Base URL for successful redirects and image paths
    @Value("${app.base.url}")
    private String appBaseUrl;

    public StripeService(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    /**
     * Retrieves the Stripe Publishable Key for the frontend.
     */
    public String getPublishableKey() {
        return publishableKey;
    }

    /**
     * Helper to ensure the image URL is absolute for Stripe's API.
     */
    private String makeAbsoluteUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return "https://placehold.co/600x600"; // Fallback placeholder image (must be absolute)
        }
        // Check if it's already absolute (starts with http/https)
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }
        // If it's a relative path (e.g., /images/saree.jpg), prepend the base URL
        // Ensure only one slash separates the base and the relative path
        return appBaseUrl + (relativeUrl.startsWith("/") ? relativeUrl : "/" + relativeUrl);
    }

    /**
     * Creates a new Stripe Checkout Session for the user's current cart contents.
     * @param username The authenticated user's username (email).
     * @return The created Stripe Session object.
     * @throws StripeException If the Stripe API call fails.
     */
    public Session createCheckoutSession(String username) throws StripeException {
        User user = userService.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        List<CartItem> cartItems = cartService.getCartItems(user.getId());

        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart.");
        }

        // 1. Create Line Items from Cart
        List<SessionCreateParams.LineItem> lineItems = cartItems.stream()
                .map(item -> {
                    // Stripe uses the smallest currency unit (e.g., cents or paise)
                    Long unitAmount = item.getProduct().getDiscountedPrice()
                            .multiply(BigDecimal.valueOf(100))
                            .longValue();

                    // Ensure image URL is absolute
                    String absoluteImageUrl = makeAbsoluteUrl(item.getProduct().getImageUrl());

                    return SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantity())
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(unitAmount)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(item.getProduct().getName())
                                            .setDescription(item.getProduct().getDescription())
                                            .addImage(absoluteImageUrl) // Use absolute URL
                                            .build())
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());

        // 2. Build the Checkout Session Parameters
        SessionCreateParams params = SessionCreateParams.builder()
                .addAllLineItem(lineItems)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                // CRITICAL FIX: Base URL is confirmed to be used here
                .setSuccessUrl(appBaseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appBaseUrl + "/payment/cancel")
                .setClientReferenceId(user.getId().toString()) // Use user ID for order reconciliation
                .setCustomerEmail(user.getUsername()) // Pre-fill customer email
                .build();

        // 3. Create the Session
        return Session.create(params);
    }
}