package com.example.dropbox.storage;

import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.dropbox.config.StorageProperties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

@Service
public class R2ObjectStorageService implements ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    public R2ObjectStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    @Override
    public MultipartUpload createMultipartUpload(String objectKey, String contentType) {
        var request = CreateMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build();

        var response = s3Client.createMultipartUpload(request);
        return new MultipartUpload(response.uploadId());
    }

    @Override
    public PresignedPartUpload createPresignedPartUpload(String objectKey, String uploadId, int partNumber) {
        var uploadPartRequest = UploadPartRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        var presignedRequest = s3Presigner.presignUploadPart(UploadPartPresignRequest.builder()
                .uploadPartRequest(uploadPartRequest)
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpiryMinutes()))
                .build());

        return new PresignedPartUpload(partNumber, presignedRequest.url().toString(), presignedRequest.expiration());
    }

    @Override
    public void completeMultipartUpload(
            String objectKey,
            String uploadId,
            List<ObjectStorageService.CompletedPart> parts) {
        var completedParts = parts.stream()
                .map(part -> software.amazon.awssdk.services.s3.model.CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.etag())
                        .build())
                .toList();

        var completedUpload = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        s3Client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .multipartUpload(completedUpload)
                .build());
    }

    @Override
    public void abortMultipartUpload(String objectKey, String uploadId) {
        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .uploadId(uploadId)
                .build());
    }

    @Override
    public PresignedDownload createPresignedDownload(String objectKey, String fileName) {
        var getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(objectKey)
                .responseContentDisposition(contentDisposition(fileName))
                .build();

        var presignedRequest = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(properties.getPresignedUrlExpiryMinutes()))
                .build());

        return new PresignedDownload(presignedRequest.url().toString(), presignedRequest.expiration());
    }

    private static String contentDisposition(String fileName) {
        var safeFileName = fileName == null ? "download" : fileName.replace("\"", "");
        var encodedFileName = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + safeFileName + "\"; filename*=UTF-8''" + encodedFileName;
    }
}
