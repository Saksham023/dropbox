package com.example.dropbox.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;

public record FileListItemResponse(
        UUID id,
        UUID parentId,
        String name,
        FileType type,
        String mimeType,
        long size,
        FileStatus status,
        String objectKey,
        String downloadUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
