package com.creatorengine.contacts.service;

import com.creatorengine.contacts.entity.Contact;
import com.creatorengine.contacts.repository.ContactRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin layer over {@link ContactRepository}. Exists so the automation
 * engine doesn't need to know Firestore exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository repository;

    public List<Contact> list(String uid) {
        return repository.listForUser(uid);
    }

    /**
     * Record (or refresh) a contact from a webhook event.
     *
     * @param uid          owner CreatorEngine user
     * @param event        the source event
     * @param lastMessage  the message we just sent to them (or the trigger text
     *                     for SAVE_CONTACT, where nothing is sent)
     */
    public Contact recordFromEvent(String uid, WebhookEventDto event, String lastMessage) {
        Contact contact = Contact.builder()
                .instagramUserId(event.instagramUserId())
                .username(event.username())
                .source(event.type() != null ? event.type().name() : null)
                .lastMessage(lastMessage)
                .build();
        Contact saved = repository.upsertByInstagramUserId(uid, contact);
        log.debug("Contact upserted uid={} ig={} source={}",
                uid, saved.getInstagramUserId(), saved.getSource());
        return saved;
    }
}
