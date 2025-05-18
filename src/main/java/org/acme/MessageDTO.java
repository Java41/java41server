package org.acme;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class MessageDTO {

    @Schema(description = "Request for creating a new message")
    public static class CreateMessage {
        @Schema(
                description = "Recipient user ID",
                required = true,
                implementation = Long.class
        )
        public Long recipientId;

        @Schema(
                description = "Message content",
                required = true,
                examples = {"Hello there!", "Can we meet tomorrow?"}
        )
        public String content;
    }

    @Schema(description = "Message response")
    public static class MessageResponse {
        @Schema(
                description = "Unique message identifier",
                implementation = Long.class
        )
        public Long id;

        @Schema(
                description = "Sender user ID",
                implementation = Long.class
        )
        public Long senderId;

        @Schema(
                description = "Recipient user ID",
                implementation = Long.class
        )
        public Long recipientId;

        @Schema(
                description = "Message content",
                examples = {"Hi there!", "Don't forget our meeting"}
        )
        public String content;

        @Schema(
                description = "Timestamp when message was sent (ISO format)",
                pattern = "yyyy-MM-dd'T'HH:mm:ss"
        )
        public String timestamp;

        public MessageResponse(Long id, Long senderId, Long recipientId,
                               String content, String timestamp) {
            this.id = id;
            this.senderId = senderId;
            this.recipientId = recipientId;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}