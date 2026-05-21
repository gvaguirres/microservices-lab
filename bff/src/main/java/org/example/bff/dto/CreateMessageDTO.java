package org.example.bff.dto;

public record CreateMessageDTO(
        Long senderId,
        Long receiverId,
        String message){}
