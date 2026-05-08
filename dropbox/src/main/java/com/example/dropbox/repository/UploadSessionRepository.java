package com.example.dropbox.repository;

import com.example.dropbox.entity.UploadSession;
import com.example.dropbox.enums.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    List<UploadSession> findByStatus(UploadStatus status);

    Optional<UploadSession> findByIdAndFile_IdAndFile_Owner_Id(UUID id, UUID fileId, UUID ownerId);

    Optional<UploadSession> findFirstByFile_IdAndFile_Owner_IdOrderByCreatedAtDesc(UUID fileId, UUID ownerId);
}
