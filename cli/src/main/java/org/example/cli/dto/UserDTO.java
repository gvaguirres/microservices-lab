package org.example.cli.dto;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber) {}
