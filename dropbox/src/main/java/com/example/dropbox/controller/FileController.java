package com.example.dropbox.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import com.example.dropbox.service.FileService;

@RestController
@RequestMapping("/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public List<FileListItemResponse> listFiles(
            @RequestParam UUID ownerId,
            @RequestParam(required = false) UUID parentId) {
        return fileService.listFiles(ownerId, parentId);
    }

    @PostMapping("/folders")
    public CreateFolderResponse createFolder(@RequestBody CreateFolderRequest request) {
        return fileService.createFolder(request);
    }

    @PostMapping
    public InitiateUploadResponse initiateUpload(@RequestBody InitiateUploadRequest request) {
        return fileService.initiateUpload(request);
    }

    @PostMapping("/{fileId}/upload-session/parts")
    public RecordUploadPartResponse recordUploadPart(
            @PathVariable UUID fileId,
            @RequestBody RecordUploadPartRequest request) {
        return fileService.recordUploadPart(fileId, request);
    }

    @GetMapping("/{fileId}/upload-session")
    public UploadSessionResponse getUploadSession(
            @PathVariable UUID fileId,
            @RequestParam UUID ownerId) {
        return fileService.getUploadSession(fileId, ownerId);
    }

    @PostMapping("/{fileId}/upload-session/complete")
    public CompleteUploadResponse completeUpload(
            @PathVariable UUID fileId,
            @RequestBody UploadSessionActionRequest request) {
        return fileService.completeUpload(fileId, request);
    }

    @PostMapping("/{fileId}/upload-session/abort")
    public CompleteUploadResponse abortUpload(
            @PathVariable UUID fileId,
            @RequestBody UploadSessionActionRequest request) {
        return fileService.abortUpload(fileId, request);
    }
}
