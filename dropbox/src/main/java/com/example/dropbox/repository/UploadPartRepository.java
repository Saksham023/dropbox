package com.example.dropbox.repository;

import com.example.dropbox.entity.UploadPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadPartRepository extends JpaRepository<UploadPart, UUID> {

    List<UploadPart> findByUploadSession_IdOrderByPartNumberAsc(UUID uploadSessionId);

    boolean existsByUploadSession_IdAndPartNumber(UUID uploadSessionId, Integer partNumber);
}
