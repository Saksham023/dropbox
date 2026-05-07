package com.example.dropbox.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;

public record CreateFolderResponse(
        UUID id,
        UUID ownerId,
        UUID parentId,
        String name,
        FileType type,
        FileStatus status,
        long size,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
