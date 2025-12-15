package com.anvistudio.boutique.config;

import com.anvistudio.boutique.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler; // NEW IMPORT
//import org.springframework.security.core.userdetails.DisabledException; // NEW IMPORT
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the authentication provider.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * NEW: Custom Failure Handler to catch DisabledException (unverified user)
     */
    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String redirectUrl = "/login?error";

            // If the user is disabled (i.e., email not verified)
            if (exception instanceof DisabledException) {
                // We redirect them back to the OTP page, passing their username (email)
                redirectUrl = "/confirm-otp?email=" + request.getParameter("username") + "&error=unverified";
            } else if (exception.getMessage().equals("Bad credentials")) {
                redirectUrl = "/login?error=bad_credentials";
            }

            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/wishlist", "/wishlist/**").authenticated()
                        .requestMatchers("/cart", "/cart/**").authenticated()
                        .requestMatchers(
                                "/", "/login", "/register", "/about", "/contact",
                                "/products", "/products/**", "/wishlist-unauth", "/cart-unauth",
                                "/custom-request",
                                "/confirm-otp", // MUST BE PUBLIC
                                // NEW: Password Reset Endpoints MUST be permitted
                                "/forgot-password",     // <--- ADDED
                                "/reset-otp",           // <--- ADDED
                                "/reset-password",      // <--- ADDED
                                // NEW: Newsletter Subscription Endpoint MUST be permitted
                                "/newsletter/subscribe", // <--- ADDED
                                // NEW: Policy Endpoints MUST be permitted
                                "/policy_return", // <--- ADDED
                                "/policy_privacy", // <--- ADDED
                                "/policy_terms", // <--- ADDED
                                "/policy_shipping", // <--- ADDED
                                "/customer/profile/verify-new-email",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        // Use the custom failure handler
                        .failureHandler(authenticationFailureHandler()) // NEW LINE
                        .successHandler((request, response, authentication) -> {
                            if (authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                response.sendRedirect("/admin/dashboard");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                )
                .logout(logout -> logout
                        .permitAll()
                        .logoutSuccessUrl("/")
                );

        return http.build();
    }
}