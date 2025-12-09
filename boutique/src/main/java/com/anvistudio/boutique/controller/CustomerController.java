//package com.anvistudio.boutique.controller;
//
//import com.anvistudio.boutique.model.Customer;
//import com.anvistudio.boutique.service.CartService;
//import com.anvistudio.boutique.service.WishlistService;
//import com.anvistudio.boutique.service.ProductService;
//import com.anvistudio.boutique.service.UserService;
//import com.anvistudio.boutique.dto.RegistrationDTO;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.context.SecurityContextHolder; // NEW IMPORT
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.ModelAttribute;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.Optional;
//
///**
// * Controller for customer-specific pages, requiring ROLE_CUSTOMER access.
// */
//@Controller
//@RequestMapping("/customer")
//public class CustomerController {
//
//    private final ProductService productService;
//    private final UserService userService;
//    private final CartService cartService;
//    private final WishlistService wishlistService;
//
//    // CONSTRUCTOR UPDATED to inject all necessary services
//    public CustomerController(ProductService productService, UserService userService,
//                              CartService cartService, WishlistService wishlistService) {
//        this.productService = productService;
//        this.userService = userService;
//        this.cartService = cartService;
//        this.wishlistService = wishlistService;
//    }
//
//    /**
//     * Redirects /customer/dashboard to the root / (home page) as the main view.
//     */
//    @GetMapping("/dashboard")
//    public String customerDashboard() {
//        return "redirect:/";
//    }
//
//    // =========================================================================
//    // 1. Customer Profile Management (No Change)
//    // =========================================================================
//
//    /**
//     * Displays the customer profile form.
//     */
//    @GetMapping("/profile")
//    public String showCustomerProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
//        String username = userDetails.getUsername();
//
//        Optional<Customer> customerOptional = userService.getCustomerDetailsByUsername(username);
//
//        if (customerOptional.isEmpty()) {
//            // Should not happen for authenticated users, but redirect if data is missing
//            return "redirect:/";
//        }
//
//        Customer customer = customerOptional.get();
//
//        // Use a DTO to pre-populate the form for editing
//        RegistrationDTO profileDTO = userService.getProfileDTOFromCustomer(customer);
//        model.addAttribute("profileDTO", profileDTO);
//        model.addAttribute("currentEmail", username);
//
//        // Pass customer object to the navbar fragment
//        model.addAttribute("customer", customer);
//
//        return "customer_profile"; // Maps to the new template
//    }
//
//
//    /**
//     * Handles the update of customer profile details (excluding password/email change flow).
//     */
//    @PostMapping("/profile/update")
//    public String updateCustomerProfile(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @ModelAttribute("profileDTO") RegistrationDTO profileDTO,
//            RedirectAttributes redirectAttributes) {
//
//        String currentUsername = userDetails.getUsername();
//
//        try {
//            userService.updateCustomerProfile(currentUsername, profileDTO);
//            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
//        } catch (IllegalStateException e) {
//            // Catches validation errors like phone number already taken
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during profile update.");
//        }
//
//        return "redirect:/customer/profile";
//    }
//
//    /**
//     * Handles the password change request from the profile page.
//     */
//    @PostMapping("/profile/change-password")
//    public String changeCustomerPassword(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam("currentPassword") String currentPassword,
//            @RequestParam("newPassword") String newPassword,
//            @RequestParam("confirmPassword") String confirmPassword,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            userService.changePassword(userDetails.getUsername(), currentPassword, newPassword, confirmPassword);
//            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully! Please log in with your new password.");
//
//            // CRITICAL: Log user out after password change for security
//            return "redirect:/logout";
//
//        } catch (IllegalStateException e) {
//            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("passwordError", "An unexpected error occurred during password change.");
//        }
//
//        return "redirect:/customer/profile";
//    }
//
//    // =========================================================================
//    // 2. Secure Email Change Flow
//    // =========================================================================
//
//    /**
//     * STEP 1 (POST): Initiates the email change process by sending an OTP to the NEW email.
//     */
//    @PostMapping("/profile/change-email/initiate")
//    public String initiateEmailChange(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam("newEmail") String newEmail,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            userService.initiateEmailChange(userDetails.getUsername(), newEmail);
//
//            redirectAttributes.addFlashAttribute("successMessage", "Verification code sent to " + newEmail + ".");
//            // Redirect to the new OTP verification page
//            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;
//
//        } catch (IllegalStateException e) {
//            redirectAttributes.addFlashAttribute("emailChangeError", e.getMessage());
//            return "redirect:/customer/profile";
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("emailChangeError", "Error initiating email change. Please try again.");
//            return "redirect:/customer/profile";
//        }
//    }
//
//
//    /**
//     * STEP 2 (GET): Displays the OTP verification form for the new email.
//     */
//    @GetMapping("/profile/verify-new-email")
//    public String showVerifyNewEmailForm(@RequestParam("newEmail") String newEmail, Model model) {
//
//        // We check if a token exists using the proposed new email (which is the recipient).
//        if (userService.findActiveToken(newEmail, com.anvistudio.boutique.model.VerificationToken.TokenType.NEW_EMAIL_VERIFICATION).isEmpty()) {
//            // Only show the error if the active token check explicitly fails.
//            model.addAttribute("error", "No active verification request found. Please try initiating the change again.");
//        }
//
//        model.addAttribute("newEmail", newEmail);
//        return "verify_new_email";
//    }
//
//
//    /**
//     * STEP 3 (POST): Validates the OTP and commits the new email to the database.
//     * CRITICAL FIX APPLIED HERE.
//     */
//    @PostMapping("/profile/change-email/finalize")
//    public String finalizeEmailChange(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam("newEmail") String newEmail,
//            @RequestParam("otp") String otp,
//            RedirectAttributes redirectAttributes) {
//
//        try {
//            String currentUsername = userDetails.getUsername();
//            userService.finalizeEmailChange(currentUsername, newEmail, otp);
//
//            // 1. Success! Clear the security context immediately to log the user out.
//            SecurityContextHolder.clearContext();
//
//            // 2. Redirect directly to the login page with a success message.
//            redirectAttributes.addFlashAttribute("successMessage", "Your email address has been successfully updated to " + newEmail + ". Please log in with your new email.");
//
//            // FIX: Redirect directly to /login instead of relying on the /logout handler logic
//            return "redirect:/login";
//
//        } catch (IllegalStateException e) {
//            redirectAttributes.addFlashAttribute("emailChangeError", e.getMessage());
//            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("emailChangeError", "Error finalizing email change.");
//            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;
//        }
//    }
//}
//


/*
package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.service.CartService;
import com.anvistudio.boutique.service.WishlistService;
import com.anvistudio.boutique.service.ProductService;
import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.dto.RegistrationDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

*/
/**
 * Controller for customer-specific pages, requiring ROLE_CUSTOMER access.
 *//*

@Controller
@RequestMapping("/customer")
public class CustomerController {

    private final ProductService productService;
    private final UserService userService;
    private final CartService cartService;
    private final WishlistService wishlistService;

    // CONSTRUCTOR UPDATED to inject all necessary services
    public CustomerController(ProductService productService, UserService userService,
                              CartService cartService, WishlistService wishlistService) {
        this.productService = productService;
        this.userService = userService;
        this.cartService = cartService;
        this.wishlistService = wishlistService;
    }

    */
/**
     * Redirects /customer/dashboard to the root / (home page) as the main view.
     *//*

    @GetMapping("/dashboard")
    public String customerDashboard() {
        return "redirect:/";
    }

    // =========================================================================
    // 1. Customer Profile Management
    // =========================================================================

    */
/**
     * Displays the customer profile form.
     *//*

    @GetMapping("/profile")
    public String showCustomerProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();

        Optional<Customer> customerOptional = userService.getCustomerDetailsByUsername(username);

        if (customerOptional.isEmpty()) {
            // Should not happen for authenticated users, but redirect if data is missing
            return "redirect:/";
        }

        Customer customer = customerOptional.get();

        // Use a DTO to pre-populate the form for editing
        RegistrationDTO profileDTO = userService.getProfileDTOFromCustomer(customer);
        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("currentEmail", username);

        // Pass customer object to the navbar fragment
        model.addAttribute("customer", customer);

        return "customer_profile"; // Maps to the new template
    }


    */
/**
     * Handles the update of customer profile details (excluding password/email change flow).
     *//*

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
            // Catches validation errors like phone number already taken
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during profile update.");
        }

        return "redirect:/customer/profile";
    }

    */
/**
     * Handles the password change request from the profile page.
     * Uses the existing UserDetails (username) for authentication.
     *//*

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

            // CRITICAL: Log user out after password change for security
            return "redirect:/logout";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "An unexpected error occurred during password change.");
        }

        return "redirect:/customer/profile";
    }

    // =========================================================================
    // 2. NEW: Secure Email Change Flow (Initiation)
    // =========================================================================

    */
/**
     * STEP 1 (POST): Initiates the email change process by sending an OTP to the NEW email.
     *//*

    @PostMapping("/profile/change-email/initiate")
    public String initiateEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newEmail") String newEmail,
            RedirectAttributes redirectAttributes) {

        try {
            // Service method handles validation (is email taken?) and sends OTP
            userService.initiateEmailChange(userDetails.getUsername(), newEmail);

            redirectAttributes.addFlashAttribute("successMessage", "Verification code sent to " + newEmail + ".");
            // Redirect to the new OTP verification page
            return "redirect:/customer/profile/verify-new-email?newEmail=" + newEmail;

        } catch (IllegalStateException e) {
            // FIX: Use the specific flash attribute for email change errors
            redirectAttributes.addFlashAttribute("emailChangeError", e.getMessage());
            return "redirect:/customer/profile";
        } catch (Exception e) {
            // FIX: Use the specific flash attribute for email change errors
            redirectAttributes.addFlashAttribute("emailChangeError", "Error initiating email change. Please try again.");
            return "redirect:/customer/profile";
        }
    }



    */
/**
     * STEP 2 (GET): Displays the OTP verification form for the new email.
     * MODIFIED: Added check for empty newEmail parameter.
     *//*

    @GetMapping("/profile/verify-new-email")
    public String showVerifyNewEmailForm(@RequestParam(value = "newEmail", required = false) String newEmail, Model model, RedirectAttributes redirectAttributes) {

        // --- NEW CHECK: If the email parameter is missing or empty ---
        if (newEmail == null || newEmail.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("emailChangeError", "Error: Please provide the new email address to start verification.");
            return "redirect:/customer/profile";
        }
        // --- END NEW CHECK ---

        // Check if a token for this new email exists.
        if (userService.findActiveToken(newEmail, com.anvistudio.boutique.model.VerificationToken.TokenType.NEW_EMAIL_VERIFICATION).isEmpty()) {

            // If no token is found, we pass the error back as a model attribute
            // so the user sees the error on the verification page itself.
            model.addAttribute("newEmail", newEmail);
            model.addAttribute("error", "The verification link or code has expired. Please return to your profile and re-initiate the email change.");
//            model.addAttribute("error", "No active verification request found. Please try initiating the change again.");
            return "verify_new_email";
        }

        model.addAttribute("newEmail", newEmail);
        return "verify_new_email";
    }

    */
/**
     * STEP 2 (GET): Displays the OTP verification form for the new email.
     *//*

    */
/*@GetMapping("/profile/verify-new-email")
    public String showVerifyNewEmailForm(@RequestParam("newEmail") String newEmail, Model model) {

        // Check if a token for this new email exists. If not, redirect back to profile.
        // We use the newEmail as the identifier to check if there is a pending token associated with the underlying user ID.
        if (userService.findActiveToken(newEmail, com.anvistudio.boutique.model.VerificationToken.TokenType.NEW_EMAIL_VERIFICATION).isEmpty()) {
            // Note: If no token is found, we redirect to the profile page with an error flash message.
            // However, this page is designed to be public/unauthenticated, so we use a model attribute here.
            model.addAttribute("newEmail", newEmail); // Still pass it back for the user context
            model.addAttribute("error", "No active verification request found. Please try initiating the change again.");
            return "verify_new_email"; // Stay on the page but show the error
        }

        model.addAttribute("newEmail", newEmail);
        return "verify_new_email"; // Maps to the new template
    }*//*



    */
/**
     * STEP 3 (POST): Validates the OTP and commits the new email to the database.
     *//*

    @PostMapping("/profile/change-email/finalize")
    public String finalizeEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newEmail") String newEmail,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        try {
            String currentUsername = userDetails.getUsername();
            userService.finalizeEmailChange(currentUsername, newEmail, otp);

            // Success! The username (principal) has changed. Force logout.
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
}*/


package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.model.Address; // NEW
import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.model.Order; // NEW
import com.anvistudio.boutique.model.User; // NEW
import com.anvistudio.boutique.service.*; // NEW IMPORTS
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
    private final OrderService orderService; // NEW
    private final AddressService addressService; // NEW
    private final CouponService couponService; // NEW
    private final GiftCardService giftCardService; // NEW

    // CONSTRUCTOR UPDATED to inject all necessary services
    public CustomerController(ProductService productService, UserService userService,
                              CartService cartService, WishlistService wishlistService,
                              OrderService orderService, AddressService addressService,
                              CouponService couponService, GiftCardService giftCardService) { // NEW INJECTIONS
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
     * Redirects /customer/dashboard to the root / (home page) as the main view.
     */
    @GetMapping("/dashboard")
    public String customerDashboard() {
        return "redirect:/";
    }

    // =========================================================================
    // 1. Customer Profile Management (Existing)
    // =========================================================================

    /**
     * Displays the customer profile form.
     */
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

        return "customer_profile"; // Maps to the existing template
    }

    /**
     * Handles the update of customer profile details (excluding password/email change flow).
     */
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

    /**
     * Handles the password change request from the profile page.
     */
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

            // CRITICAL: Log user out after password change for security
            return "redirect:/logout";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("passwordError", "An unexpected error occurred during password change.");
        }

        return "redirect:/customer/profile";
    }

    /**
     * STEP 1 (POST): Initiates the email change process by sending an OTP to the NEW email.
     */
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


    /**
     * STEP 2 (GET): Displays the OTP verification form for the new email.
     */
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

    /**
     * STEP 3 (POST): Validates the OTP and commits the new email to the database.
     */
    @PostMapping("/profile/change-email/finalize")
    public String finalizeEmailChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("newEmail") String newEmail,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        try {
            String currentUsername = userDetails.getUsername();
            userService.finalizeEmailChange(currentUsername, newEmail, otp);

            // Success! The username (principal) has changed. Force logout.
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

    // =========================================================================
    // 2. NEW FEATURE: My Orders & Returns
    // =========================================================================

    @GetMapping("/orders")
    public String showMyOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        try {
            List<Order> orders = orderService.getOrdersByUsername(username);
            model.addAttribute("orders", orders);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load order history.");
        }
        return "customer_orders";
    }

    // =========================================================================
    // 3. NEW FEATURE: Saved Addresses
    // =========================================================================

    @GetMapping("/addresses")
    public String showSavedAddresses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        try {
            List<Address> addresses = addressService.getAddressesByUsername(username);
            model.addAttribute("addresses", addresses);
            model.addAttribute("newAddress", new Address()); // For the 'Add New' form
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
            // Note: If address.id is null, it saves a new one. If present, it updates.
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


    // =========================================================================
    // 4. NEW FEATURE: Coupons & Offers
    // =========================================================================

    @GetMapping("/coupons")
    public String showCouponsAndOffers(Model model) {
        try {
            model.addAttribute("coupons", couponService.getAllActiveCoupons());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not load coupons and offers.");
        }
        return "customer_coupons";
    }

    // =========================================================================
    // 5. NEW FEATURE: Gift Cards
    // =========================================================================

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