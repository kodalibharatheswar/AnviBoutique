package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Address;
import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.model.Order;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.service.*;
import com.anvistudio.boutique.dto.RegistrationDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.List;

/**
 * Controller for customer-specific pages, requiring ROLE_CUSTOMER access.
 */
@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final OrderService orderService;
    private final AddressService addressService;
    private final CouponService couponService;
    private final GiftCardService giftCardService;

    // CONSTRUCTOR UPDATED to inject all necessary services (ReviewService assumed to be injected elsewhere if needed)
    public CustomerController(ProductService productService, UserService userService,
                              CartService cartService, WishlistService wishlistService,
                              OrderService orderService, AddressService addressService,
                              CouponService couponService, GiftCardService giftCardService) {
        this.productService = productService;
        this.userService = userService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
        this.orderService = orderService;
        this.addressService = addressService;
        this.couponService = couponService;
        this.giftCardService = giftCardService;
    }

    /**
     * NEW: Static helper method for Thymeleaf to fetch Product Image URL by ID.
     */
    @ModelAttribute("productImageUrl")
    public String getProductImageUrl(@RequestParam(required = false) Long productId) {
        if (productId == null) return "https://placehold.co/80x80/f0f0f0/333?text=N%2FA";
        return productService.getProductImageUrl(productId);
    }

    // =========================================================================
    // 1. My Orders & Returns
    // =========================================================================

    @GetMapping("/orders")
    public String showMyOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        try {
            List<Order> orders = orderService.getOrdersByUsername(username);
            model.addAttribute("orders", orders);

            // Inject ProductService into model for direct use in the template
            model.addAttribute("productService", productService);

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load order history: " + e.getMessage());
        }
        return "customer_orders";
    }

    @PostMapping("/order/cancel/{orderId}")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " has been successfully cancelled. A refund process has been initiated.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling order: " + e.getMessage());
        }
        return "redirect:/customer/orders";
    }

    /**
     * NEW ENDPOINT: Handles return request submission.
     */
    @PostMapping("/order/return/{orderId}")
    public String returnOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            orderService.returnOrder(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Return request for Order #" + orderId + " submitted successfully. Awaiting confirmation!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting return request: " + e.getMessage());
        }
        return "redirect:/customer/orders";
    }

    // =========================================================================
    // 2. Customer Profile Management (Retaining Existing Methods)
    // =========================================================================

    @GetMapping("/dashboard")
    public String customerDashboard() {
        return "redirect:/";
    }

    @GetMapping("/profile")
    public String showCustomerProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();

        Optional<Customer> customerOptional = userService.getCustomerDetailsByUsername(username);

        if (customerOptional.isEmpty()) {
            return "redirect:/";
        }

        Customer customer = customerOptional.get();

        RegistrationDTO profileDTO = userService.getProfileDTOFromCustomer(customer);
        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("currentEmail", username);
        model.addAttribute("customer", customer);

        return "customer_profile";
    }

    @PostMapping("/profile/update")
    public String updateCustomerProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute("profileDTO") RegistrationDTO profileDTO,
            RedirectAttributes redirectAttributes) {

        String currentUsername = userDetails.getUsername();

        try {
            userService.updateCustomerProfile(currentUsername, profileDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during profile update.");
        }

        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/change-password")
    public String changeCustomerPassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            userService.changePassword(userDetails.getUsername(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully! Please log in with your new password.");

            return "redirect:/logout";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "An unexpected error occurred during password change.");
        }

        return "redirect:/customer/profile";
    }

    @PostMapping("/profile/change-email/initiate")
    public String initiateEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newEmail") String newEmail,
            RedirectAttributes redirectAttributes) {

        try {
            userService.initiateEmailChange(userDetails.getUsername(), newEmail);

            redirectAttributes.addFlashAttribute("successMessage", "Verification code sent to " + newEmail + ".");
            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("emailChangeError", e.getMessage());
            return "redirect:/customer/profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("emailChangeError", "Error initiating email change. Please try again.");
            return "redirect:/customer/profile";
        }
    }

    @GetMapping("/profile/verify-new-email")
    public String showVerifyNewEmailForm(@RequestParam(value = "newEmail", required = false) String newEmail, Model model, RedirectAttributes redirectAttributes) {

        if (newEmail == null || newEmail.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("emailChangeError", "Error: Please provide the new email address to start verification.");
            return "redirect:/customer/profile";
        }

        if (userService.findActiveToken(newEmail, com.anvistudio.boutique.model.VerificationToken.TokenType.NEW_EMAIL_VERIFICATION).isEmpty()) {
            model.addAttribute("newEmail", newEmail);
            model.addAttribute("error", "The verification link or code has expired. Please return to your profile and re-initiate the email change.");
            return "verify_new_email";
        }

        model.addAttribute("newEmail", newEmail);
        return "verify_new_email";
    }

    @PostMapping("/profile/change-email/finalize")
    public String finalizeEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newEmail") String newEmail,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        try {
            String currentUsername = userDetails.getUsername();
            userService.finalizeEmailChange(currentUsername, newEmail, otp);

            redirectAttributes.addFlashAttribute("successMessage", "Your email address has been successfully updated to " + newEmail + ". Please log in with your new email.");
            return "redirect:/logout";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("emailChangeError", e.getMessage());
            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("emailChangeError", "Error finalizing email change.");
            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;
        }
    }

    @PostMapping("/profile/update-newsletter")
    public String updateNewsletterSubscription(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("optIn") boolean optIn,
            RedirectAttributes redirectAttributes) {

        try {
            userService.updateNewsletterOptIn(userDetails.getUsername(), optIn);
            String status = optIn ? "subscribed to" : "unsubscribed from";
            redirectAttributes.addFlashAttribute("successMessage", "You have successfully " + status + " the newsletter.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating newsletter status: " + e.getMessage());
        }

        return "redirect:/";
    }


    @GetMapping("/addresses")
    public String showSavedAddresses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        try {
            List<Address> addresses = addressService.getAddressesByUsername(username);
            model.addAttribute("addresses", addresses);
            model.addAttribute("newAddress", new Address());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load saved addresses.");
        }
        return "customer_addresses";
    }

    @PostMapping("/addresses/add")
    public String addOrUpdateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute("address") Address address,
            RedirectAttributes redirectAttributes) {
        try {
            addressService.saveAddress(userDetails.getUsername(), address);
            redirectAttributes.addFlashAttribute("successMessage", "Address saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving address: " + e.getMessage());
        }
        return "redirect:/customer/addresses";
    }

    @PostMapping("/addresses/delete/{id}")
    public String deleteAddress(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            addressService.deleteAddress(id);
            redirectAttributes.addFlashAttribute("successMessage", "Address deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting address.");
        }
        return "redirect:/customer/addresses";
    }


    @GetMapping("/coupons")
    public String showCouponsAndOffers(Model model) {
        try {
            model.addAttribute("coupons", couponService.getAllActiveCoupons());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load coupons and offers.");
        }
        return "customer_coupons";
    }

    @GetMapping("/gift-cards")
    public String showGiftCards(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        try {
            model.addAttribute("giftCards", giftCardService.getGiftCardsByUsername(username));
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load gift card information.");
        }
        return "customer_gift_cards";
    }
}