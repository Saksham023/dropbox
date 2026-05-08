package com.example.dropbox.dto;

import java.util.UUID;

import com.example.dropbox.enums.UploadStatus;

public record RecordUploadPartResponse(
        UUID uploadSessionId,
        int partNumber,
        int uploadedChunks,
        int totalChunks,
        UploadStatus status) {
}
