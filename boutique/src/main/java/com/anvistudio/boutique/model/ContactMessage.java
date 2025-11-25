package com.anvistudio.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * Entity to store messages submitted via the Contact Us form.
 * Mapped to the 'contact_messages' table in the database.
 */
@Entity
@Table(name = "contact_messages")
@Data // Lombok annotation for getters, setters, toString, etc.
@NoArgsConstructor // Lombok for no-argument constructor
@AllArgsConstructor // Lombok for constructor with all arguments
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    // NEW FIELD ADDED
    @Column(nullable = true) // Making this nullable just in case the user doesn't require it,
    private String phoneNumber;

    @Column(nullable = false, length = 1000) // Allows for longer message text
    private String message;

    /**
     * Stores the date and time the message was submitted.
     * Set once on creation and cannot be updated.
     */
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateSubmitted = new Date();
}