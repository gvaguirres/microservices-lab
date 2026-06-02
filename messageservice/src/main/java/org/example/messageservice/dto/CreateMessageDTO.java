package org.example.messageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMessageDTO(
        @NotNull @Positive Long senderId,
        @NotNull @Positive Long receiverId,
        @NotBlank String text) {}
