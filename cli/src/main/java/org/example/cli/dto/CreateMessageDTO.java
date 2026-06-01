package org.example.cli.dto;

public record CreateMessageDTO(
        Long senderId,
        Long receiverId,
        String text){}
