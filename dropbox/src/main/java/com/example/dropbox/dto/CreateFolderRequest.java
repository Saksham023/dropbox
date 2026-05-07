package com.example.dropbox.dto;

import java.util.UUID;

public record CreateFolderRequest(
        UUID ownerId,
        UUID parentId,
        String name) {
}
