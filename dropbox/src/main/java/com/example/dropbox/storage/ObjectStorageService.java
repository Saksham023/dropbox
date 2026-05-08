package com.example.dropbox.storage;

import java.time.Instant;
import java.util.List;

public interface ObjectStorageService {

    MultipartUpload createMultipartUpload(String objectKey, String contentType);

    PresignedPartUpload createPresignedPartUpload(String objectKey, String uploadId, int partNumber);

    void completeMultipartUpload(String objectKey, String uploadId, List<CompletedPart> parts);

    void abortMultipartUpload(String objectKey, String uploadId);

    PresignedDownload createPresignedDownload(String objectKey, String fileName);

    record MultipartUpload(String uploadId) {
    }

    record PresignedPartUpload(int partNumber, String url, Instant expiresAt) {
    }

    record CompletedPart(int partNumber, String etag) {
    }

    record PresignedDownload(String url, Instant expiresAt) {
    }
}
