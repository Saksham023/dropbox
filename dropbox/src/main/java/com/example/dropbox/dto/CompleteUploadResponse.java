package com.example.dropbox.dto;

import java.util.UUID;

import com.example.dropbox.enums.FileStatus;
import com.example.dropbox.enums.UploadStatus;

public record CompleteUploadResponse(
        UUID fileId,
        UUID uploadSessionId,
        FileStatus fileStatus,
        UploadStatus uploadStatus) {
}
