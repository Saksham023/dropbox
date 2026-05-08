package com.example.dropbox.dto;

import java.util.UUID;

public record RecordUploadPartRequest(
        UUID ownerId,
        UUID uploadSessionId,
        int partNumber,
        String etag,
        long size) {
}
