package org.acme.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends PanacheEntity {
    public String token;
    public Long userId;
    public LocalDateTime expiryDate;

    public static RefreshToken findByToken(String token) {
        return find("token", token).firstResult();
    }
}