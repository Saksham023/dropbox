package com.example.dropbox.repository;

import com.example.dropbox.entity.UploadSession;
import com.example.dropbox.enums.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    List<UploadSession> findByStatus(UploadStatus status);
}
