package org.acme.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class MessageDTO {

    @Schema(description = "Request for creating a new message")
    public static class CreateMessage {
        @Schema(description = "Recipient user ID", required = true, implementation = Long.class, format = "int64", minimum = "1", examples = {"1", "5", "999"}, defaultValue = "1")

        public Long recipientId;
        @Schema(description = "Message content", required = true, minLength = 1, maxLength = 2000, examples = {"Hello there!", "Can we meet tomorrow?"}, defaultValue = "Hi there!")
        public String content;
    }

    @Schema(description = "Message response")
    public static class MessageResponse {
        @Schema(description = "Unique message identifier", implementation = Long.class, format = "int64", examples = {"1", "5", "999"}, defaultValue = "1")
        public Long id;

        @Schema(description = "Sender user ID", implementation = Long.class, format = "int64", examples = {"4", "99"}, defaultValue = "1")
        public Long senderId;

        @Schema(description = "Sender username starting with @", examples = {"@User123", "@Sender"}, defaultValue = "@DefaultUser")
        public String senderUsername;

        @Schema(description = "Recipient user ID", implementation = Long.class, format = "int64", examples = {"1", "3", "102"}, defaultValue = "1")
        public Long recipientId;

        @Schema(description = "Recipient username starting with @", examples = {"@User456", "@Recipient"}, defaultValue = "@DefaultRecipient")

        public String recipientUsername;
        @Schema(description = "Message content (max 2000 characters)", minLength = 1, maxLength = 2000, examples = {"Hi there!", "Don't forget our meeting"}, defaultValue = "Hi there!")

        public String content;
        @Schema(description = "Timestamp when message was sent in ISO 8601 format", implementation = String.class, format = "date-time", pattern = "yyyy-MM-dd'T'HH:mm:ss", defaultValue = "2024-01-01T00:00:00")
        public String timestamp;

        public MessageResponse(Long id,
                               Long senderId,
                               String senderUsername,
                               Long recipientId,
                               String recipientUsername,
                               String content,
                               String timestamp
        ) {
            this.id = id;
            this.senderId = senderId;
            this.senderUsername = senderUsername;
            this.recipientId = recipientId;
            this.recipientUsername = recipientUsername;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}