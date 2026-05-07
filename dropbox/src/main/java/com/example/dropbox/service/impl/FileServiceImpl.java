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

        File parent = null;
        if (request.parentId() != null) {
            parent = fileRepository.findByIdAndOwner_Id(request.parentId(), request.ownerId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found: " + request.parentId()));

            if (parent.getType() != FileType.FOLDER) {
                throw new IllegalArgumentException("parentId must reference a folder");
            }
        }

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
}
