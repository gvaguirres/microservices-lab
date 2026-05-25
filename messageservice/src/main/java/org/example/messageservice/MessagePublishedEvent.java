package org.example.messageservice;

import java.time.LocalDateTime;

public record MessagePublishedEvent(
        Long messageId,
        Long senderId,
        Long receiverId,
        String message,
        LocalDateTime createdAt) {}
