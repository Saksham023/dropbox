package com.example.dropbox.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dropbox.dto.CreateFolderRequest;
import com.example.dropbox.dto.CreateFolderResponse;
import com.example.dropbox.dto.FileListItemResponse;
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
}
