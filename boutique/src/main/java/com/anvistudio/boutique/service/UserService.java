package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.repository.CustomerRepository;
import com.anvistudio.boutique.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    public final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Finds the user details. Handles both the initial hardcoded 'admin' and DB users.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Find user in the database
        Optional<User> userOptional = userRepository.findByUsername(username);

        // --- 1. Handle Admin User (either hardcoded or persisted) ---
        if ("admin".equals(username) && userOptional.isEmpty()) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("password123"))
                    .roles("ADMIN")
                    .build();
        }

        // --- 2. Handle Database User (Customer or persisted/changed Admin) ---
        User user = userOptional
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // This is the already encoded password from the DB
                Collections.singleton(authority)
        );
    }

    // --- NEW METHOD FOR SECURE USER LOOKUP ---
    /**
     * Retrieves the User entity (not UserDetails) by username.
     * Used by controllers to get the database ID.
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    // ------------------------------------------

    /**
     * NEW: Method to update the admin's username/email and password.
     */
    @Transactional
    public User updateAdminCredentials(String currentUsername, String newUsername, String newPassword) {
        // 1. Find the user by their currently authenticated username (will be 'admin' if first time)
        User adminUser = userRepository.findByUsername(currentUsername)
                .orElseGet(() -> {
                    // Create temporary object for persistence if new
                    User tempAdmin = new User();
                    tempAdmin.setUsername(currentUsername);
                    tempAdmin.setRole("ADMIN");
                    return tempAdmin;
                });

        // 2. Update the credentials
        if (!currentUsername.equals(newUsername) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalStateException("The new username is already taken.");
        }

        adminUser.setUsername(newUsername);
        adminUser.setPassword(passwordEncoder.encode(newPassword));

        // 3. Save to the database (making the change permanent)
        return userRepository.save(adminUser);
    }

    // --- Customer Registration Logic (Remains the same) ---
    @Transactional
    public User registerCustomer(String username, String rawPassword, String firstName, String lastName) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("Username is already taken.");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(rawPassword));
        newUser.setRole("CUSTOMER");
        User savedUser = userRepository.save(newUser);

        Customer newCustomer = new Customer();
        newCustomer.setFirstName(firstName);
        newCustomer.setLastName(lastName);
        newCustomer.setUser(savedUser);
        customerRepository.save(newCustomer);

        return savedUser;
    }

    public Optional<Customer> getCustomerDetailsByUsername(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> customerRepository.findByUserId(user.getId()));
    }
}