package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

@Entity
public class Message extends PanacheEntity {

    @ManyToOne
    public User sender;

    @ManyToOne
    public User recipient;

    public String content;
    public LocalDateTime timestamp;




}