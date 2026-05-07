package com.example.dropbox.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String email,
        String fullName,
        long storageUsed,
        LocalDateTime createdAt) {
}
