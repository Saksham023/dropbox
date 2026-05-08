package com.example.dropbox.service;

import java.util.List;
import java.util.UUID;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.CompleteUploadResponse;
import com.example.dropbox.dto.FileListItemResponse;
import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;
import com.example.dropbox.dto.RecordUploadPartRequest;
import com.example.dropbox.dto.RecordUploadPartResponse;
import com.example.dropbox.dto.UploadSessionActionRequest;
import com.example.dropbox.dto.UploadSessionResponse;

public interface FileService {
    List<FileListItemResponse> listFiles(UUID ownerId, UUID parentId);

    CreateFolderResponse createFolder(CreateFolderRequest request);

    InitiateUploadResponse initiateUpload(InitiateUploadRequest request);

    RecordUploadPartResponse recordUploadPart(UUID fileId, RecordUploadPartRequest request);

    UploadSessionResponse getUploadSession(UUID fileId, UUID ownerId);

    CompleteUploadResponse completeUpload(UUID fileId, UploadSessionActionRequest request);

    CompleteUploadResponse abortUpload(UUID fileId, UploadSessionActionRequest request);
}
