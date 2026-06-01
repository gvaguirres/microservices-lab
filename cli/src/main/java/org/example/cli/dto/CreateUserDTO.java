package org.example.cli.dto;

public record CreateUserDTO(
        String firstName,
        String lastName,
        String email,
        String phoneNumber) {}
