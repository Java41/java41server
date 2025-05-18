package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Message extends PanacheEntity {

    @ManyToOne
    public User sender;

    @ManyToOne
    public User recipient;

    public String content;
    public LocalDateTime timestamp;

    public static Message create(User sender, User recipient, String content) {
        Message message = new Message();
        message.sender = sender;
        message.recipient = recipient;
        message.content = content;
        message.timestamp = LocalDateTime.now();
        message.persist();
        return message;
    }

    public static List<Message> findBySender(User sender) {
        return find("sender", sender).list();
    }

    public static List<Message> findByRecipient(User recipient) {
        return find("recipient", recipient).list();
    }

    public static List<Message> findConversation(Long userId1, Long userId2) {
        return find("(sender.id = ?1 and recipient.id = ?2) or (sender.id = ?2 and recipient.id = ?1) order by timestamp",
                userId1, userId2).list();
    }

    public static List<Message> findByParticipant(Long userId) {
        return find("sender.id = ?1 or recipient.id = ?1 order by timestamp", userId).list();
    }
}