package com.anvistudio.boutique.service;

import com.anvistudio.boutique.model.ContactMessage;
import com.anvistudio.boutique.repository.ContactRepository;
import org.springframework.stereotype.Service;

/**
 * Service layer for handling contact message business logic.
 * Primarily responsible for persisting messages received via the contact form.
 */
@Service
public class ContactService {

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    /**
     * Saves a new customer query to the database.
     * @param message The ContactMessage object submitted by the user.
     * @return The persisted ContactMessage object.
     */
    public ContactMessage saveMessage(ContactMessage message) {
        // In a real application, logic for spam filtering, validation,
        // and triggering an internal email notification would occur here.
        return contactRepository.save(message);
    }
}