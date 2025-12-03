package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.Customer;
import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.model.VerificationToken;
import com.anvistudio.boutique.model.VerificationToken.TokenType;
import com.anvistudio.boutique.repository.CustomerRepository;
import com.anvistudio.boutique.repository.UserRepository;
import com.anvistudio.boutique.repository.VerificationTokenRepository;
import com.anvistudio.boutique.dto.RegistrationDTO;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.security.core.userdetails.DisabledException;

import java.util.Collections;
import java.util.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");

    public final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, CustomerRepository customerRepository,
                       VerificationTokenRepository tokenRepository, EmailService emailService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * MODIFIED: Added phone number lookup logic for login.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        Optional<User> userOptional = findUserByIdentifier(identifier);

        // --- 1. Handle Admin User (must still use "admin" as username/email) ---
        if ("admin".equalsIgnoreCase(identifier) && userOptional.isEmpty()) {
            return org.springframework.security.core.userdetails.User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("password123"))
                    .roles("ADMIN")
                    .disabled(false).accountExpired(false).credentialsExpired(false).accountLocked(false).build();
        }

        // --- 2. Handle Database User ---
        User user = userOptional
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        // CRITICAL: User must be verified to log in (except for Admin)
        boolean isEnabled = user.getEmailVerified() || "ADMIN".equals(user.getRole());

        // Throw DisabledException if not enabled (will be caught by SecurityConfig failure handler)
        if (!isEnabled) {
            throw new DisabledException("Account is not yet verified. Please confirm your email address.");
        }


        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isEnabled,
                true, true, true,
                Collections.singleton(authority)
        );
    }

    // --- NEW/MODIFIED: User Lookup Methods ---

    /**
     * Finds the User entity (not UserDetails) by username (email).
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * NEW: Finds a User by either username (email) or phone number.
     * Used by loadUserByUsername and Forgot Password feature.
     */
    public Optional<User> findUserByIdentifier(String identifier) {
        // 1. Try to treat identifier as email (Username)
        Optional<User> userByEmail = userRepository.findByUsername(identifier);
        if (userByEmail.isPresent()) {
            return userByEmail;
        }

        // 2. If not found by email, check if it matches phone pattern
        if (PHONE_PATTERN.matcher(identifier).matches()) {
            // Try to find customer by phone number, then get the associated User
            return customerRepository.findByPhoneNumber(identifier)
                    .map(Customer::getUser);
        }

        // 3. Not found by email or valid phone pattern
        return Optional.empty();
    }
    // ------------------------------------------

    @Transactional
    public User updateAdminCredentials(String currentUsername, String newUsername, String newPassword) {
        User adminUser = userRepository.findByUsername(currentUsername)
                .orElseGet(() -> {
                    User tempAdmin = new User();
                    tempAdmin.setUsername(currentUsername);
                    tempAdmin.setRole("ADMIN");
                    return tempAdmin;
                });

        if (!currentUsername.equals(newUsername) && userRepository.findByUsername(newUsername).isPresent()) {
            throw new IllegalStateException("The new username is already taken.");
        }

        adminUser.setUsername(newUsername);
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        adminUser.setEmailVerified(true);
        return userRepository.save(adminUser);
    }


    // --- Customer Registration Logic ---
    @Transactional
    public User registerCustomer(RegistrationDTO registrationDTO) {

        if (userRepository.findByUsername(registrationDTO.getUsername()).isPresent()) {
            throw new IllegalStateException("Username is already taken.");
        }

        // Ensure phone number isn't already used
        if (customerRepository.findByPhoneNumber(registrationDTO.getPhoneNumber()).isPresent()) {
            throw new IllegalStateException("Phone number is already registered.");
        }

        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            throw new IllegalStateException("Passwords do not match.");
        }

        User newUser = new User();
        newUser.setUsername(registrationDTO.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
        newUser.setRole("CUSTOMER");
        newUser.setEmailVerified(false);
        User savedUser = userRepository.save(newUser);

        Customer newCustomer = new Customer();
        newCustomer.setFirstName(registrationDTO.getFirstName());
        newCustomer.setLastName(registrationDTO.getLastName());
        newCustomer.setUser(savedUser);
        newCustomer.setPhoneNumber(registrationDTO.getPhoneNumber());
        newCustomer.setPreferredSize(registrationDTO.getPreferredSize());
        newCustomer.setGender(registrationDTO.getGender());
        newCustomer.setTermsAccepted(registrationDTO.getTermsAccepted());
        newCustomer.setNewsletterOptIn(registrationDTO.getNewsletterOptIn());

        if (registrationDTO.getDateOfBirth() != null && !registrationDTO.getDateOfBirth().isEmpty()) {
            try {
                Date dob = new SimpleDateFormat("yyyy-MM-dd").parse(registrationDTO.getDateOfBirth());
                newCustomer.setDateOfBirth(dob);
            } catch (Exception e) {
                System.err.println("Failed to parse DOB: " + e.getMessage());
            }
        }

        customerRepository.save(newCustomer);

        // Create OTP and send email for REGISTRATION
        createOtpAndSendEmail(savedUser);

        return savedUser;
    }

    /**
     * Creates a new OTP for the user and triggers the email sending (For REGISTRATION).
     */
    @Transactional
    public void createOtpAndSendEmail(User user) {
        // Ensure only one active token per user by deleting existing ones
        tokenRepository.deleteByUserId(user.getId());

        VerificationToken otpToken = new VerificationToken(user, TokenType.REGISTRATION); // Use REGISTRATION type
        tokenRepository.save(otpToken);

        emailService.sendOtpEmail(user, otpToken);
    }

    // --- NEW: Password Reset Logic ---

    /**
     * Finds the user by email/phone and creates a new PASSWORD_RESET OTP.
     * Called by AuthController's POST /forgot-password.
     * @param identifier Email or phone number.
     * @return The User entity whose reset OTP was sent.
     * @throws UsernameNotFoundException if the user doesn't exist.
     */
    @Transactional
    public User findAndCreateResetOtp(String identifier) throws UsernameNotFoundException {
        // Use the general identifier lookup
        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        // Create OTP and send email for PASSWORD_RESET
        createResetOtpAndSendEmail(user);

        return user;
    }


    /**
     * Initiates the password reset process by creating and sending a PASSWORD_RESET OTP.
     * @param user The user requesting the reset.
     */
    @Transactional
    public void createResetOtpAndSendEmail(User user) {
        // Clean up any existing tokens for this user first
        tokenRepository.deleteByUserId(user.getId());

        VerificationToken resetToken = new VerificationToken(user, TokenType.PASSWORD_RESET); // Use RESET type
        tokenRepository.save(resetToken);

        emailService.sendOtpEmail(user, resetToken);
    }

    /**
     * NEW: Finds the active token of a specific type for a user.
     * Called by AuthController's GET /reset-otp validation.
     */
    public Optional<VerificationToken> findActiveToken(String email, TokenType tokenType) {
        return userRepository.findByUsername(email)
                .flatMap(user -> tokenRepository.findByUserId(user.getId()))
                .filter(token -> token.getTokenType() == tokenType && !token.isExpired());
    }

    /**
     * Attempts to verify an OTP against a specific user and token type.
     * @param otp The code submitted by the user.
     * @param username The user's email identifier.
     * @param tokenType The expected type (REGISTRATION or PASSWORD_RESET).
     * @return The User object if valid, or Optional.empty().
     */
    @Transactional
    public Optional<User> verifyOtp(String otp, String username, TokenType tokenType) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            return Optional.empty(); // User not found
        }

        User user = userOptional.get();

        // Find the specific token for this user
        // Ensure we fetch the token that matches the required type
        Optional<VerificationToken> tokenOptional = tokenRepository.findByUserId(user.getId())
                .filter(token -> token.getTokenType() == tokenType);

        if (tokenOptional.isEmpty()) {
            // No active token of the correct type found
            return Optional.empty();
        }

        VerificationToken otpToken = tokenOptional.get();

        // Check 1: Expiry
        if (otpToken.isExpired()) {
            tokenRepository.delete(otpToken);
            return Optional.empty();
        }

        // Check 2: OTP match
        if (!otpToken.getToken().equals(otp)) {
            return Optional.empty();
        }

        // Valid OTP found. Delete the token immediately after success.
        tokenRepository.delete(otpToken);

        return Optional.of(user);
    }

    /**
     * Confirms a user account after successful REGISTRATION OTP validation.
     */
    @Transactional
    public String confirmUserAccountWithOtp(String otp, String username) {
        Optional<User> verifiedUser = verifyOtp(otp, username, TokenType.REGISTRATION);

        if (verifiedUser.isPresent()) {
            User user = verifiedUser.get();
            // Mark user as verified
            user.setEmailVerified(true);
            userRepository.save(user);
            return "Verification successful: Your account is now active!";
        } else {
            // Provide specific error feedback based on why the token failed (though complex to differentiate here)
            return "Invalid or expired OTP. Please check the code, request a new one, and try again.";
        }
    }

    /**
     * Final step in password reset: update the password hash.
     */
    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for reset."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Optional<Customer> getCustomerDetailsByUsername(String username) {
        return userRepository.findByUsername(username)
                .flatMap(user -> customerRepository.findByUserId(user.getId()));
    }
}