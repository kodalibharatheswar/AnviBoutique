package com.anvistudio.boutique.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Defines the authentication provider explicitly, relying on method injection
     * for UserDetailsService (UserService) and PasswordEncoder.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        // Uses the modern constructor style to link UserDetailsService
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        // Sets the PasswordEncoder used for hash comparison
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for simpler form handling during development
                .csrf(csrf -> csrf.disable())
                // Allows iframes (e.g., for H2 console)
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))

                // Define authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Admin Dashboard (Role-based access)
                        // Includes new edit endpoint for admin: /admin/product/edit/{id}
                        .requestMatchers("/admin/profile", "/admin/updateProfile").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/customer/**").hasRole("CUSTOMER")

                        // Protected E-commerce Features (Require Authentication)
                        .requestMatchers("/wishlist", "/wishlist/**").authenticated()
                        .requestMatchers("/cart", "/cart/**").authenticated()

                        // Public Access (Browsing must be permitted)
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/register/success",
                                "/about",
                                "/contact",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/products",            // Main catalog page
                                "/products/**",         // Catalog/Detail pages
                                "/wishlist-unauth",
                                "/cart-unauth"
                        ).permitAll()

                        // All other requests must be authenticated
                        .anyRequest().authenticated()
                )
                // Configure form-based login
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler((request, response, authentication) -> {
                            // --- CRITICAL REDIRECTION LOGIC ---
                            if (authentication.getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                                // Admin is correctly directed to their dedicated dashboard
                                response.sendRedirect("/admin/dashboard");
                            } else {
                                // Customer is directed to the main home page (which acts as their dashboard)
                                response.sendRedirect("/");
                            }
                        })
                )
                // Configure logout
                .logout(logout -> logout
                        .permitAll()
                        .logoutSuccessUrl("/")
                );

        return http.build();
    }
}

