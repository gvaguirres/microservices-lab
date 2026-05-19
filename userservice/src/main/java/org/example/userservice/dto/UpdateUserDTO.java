package org.example.userservice.dto;

public record UpdateUserDTO(
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {}
