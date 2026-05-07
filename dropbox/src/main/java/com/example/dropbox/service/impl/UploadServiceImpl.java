package com.example.dropbox.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;
import com.example.dropbox.entity.File;
import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;
import com.example.dropbox.repository.FileRepository;
import com.example.dropbox.repository.UserRepository;
import com.example.dropbox.service.UploadService;
import com.example.dropbox.storage.ObjectStorageService;
import com.github.f4b6a3.uuid.UuidCreator;

@Service
public class UploadServiceImpl implements UploadService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;

    public UploadServiceImpl(
            FileRepository fileRepository,
            UserRepository userRepository,
            ObjectStorageService objectStorageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Override
    @Transactional
    public InitiateUploadResponse initiateUpload(InitiateUploadRequest request) {
        validateRequest(request);

        var owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + request.ownerId()));

        File parent = null;
        if (request.parentId() != null) {
            parent = fileRepository.findById(request.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + request.parentId()));
        }

        UUID fileId = UuidCreator.getTimeOrderedEpoch();
        String objectKey = "users/" + owner.getId() + "/files/" + fileId + "/original";
        LocalDateTime now = LocalDateTime.now();

        var file = new File();
        file.setId(fileId);
        file.setOwner(owner);
        file.setParent(parent);
        file.setName(request.name());
        file.setType(FileType.FILE);
        file.setMimeType(request.mimeType());
        file.setObjectKey(objectKey);
        file.setSize(request.size());
        file.setStatus(FileStatus.UPLOADING);
        file.setCreatedAt(now);
        file.setUpdatedAt(now);

        fileRepository.save(file);

        var presigned = objectStorageService.createPresignedUpload(objectKey, request.mimeType());
        return new InitiateUploadResponse(fileId, objectKey, presigned.url(), presigned.expiresAt());
    }

    private static void validateRequest(InitiateUploadRequest request) {
        if (request.ownerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("name is required");
        }
        if (!StringUtils.hasText(request.mimeType())) {
            throw new IllegalArgumentException("mimeType is required");
        }
        if (request.size() <= 0L) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
