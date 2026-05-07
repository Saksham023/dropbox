package com.example.dropbox.service;

import java.util.List;
import java.util.UUID;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.FileListItemResponse;

public interface FileService {
    List<FileListItemResponse> listFiles(UUID ownerId, UUID parentId);

    CreateFolderResponse createFolder(CreateFolderRequest request);
}
