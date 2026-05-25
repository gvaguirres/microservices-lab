package org.example.messageservice.dto;

import java.time.LocalDateTime;

public record MessageDTO(
        Long id,
        Long senderId,
        Long receiverId,
        String text,
        LocalDateTime createdAt) {}
