package com.example.dropbox.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.RecordUploadPartRequest;
import com.example.dropbox.dto.UploadSessionActionRequest;
import com.example.dropbox.entity.File;
import com.example.dropbox.entity.UploadPart;
import com.example.dropbox.entity.UploadSession;
import com.example.dropbox.entity.User;
import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.FileType;
import com.example.dropbox.enums.UploadStatus;
import com.example.dropbox.repository.FileRepository;
import com.example.dropbox.repository.UploadPartRepository;
import com.example.dropbox.repository.UploadSessionRepository;
import com.example.dropbox.repository.UserRepository;
import com.example.dropbox.storage.ObjectStorageService;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private static final long CHUNK_SIZE = 8L * 1024L * 1024L;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private UploadPartRepository uploadPartRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileServiceImpl(
                fileRepository,
                userRepository,
                uploadSessionRepository,
                uploadPartRepository,
                objectStorageService);
    }

    @Test
    void initiateUploadCreatesMultipartSessionAndPartUrls() {
        var owner = user();
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(objectStorageService.createMultipartUpload(any(), any()))
                .thenReturn(new ObjectStorageService.MultipartUpload("storage-upload-id"));
        when(objectStorageService.createPresignedPartUpload(any(), any(), any(Integer.class)))
                .thenAnswer(invocation -> new ObjectStorageService.PresignedPartUpload(
                        invocation.getArgument(2),
                        "https://upload.test/part-" + invocation.getArgument(2),
                        Instant.now().plusSeconds(900)));

        var response = fileService.initiateUpload(new InitiateUploadRequest(
                owner.getId(),
                null,
                "video.mp4",
                "video/mp4",
                CHUNK_SIZE + 1L));

        var sessionCaptor = ArgumentCaptor.forClass(UploadSession.class);
        verify(uploadSessionRepository).save(sessionCaptor.capture());

        assertThat(response.fileId()).isNotNull();
        assertThat(response.uploadSessionId()).isEqualTo(sessionCaptor.getValue().getId());
        assertThat(response.chunkSize()).isEqualTo(CHUNK_SIZE);
        assertThat(response.totalChunks()).isEqualTo(2);
        assertThat(response.partUploadUrls()).extracting("partNumber").containsExactly(1, 2);
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(UploadStatus.INITIATED);
        assertThat(sessionCaptor.getValue().getUploadedChunks()).isZero();
    }

    @Test
    void recordingDuplicatePartDoesNotCreateAnotherPartOrInflateCount() {
        var owner = user();
        var file = file(owner);
        var session = uploadSession(file, 3);
        var existingPart = uploadPart(session, 1, "\"etag-1\"");

        when(uploadSessionRepository.findByIdAndFile_IdAndFile_Owner_Id(session.getId(), file.getId(), owner.getId()))
                .thenReturn(Optional.of(session));
        when(uploadPartRepository.findByUploadSession_IdAndPartNumber(session.getId(), 1))
                .thenReturn(Optional.of(existingPart));
        when(uploadPartRepository.countByUploadSession_Id(session.getId())).thenReturn(1L);

        var response = fileService.recordUploadPart(file.getId(), new RecordUploadPartRequest(
                owner.getId(),
                session.getId(),
                1,
                "\"etag-1\"",
                CHUNK_SIZE));

        verify(uploadPartRepository, never()).save(any());
        assertThat(response.uploadedChunks()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(UploadStatus.IN_PROGRESS);
        assertThat(session.getUploadedChunks()).isEqualTo(1);
    }

    @Test
    void completeUploadRejectsMissingParts() {
        var owner = user();
        var file = file(owner);
        var session = uploadSession(file, 2);

        when(uploadSessionRepository.findByIdAndFile_IdAndFile_Owner_Id(session.getId(), file.getId(), owner.getId()))
                .thenReturn(Optional.of(session));
        when(uploadPartRepository.findByUploadSession_IdOrderByPartNumberAsc(session.getId()))
                .thenReturn(List.of(uploadPart(session, 1, "\"etag-1\"")));

        assertThatThrownBy(() -> fileService.completeUpload(file.getId(), new UploadSessionActionRequest(
                owner.getId(),
                session.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing parts");

        verify(objectStorageService, never()).completeMultipartUpload(any(), any(), any());
    }

    @Test
    void completeUploadMarksFileReadyWhenAllPartsExist() {
        var owner = user();
        var file = file(owner);
        var session = uploadSession(file, 2);
        var parts = List.of(
                uploadPart(session, 1, "\"etag-1\""),
                uploadPart(session, 2, "\"etag-2\""));

        when(uploadSessionRepository.findByIdAndFile_IdAndFile_Owner_Id(session.getId(), file.getId(), owner.getId()))
                .thenReturn(Optional.of(session));
        when(uploadPartRepository.findByUploadSession_IdOrderByPartNumberAsc(session.getId()))
                .thenReturn(parts);

        var response = fileService.completeUpload(file.getId(), new UploadSessionActionRequest(owner.getId(), session.getId()));

        verify(objectStorageService).completeMultipartUpload(any(), any(), any());
        assertThat(response.fileStatus()).isEqualTo(FileStatus.READY);
        assertThat(response.uploadStatus()).isEqualTo(UploadStatus.COMPLETED);
        assertThat(file.getStatus()).isEqualTo(FileStatus.READY);
        assertThat(session.getStatus()).isEqualTo(UploadStatus.COMPLETED);
        assertThat(session.getCompletedAt()).isNotNull();
    }

    private static User user() {
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private static File file(User owner) {
        var file = new File();
        file.setId(UUID.randomUUID());
        file.setOwner(owner);
        file.setName("video.mp4");
        file.setType(FileType.FILE);
        file.setMimeType("video/mp4");
        file.setObjectKey("users/" + owner.getId() + "/files/" + UUID.randomUUID() + "/original");
        file.setSize(CHUNK_SIZE + 1L);
        file.setStatus(FileStatus.UPLOADING);
        file.setCreatedAt(LocalDateTime.now());
        file.setUpdatedAt(LocalDateTime.now());
        return file;
    }

    private static UploadSession uploadSession(File file, int totalChunks) {
        var session = new UploadSession();
        session.setId(UUID.randomUUID());
        session.setFile(file);
        session.setStorageUploadId("storage-upload-id");
        session.setChunkSize(CHUNK_SIZE);
        session.setTotalChunks(totalChunks);
        session.setUploadedChunks(0);
        session.setStatus(UploadStatus.INITIATED);
        session.setCreatedAt(LocalDateTime.now());
        return session;
    }

    private static UploadPart uploadPart(UploadSession session, int partNumber, String etag) {
        var part = new UploadPart();
        part.setId(UUID.randomUUID());
        part.setUploadSession(session);
        part.setPartNumber(partNumber);
        part.setEtag(etag);
        part.setSize(CHUNK_SIZE);
        part.setUploadedAt(LocalDateTime.now());
        return part;
    }
}
