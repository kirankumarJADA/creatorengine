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

    public List<Contact> list(String uid) {
        return repository.listForUser(uid);
    }

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