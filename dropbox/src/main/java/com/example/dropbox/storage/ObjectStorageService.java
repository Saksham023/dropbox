package com.example.dropbox.storage;

import java.time.Instant;

public interface ObjectStorageService {

    PresignedUpload createPresignedUpload(String objectKey, String contentType);

    record PresignedUpload(String url, Instant expiresAt) {
    }
}
