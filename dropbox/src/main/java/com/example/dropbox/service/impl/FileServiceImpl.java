package com.example.dropbox.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.CompleteUploadResponse;
import com.example.dropbox.dto.FileListItemResponse;
import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;
import com.example.dropbox.dto.PresignedPartUploadResponse;
import com.example.dropbox.dto.RecordUploadPartRequest;
import com.example.dropbox.dto.RecordUploadPartResponse;
import com.example.dropbox.dto.UploadSessionActionRequest;
import com.example.dropbox.dto.UploadSessionResponse;
import com.example.dropbox.entity.File;
import com.example.dropbox.entity.UploadPart;
import com.example.dropbox.entity.UploadSession;
import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;
import com.example.dropbox.enums.UploadStatus;
import com.example.dropbox.repository.FileRepository;
import com.example.dropbox.repository.UploadPartRepository;
import com.example.dropbox.repository.UploadSessionRepository;
import com.example.dropbox.repository.UserRepository;
import com.example.dropbox.service.FileService;
import com.example.dropbox.storage.ObjectStorageService;
import com.github.f4b6a3.uuid.UuidCreator;

@Service
public class FileServiceImpl implements FileService {

    private static final long DEFAULT_CHUNK_SIZE = 8L * 1024L * 1024L;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadPartRepository uploadPartRepository;
    private final ObjectStorageService objectStorageService;

    public FileServiceImpl(
            FileRepository fileRepository,
            UserRepository userRepository,
            UploadSessionRepository uploadSessionRepository,
            UploadPartRepository uploadPartRepository,
            ObjectStorageService objectStorageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.uploadPartRepository = uploadPartRepository;
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
        if (file.getStatus() != FileStatus.READY) {
            return null;
        }
        if (!StringUtils.hasText(file.getObjectKey())) {
            return null;
        }
        return objectStorageService.createPresignedDownload(file.getObjectKey(), file.getName()).url();
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

        var multipartUpload = objectStorageService.createMultipartUpload(objectKey, request.mimeType());
        int totalChunks = calculateTotalChunks(request.size(), DEFAULT_CHUNK_SIZE);

        var uploadSession = new UploadSession();
        uploadSession.setId(UuidCreator.getTimeOrderedEpoch());
        uploadSession.setFile(file);
        uploadSession.setStorageUploadId(multipartUpload.uploadId());
        uploadSession.setChunkSize(DEFAULT_CHUNK_SIZE);
        uploadSession.setTotalChunks(totalChunks);
        uploadSession.setUploadedChunks(0);
        uploadSession.setStatus(UploadStatus.INITIATED);
        uploadSession.setCreatedAt(now);
        uploadSession.setCompletedAt(null);

        uploadSessionRepository.save(uploadSession);

        return new InitiateUploadResponse(
                fileId,
                uploadSession.getId(),
                objectKey,
                DEFAULT_CHUNK_SIZE,
                totalChunks,
                createPresignedPartUploads(file, uploadSession, allPartNumbers(totalChunks)));
    }

    @Override
    @Transactional
    public RecordUploadPartResponse recordUploadPart(UUID fileId, RecordUploadPartRequest request) {
        validateRecordUploadPartRequest(fileId, request);
        var uploadSession = resolveUploadSession(fileId, request.ownerId(), request.uploadSessionId());

        if (request.partNumber() > uploadSession.getTotalChunks()) {
            throw new IllegalArgumentException("partNumber must be within totalChunks");
        }
        if (uploadSession.getStatus() == UploadStatus.COMPLETED || uploadSession.getStatus() == UploadStatus.ABORTED) {
            throw new IllegalArgumentException("upload session is not accepting parts");
        }

        uploadPartRepository.findByUploadSession_IdAndPartNumber(uploadSession.getId(), request.partNumber())
                .orElseGet(() -> {
                    var uploadPart = new UploadPart();
                    uploadPart.setId(UuidCreator.getTimeOrderedEpoch());
                    uploadPart.setUploadSession(uploadSession);
                    uploadPart.setPartNumber(request.partNumber());
                    uploadPart.setEtag(request.etag().trim());
                    uploadPart.setSize(request.size());
                    uploadPart.setUploadedAt(LocalDateTime.now());
                    return uploadPartRepository.save(uploadPart);
                });

        int uploadedChunks = Math.toIntExact(uploadPartRepository.countByUploadSession_Id(uploadSession.getId()));
        uploadSession.setUploadedChunks(uploadedChunks);
        if (uploadSession.getStatus() == UploadStatus.INITIATED && uploadedChunks > 0) {
            uploadSession.setStatus(UploadStatus.IN_PROGRESS);
        }
        uploadSessionRepository.save(uploadSession);

        return new RecordUploadPartResponse(
                uploadSession.getId(),
                request.partNumber(),
                uploadedChunks,
                uploadSession.getTotalChunks(),
                uploadSession.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public UploadSessionResponse getUploadSession(UUID fileId, UUID ownerId) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("ownerId is required");
        }

        var uploadSession = uploadSessionRepository.findFirstByFile_IdAndFile_Owner_IdOrderByCreatedAtDesc(fileId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found for file: " + fileId));

        return buildUploadSessionResponse(uploadSession);
    }

    @Override
    @Transactional
    public CompleteUploadResponse completeUpload(UUID fileId, UploadSessionActionRequest request) {
        validateUploadSessionActionRequest(fileId, request);
        var uploadSession = resolveUploadSession(fileId, request.ownerId(), request.uploadSessionId());
        var parts = uploadPartRepository.findByUploadSession_IdOrderByPartNumberAsc(uploadSession.getId());
        var missingPartNumbers = missingPartNumbers(uploadSession, parts);

        if (!missingPartNumbers.isEmpty()) {
            throw new IllegalArgumentException("Cannot complete upload; missing parts: " + missingPartNumbers);
        }
        if (uploadSession.getStatus() == UploadStatus.COMPLETED) {
            return new CompleteUploadResponse(
                    uploadSession.getFile().getId(),
                    uploadSession.getId(),
                    uploadSession.getFile().getStatus(),
                    uploadSession.getStatus());
        }
        if (uploadSession.getStatus() == UploadStatus.ABORTED) {
            throw new IllegalArgumentException("upload session has been aborted");
        }

        var completedParts = parts.stream()
                .map(part -> new ObjectStorageService.CompletedPart(part.getPartNumber(), part.getEtag()))
                .toList();

        objectStorageService.completeMultipartUpload(
                uploadSession.getFile().getObjectKey(),
                uploadSession.getStorageUploadId(),
                completedParts);

        var now = LocalDateTime.now();
        uploadSession.setUploadedChunks(parts.size());
        uploadSession.setStatus(UploadStatus.COMPLETED);
        uploadSession.setCompletedAt(now);

        var file = uploadSession.getFile();
        file.setStatus(FileStatus.READY);
        file.setUpdatedAt(now);

        uploadSessionRepository.save(uploadSession);
        fileRepository.save(file);

        return new CompleteUploadResponse(file.getId(), uploadSession.getId(), file.getStatus(), uploadSession.getStatus());
    }

    @Override
    @Transactional
    public CompleteUploadResponse abortUpload(UUID fileId, UploadSessionActionRequest request) {
        validateUploadSessionActionRequest(fileId, request);
        var uploadSession = resolveUploadSession(fileId, request.ownerId(), request.uploadSessionId());

        if (uploadSession.getStatus() != UploadStatus.COMPLETED && uploadSession.getStatus() != UploadStatus.ABORTED) {
            objectStorageService.abortMultipartUpload(
                    uploadSession.getFile().getObjectKey(),
                    uploadSession.getStorageUploadId());
        }

        var now = LocalDateTime.now();
        uploadSession.setStatus(UploadStatus.ABORTED);
        uploadSession.setCompletedAt(now);

        var file = uploadSession.getFile();
        file.setStatus(FileStatus.FAILED);
        file.setUpdatedAt(now);

        uploadSessionRepository.save(uploadSession);
        fileRepository.save(file);

        return new CompleteUploadResponse(file.getId(), uploadSession.getId(), file.getStatus(), uploadSession.getStatus());
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

    private UploadSession resolveUploadSession(UUID fileId, UUID ownerId, UUID uploadSessionId) {
        return uploadSessionRepository.findByIdAndFile_IdAndFile_Owner_Id(uploadSessionId, fileId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found: " + uploadSessionId));
    }

    private UploadSessionResponse buildUploadSessionResponse(UploadSession uploadSession) {
        var parts = uploadPartRepository.findByUploadSession_IdOrderByPartNumberAsc(uploadSession.getId());
        var uploadedPartNumbers = parts.stream()
                .map(UploadPart::getPartNumber)
                .toList();
        var missingPartNumbers = missingPartNumbers(uploadSession, parts);

        return new UploadSessionResponse(
                uploadSession.getFile().getId(),
                uploadSession.getId(),
                uploadSession.getFile().getObjectKey(),
                uploadSession.getChunkSize(),
                uploadSession.getTotalChunks(),
                uploadSession.getUploadedChunks(),
                uploadSession.getStatus(),
                uploadedPartNumbers,
                missingPartNumbers,
                createPresignedPartUploads(uploadSession.getFile(), uploadSession, missingPartNumbers));
    }

    private List<PresignedPartUploadResponse> createPresignedPartUploads(
            File file,
            UploadSession uploadSession,
            List<Integer> partNumbers) {
        return partNumbers.stream()
                .map(partNumber -> {
                    var presigned = objectStorageService.createPresignedPartUpload(
                            file.getObjectKey(),
                            uploadSession.getStorageUploadId(),
                            partNumber);
                    return new PresignedPartUploadResponse(
                            presigned.partNumber(),
                            presigned.url(),
                            presigned.expiresAt());
                })
                .toList();
    }

    private static int calculateTotalChunks(long size, long chunkSize) {
        return Math.toIntExact((size + chunkSize - 1) / chunkSize);
    }

    private static List<Integer> allPartNumbers(int totalChunks) {
        return IntStream.rangeClosed(1, totalChunks)
                .boxed()
                .toList();
    }

    private static List<Integer> missingPartNumbers(UploadSession uploadSession, List<UploadPart> parts) {
        var uploaded = new HashSet<Integer>();
        parts.forEach(part -> uploaded.add(part.getPartNumber()));

        var missing = new ArrayList<Integer>();
        for (int partNumber = 1; partNumber <= uploadSession.getTotalChunks(); partNumber++) {
            if (!uploaded.contains(partNumber)) {
                missing.add(partNumber);
            }
        }
        return missing;
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

    private static void validateRecordUploadPartRequest(UUID fileId, RecordUploadPartRequest request) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.ownerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (request.uploadSessionId() == null) {
            throw new IllegalArgumentException("uploadSessionId is required");
        }
        if (request.partNumber() <= 0) {
            throw new IllegalArgumentException("partNumber must be positive");
        }
        if (!StringUtils.hasText(request.etag())) {
            throw new IllegalArgumentException("etag is required");
        }
        if (request.size() <= 0L) {
            throw new IllegalArgumentException("size must be positive");
        }
    }

    private static void validateUploadSessionActionRequest(UUID fileId, UploadSessionActionRequest request) {
        if (fileId == null) {
            throw new IllegalArgumentException("fileId is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request body is required");
        }
        if (request.ownerId() == null) {
            throw new IllegalArgumentException("ownerId is required");
        }
        if (request.uploadSessionId() == null) {
            throw new IllegalArgumentException("uploadSessionId is required");
        }
    }
}
