package com.example.dropbox.dto;

import java.util.UUID;

public record InitiateUploadRequest(
        UUID ownerId,
        UUID parentId,
        String name,
        String mimeType,
        long size) {
}
