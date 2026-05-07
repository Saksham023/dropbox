package com.example.dropbox.dto;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        String fullName) {
}
