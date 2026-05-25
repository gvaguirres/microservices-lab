package org.example.messageservice.dto;

public record CreateMessageDTO(
        Long senderId,
        Long receiverId,
        String text){}
