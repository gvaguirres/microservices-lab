package org.example.bff.dto;

public record MessageDTO(
        Long id,
        Long senderId,
        Long receiverId,
        String text){}
