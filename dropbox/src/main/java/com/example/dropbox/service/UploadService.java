package com.example.dropbox.service;

import com.example.dropbox.dto.InitiateUploadRequest;
import com.example.dropbox.dto.InitiateUploadResponse;

public interface UploadService {

    InitiateUploadResponse initiateUpload(InitiateUploadRequest request);
}
