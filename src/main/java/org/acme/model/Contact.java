package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class Contact extends PanacheEntity {

    @ManyToOne
    public User owner; // Владелец списка контактов

    @ManyToOne
    public User contact; // Пользователь, добавленный в контакты

    public static boolean exists(Long ownerId, Long contactId) {
        return find("owner.id = ?1 AND contact.id = ?2", ownerId, contactId).firstResult() != null;
    }
}