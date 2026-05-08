package com.example.dropbox.dto;

import java.time.Instant;

public record PresignedPartUploadResponse(
        int partNumber,
        String uploadUrl,
        Instant expiresAt) {
}
