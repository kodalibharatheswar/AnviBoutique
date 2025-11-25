package com.anvistudio.boutique.controller;

import com.anvistudio.boutique.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles the GET request for the login page.
     */
    @GetMapping("/login")
    public String showLoginForm() {
        return "login"; // Maps to src/main/resources/templates/login.html
    }

    /**
     * Handles the GET request for the customer registration page.
     */
    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register"; // Maps to src/main/resources/templates/register.html
    }

    /**
     * Handles the GET request for the successful registration page.
     */
    @GetMapping("/register/success")
    public String showRegistrationSuccess() {
        return "registration_success"; // Maps to src/main/resources/templates/registration_success.html
    }


    /**
     * Handles the POST request for customer registration.
     */
    @PostMapping("/register")
    public String registerCustomer(@RequestParam String username,
                                   @RequestParam String password,
                                   @RequestParam String firstName,
                                   @RequestParam String lastName) {
        try {
            userService.registerCustomer(username, password, firstName, lastName);
            // Redirect to the new success page instead of immediately to login
            return "redirect:/register/success";
        } catch (IllegalStateException e) {
            return "redirect:/register?error=username_taken";
        }
    }
}