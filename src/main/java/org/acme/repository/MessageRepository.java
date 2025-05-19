package org.acme.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.Message;
import org.acme.model.User;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class MessageRepository implements PanacheRepository<Message> {

    public List<Message> findConversation(Long userId1, Long userId2) {
        return find("SELECT m FROM Message m " +
                "JOIN FETCH m.sender " +
                "JOIN FETCH m.recipient " +
                "WHERE (m.sender.id = ?1 AND m.recipient.id = ?2) OR " +
                "(m.sender.id = ?2 AND m.recipient.id = ?1) " +
                "ORDER BY m.timestamp", userId1, userId2).list();
    }

    public List<Message> findByParticipant(Long userId) {
        return find("SELECT m FROM Message m " +
                "JOIN FETCH m.sender " +
                "JOIN FETCH m.recipient " +
                "WHERE m.sender.id = ?1 OR m.recipient.id = ?1 " +
                "ORDER BY m.timestamp", userId).list();
    }

    public Message create(User sender, User recipient, String content) {
        Message message = new Message();
        message.sender = sender;
        message.recipient = recipient;
        message.content = content;
        message.timestamp = LocalDateTime.now();
        message.persist();
        return message;
    }
}