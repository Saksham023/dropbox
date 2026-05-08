package com.example.dropbox.dto;

import java.util.List;
import java.util.UUID;

import com.example.dropbox.enums.UploadStatus;

public record UploadSessionResponse(
        UUID fileId,
        UUID uploadSessionId,
        String objectKey,
        long chunkSize,
        int totalChunks,
        int uploadedChunks,
        UploadStatus status,
        List<Integer> uploadedPartNumbers,
        List<Integer> missingPartNumbers,
        List<PresignedPartUploadResponse> partUploadUrls) {
}
