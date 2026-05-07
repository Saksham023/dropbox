package com.example.dropbox.dto;

public record LoginRequest(
        String email,
        String password) {
}
