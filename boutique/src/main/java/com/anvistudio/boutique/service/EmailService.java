package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.User;
import com.anvistudio.boutique.model.VerificationToken;
import com.anvistudio.boutique.model.VerificationToken.TokenType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending live emails using the configured SMTP server.
 */
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * Sends the OTP to the user's email address.
     * MODIFIED: Content changes based on TokenType.
     * @param user The user who needs the OTP.
     * @param token The token object containing the 6-digit OTP.
     */
    public void sendOtpEmail(User user, VerificationToken token) {

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        String subject;
        String action;

        // Customize subject and action based on the purpose of the OTP
        if (token.getTokenType() == TokenType.PASSWORD_RESET) {
            subject = "Anvi Studio: Password Reset Code (OTP)";
            action = "reset your password";
        } else { // REGISTRATION
            subject = "Anvi Studio: Your One-Time Password (OTP) for Registration";
            action = "activate your account";
        }

        mailMessage.setFrom("Anvi Studio Support <kodalibharatheswar7@gmail.com>"); // Using user's email as sender
        mailMessage.setTo(user.getUsername());
        mailMessage.setSubject(subject);

        String emailContent = String.format(
                "Dear %s,\n\n" +
                        "Your One-Time Password (OTP) to %s is:\n\n" +
                        "--- %s ---\n\n" +
                        "This OTP expires in %d minutes.\n\n" +
                        "If you did not request this, please ignore this email.",
                user.getUsername(), action, token.getToken(), 5);

        mailMessage.setText(emailContent);

        try {
            javaMailSender.send(mailMessage);
            System.out.println("SMTP: Successfully sent OTP email for " + token.getTokenType() + " to " + user.getUsername());
        } catch (Exception e) {
            System.err.println("SMTP ERROR: Failed to send OTP email for " + token.getTokenType());
            e.printStackTrace();
        }
    }
}