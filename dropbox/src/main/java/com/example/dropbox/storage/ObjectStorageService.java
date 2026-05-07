package com.example.dropbox.storage;

import java.time.Instant;

public interface ObjectStorageService {

    PresignedUpload createPresignedUpload(String objectKey, String contentType);

    PresignedDownload createPresignedDownload(String objectKey);

    record PresignedUpload(String url, Instant expiresAt) {
    }

    record PresignedDownload(String url, Instant expiresAt) {
    }
}
