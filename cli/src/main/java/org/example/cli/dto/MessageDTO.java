package org.example.cli.dto;

public record MessageDTO(
        Long id,
        Long senderId,
        Long receiverId,
        String text){}
