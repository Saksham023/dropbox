package com.example.dropbox.storage;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.example.dropbox.config.StorageProperties;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class R2ObjectStorageService implements ObjectStorageService {

    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public R2ObjectStorageService(S3Presigner s3Presigner, StorageProperties properties) {
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public PresignedUpload createPresignedUpload(String objectKey, String contentType) {
        var putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        var presignedRequest = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .putObjectRequest(putObjectRequest)
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpiryMinutes()))
                .build());

        return new PresignedUpload(presignedRequest.url().toString(), presignedRequest.expiration());
    }
}
