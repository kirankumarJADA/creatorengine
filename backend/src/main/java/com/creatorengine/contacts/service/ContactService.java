package com.creatorengine.contacts.service;

import com.creatorengine.contacts.entity.Contact;
import com.creatorengine.contacts.repository.ContactRepository;
import com.creatorengine.instagram.dto.WebhookEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final ContactRepository repository;

    public ContactService(ContactRepository repository) {
        this.repository = repository;
    }

    /**
     * List contacts for the active Instagram account.
     * Falls back to legacy path if igAccountId is null.
     */
    public List<Contact> list(String uid, String igAccountId) {
        if (igAccountId != null && !igAccountId.isBlank()) {
            return repository.listForAccount(uid, igAccountId);
        }
        return repository.listForUser(uid);
    }

    /**
     * @deprecated Use list(uid, igAccountId) instead.
     */
    @Deprecated
    public List<Contact> list(String uid) {
        return repository.listForUser(uid);
    }

    /**
     * Record a contact from a webhook event.
     * Uses the event's receivingAccountId to scope to the correct account.
     */
    public Contact recordFromEvent(String uid, WebhookEventDto event, String lastMessage) {
        if (event == null || event.instagramUserId() == null) return null;

        Contact contact = Contact.builder()
                .instagramUserId(event.instagramUserId())
                .username(event.username())
                .source(event.type() != null ? event.type().name() : null)
                .lastMessage(lastMessage)
                .build();

        String igAccountId = event.receivingAccountId();

        Contact saved;
        if (igAccountId != null && !igAccountId.isBlank()) {
            saved = repository.upsertByInstagramUserId(uid, igAccountId, contact);
        } else {
            saved = repository.upsertByInstagramUserId(uid, contact);
        }

        log.debug("Contact upserted uid={} igAccountId={} ig={} source={}",
                uid, igAccountId, saved.getInstagramUserId(), saved.getSource());

        return saved;
    }
}