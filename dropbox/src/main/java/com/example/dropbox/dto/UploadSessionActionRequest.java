package com.example.dropbox.dto;

import java.util.UUID;

public record UploadSessionActionRequest(
        UUID ownerId,
        UUID uploadSessionId) {
}
