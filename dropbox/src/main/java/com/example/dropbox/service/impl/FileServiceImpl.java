package com.example.dropbox.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.FileListItemResponse;
import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;
import com.example.dropbox.entity.File;
import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;
import com.example.dropbox.repository.FileRepository;
import com.example.dropbox.repository.UserRepository;
import com.example.dropbox.service.FileService;
import com.example.dropbox.storage.ObjectStorageService;
import com.github.f4b6a3.uuid.UuidCreator;

@Service
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;

    public FileServiceImpl(
            FileRepository fileRepository,
            UserRepository userRepository,
            ObjectStorageService objectStorageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileListItemResponse> listFiles(UUID ownerId, UUID parentId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }

        return fileRepository.findVisibleByOwnerAndParent(ownerId, parentId).stream()
                .map(file -> new FileListItemResponse(
                        file.getId(),
                        file.getParent() == null ? null : file.getParent().getId(),
                        file.getName(),
                        file.getType(),
                        file.getMimeType(),
                        file.getSize(),
                        file.getStatus(),
                        file.getObjectKey(),
                        buildDownloadUrl(file),
                        file.getCreatedAt(),
                        file.getUpdatedAt()))
                .toList();
    }

    private String buildDownloadUrl(File file) {
        if (file.getType() != FileType.FILE) {
            return null;
        }
//        if (file.getStatus() != FileStatus.READY) {
//            return null;
//        }
        if (!StringUtils.hasText(file.getObjectKey())) {
            return null;
        }
        return objectStorageService.createPresignedDownload(file.getObjectKey()).url();
    }

    @Override
    @Transactional
    public CreateFolderResponse createFolder(CreateFolderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.ownerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("name is required");
        }

        var owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + request.ownerId()));
        File parent = resolveParentFolder(request.ownerId(), request.parentId());

        var now = LocalDateTime.now();
        var folder = new File();
        folder.setId(UuidCreator.getTimeOrderedEpoch());
        folder.setOwner(owner);
        folder.setParent(parent);
        folder.setName(request.name().trim());
        folder.setType(FileType.FOLDER);
        folder.setMimeType(null);
        folder.setObjectKey(null);
        folder.setSize(0L);
        folder.setStatus(FileStatus.READY);
        folder.setCreatedAt(now);
        folder.setUpdatedAt(now);

        fileRepository.save(folder);

        return new CreateFolderResponse(
                folder.getId(),
                owner.getId(),
                parent == null ? null : parent.getId(),
                folder.getName(),
                folder.getType(),
                folder.getStatus(),
                folder.getSize(),
                folder.getCreatedAt(),
                folder.getUpdatedAt());
    }

    @Override
    @Transactional
    public InitiateUploadResponse initiateUpload(InitiateUploadRequest request) {
        validateUploadRequest(request);

        var owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + request.ownerId()));
        File parent = resolveParentFolder(request.ownerId(), request.parentId());

        UUID fileId = UuidCreator.getTimeOrderedEpoch();
        String objectKey = "users/" + owner.getId() + "/files/" + fileId + "/original";
        LocalDateTime now = LocalDateTime.now();

        var file = new File();
        file.setId(fileId);
        file.setOwner(owner);
        file.setParent(parent);
        file.setName(request.name().trim());
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

    private File resolveParentFolder(UUID ownerId, UUID parentId) {
        if (parentId == null) {
            return null;
        }

        var parent = fileRepository.findByIdAndOwner_Id(parentId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Parent folder not found: " + parentId));

        if (parent.getType() != FileType.FOLDER) {
            throw new IllegalArgumentException("parentId must reference a folder");
        }

        return parent;
    }

    private static void validateUploadRequest(InitiateUploadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
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
