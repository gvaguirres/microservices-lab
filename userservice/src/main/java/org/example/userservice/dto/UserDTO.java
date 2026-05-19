package org.example.userservice.dto;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber
) {}
