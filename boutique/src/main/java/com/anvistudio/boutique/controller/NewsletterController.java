package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller to handle newsletter subscriptions from non-registered users (via footer).
 */
@Controller
public class NewsletterController {

    private final NotificationService notificationService;

    public NewsletterController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handles the subscription request from the footer form.
     */
    @PostMapping("/newsletter/subscribe")
    public String subscribe(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                throw new IllegalStateException("Please enter a valid email address.");
            }
            // This is the correct logic for non-registered users (saves to generic table)
            notificationService.subscribeEmail(email);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! You are now subscribed to our exclusive offers.");
        } catch (IllegalStateException e) {
            // FIX: If the user is registered, provide actionable steps.
            String message = e.getMessage();
            if (message.contains("already registered as a customer")) {
                // Change the message to offer a link/guidance
                redirectAttributes.addFlashAttribute("errorMessage", "You are already a registered customer. Please <a href='/login' class='alert-link'>log in</a> and manage your subscription preferences on your profile page.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", message);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Subscription failed. Please try again later.");
        }

        // Redirect back to the homepage
        return "redirect:/#footer";
    }
}