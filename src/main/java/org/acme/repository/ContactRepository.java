package org.acme.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.Contact;
import org.acme.model.User;

import java.util.List;

@ApplicationScoped
public class ContactRepository implements PanacheRepository<Contact> {

    public List<Contact> findByOwner(Long ownerId) {
        return find("owner.id = ?1", ownerId).list();
    }

    public boolean exists(Long ownerId, Long contactId) {
        return find("owner.id = ?1 AND contact.id = ?2", ownerId, contactId).firstResult() != null;
    }
}