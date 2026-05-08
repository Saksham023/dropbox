package com.example.dropbox.service;

import java.util.List;
import java.util.UUID;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.FileListItemResponse;
import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;

public interface FileService {
    List<FileListItemResponse> listFiles(UUID ownerId, UUID parentId);

    CreateFolderResponse createFolder(CreateFolderRequest request);

    InitiateUploadResponse initiateUpload(InitiateUploadRequest request);
}
