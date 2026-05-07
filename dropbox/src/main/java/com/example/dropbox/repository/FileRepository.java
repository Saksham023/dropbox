package com.example.dropbox.repository;

import com.example.dropbox.entity.File;
import com.example.dropbox.entity.User;
import com.example.dropbox.enums.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {

    List<File> findByOwner(User owner);

    List<File> findByParent_Id(UUID parentId);

    List<File> findByStatus(FileStatus status);
}
