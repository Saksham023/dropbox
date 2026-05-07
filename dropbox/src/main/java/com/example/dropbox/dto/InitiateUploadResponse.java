package com.example.dropbox.dto;

import java.time.Instant;
import java.util.UUID;

public record InitiateUploadResponse(
        UUID fileId,
        String objectKey,
        String uploadUrl,
        Instant expiresAt) {
}
