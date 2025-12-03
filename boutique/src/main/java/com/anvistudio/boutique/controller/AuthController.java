package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.service.UserService;
import com.anvistudio.boutique.dto.RegistrationDTO;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.model.VerificationToken.TokenType; // NEW IMPORT
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // NEW IMPORT

import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // =========================================================================
    // 1. LOGIN & REGISTRATION FLOW
    // =========================================================================

    /**
     * Handles the GET request for the login page.
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }

    /**
     * Handles the GET request for the customer registration page.
     */
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new RegistrationDTO());
        return "register";
    }

    /**
     * Handles the POST request for customer registration.
     */
    @PostMapping("/register")
    public String registerCustomer(
            @Valid @ModelAttribute("registrationDTO") RegistrationDTO registrationDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            // Save user and trigger OTP email
            User savedUser = userService.registerCustomer(registrationDTO);

            // CRITICAL FIX: Clear the security context to prevent auto-login
            SecurityContextHolder.clearContext();

            // Redirect to the OTP confirmation page, passing the username (email)
            redirectAttributes.addFlashAttribute("registrationEmail", savedUser.getUsername());

            return "redirect:/confirm-otp?email=" + savedUser.getUsername();
        } catch (IllegalStateException e) {
            // Catches username/phone taken or password mismatch not caught by DTO
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "An unexpected error occurred during registration: " + e.getMessage());
            return "register";
        }
    }

    // =========================================================================
    // 2. REGISTRATION OTP CONFIRMATION
    // =========================================================================

    /**
     * GET handler for the OTP Confirmation page.
     */
    @GetMapping("/confirm-otp")
    public String showOtpConfirmation(@RequestParam("email") String email, Model model, RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = userService.findUserByUsername(email);

        if (userOptional.isEmpty() || userOptional.get().getEmailVerified()) {
            // User doesn't exist or is already verified.
            redirectAttributes.addFlashAttribute("errorMessage", "Account is already verified or does not exist. Please try logging in.");
            return "redirect:/login";
        }

        model.addAttribute("registrationEmail", email);
        return "confirm_otp";
    }

    /**
     * POST handler to process the submitted OTP (for REGISTRATION).
     */
    @PostMapping("/confirm-otp")
    public String confirmOtp(@RequestParam("email") String email,
                             @RequestParam("otp") String otp,
                             RedirectAttributes redirectAttributes) {

        String result = userService.confirmUserAccountWithOtp(otp, email);

        if (result.startsWith("Verification successful")) {
            redirectAttributes.addFlashAttribute("successMessage", result + " You can now log in.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", result);
            // On failure, return to the confirmation page to try again
            return "redirect:/confirm-otp?email=" + email;
        }
    }

    // =========================================================================
    // 3. FORGOT PASSWORD FLOW
    // =========================================================================

    /**
     * STEP 1 (GET): Displays the form to request a password reset OTP.
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot_password";
    }

    /**
     * STEP 1 (POST): Processes the email/phone request, sends OTP, and redirects to OTP verification form.
     */
    @PostMapping("/forgot-password")
    public String processForgotPasswordRequest(
            @RequestParam("identifier") String identifier,
            RedirectAttributes redirectAttributes) {

        try {
            // This method finds the user and sends the PASSWORD_RESET OTP
            User user = userService.findAndCreateResetOtp(identifier);

            redirectAttributes.addFlashAttribute("successMessage", "An OTP has been sent to your registered email (" + user.getUsername() + ").");

            // Redirect to the OTP validation step, carrying the user's email
            return "redirect:/reset-otp?email=" + user.getUsername();

        } catch (UsernameNotFoundException e) {
            // Keep the error message generic to avoid revealing account existence
            redirectAttributes.addFlashAttribute("errorMessage", "Could not find an account matching that identifier.");
            return "redirect:/forgot-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error sending OTP. Please try again.");
            return "redirect:/forgot-password";
        }
    }

    /**
     * STEP 2 (GET): Displays the OTP validation form for password reset.
     */
    @GetMapping("/reset-otp")
    public String showResetOtpForm(@RequestParam("email") String email, Model model) {

        // Use the email passed in the URL to pre-populate the form/context
        model.addAttribute("userEmail", email);

        // Check if an active reset token exists for this user (optional validation)
        if (userService.findActiveToken(email, TokenType.PASSWORD_RESET).isEmpty()) {
            model.addAttribute("errorMessage", "No active password reset request found. Please start the process again.");
            return "forgot_password";
        }

        return "reset_otp";
    }

    /**
     * STEP 2 (POST): Validates the submitted OTP. If valid, proceeds to the final reset form.
     */
    @PostMapping("/reset-otp")
    public String validateResetOtp(
            @RequestParam("email") String email,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = userService.verifyOtp(otp, email, TokenType.PASSWORD_RESET);

        if (userOptional.isPresent()) {
            // OTP is valid! Redirect to the final password reset form
            return "redirect:/reset-password?email=" + email;
        } else {
            // OTP is invalid or expired
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid or expired OTP. Please try again.");
            return "redirect:/reset-otp?email=" + email;
        }
    }

    /**
     * STEP 3 (GET): Displays the final password reset form.
     * Accessible only if the user successfully completed the OTP step (implicit state/security logic is simplified here).
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("email") String email, Model model) {
        // Ensure the email is passed to the form for processing
        model.addAttribute("userEmail", email);
        return "reset_password";
    }

    /**
     * STEP 3 (POST): Processes the new password submission.
     */
    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("email") String email,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Passwords do not match.");
            return "redirect:/reset-password?email=" + email;
        }

        // Perform final password update
        try {
            userService.resetPassword(email, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully! You can now log in.");
            return "redirect:/login";
        } catch (UsernameNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Account error during reset.");
            return "redirect:/login";
        }
    }
}