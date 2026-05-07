package com.example.dropbox.dto;

public record CreateUserRequest(
        String email,
        String password,
        String fullName) {
}
