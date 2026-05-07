package com.example.dropbox.repository;

import com.example.dropbox.entity.File;
import com.example.dropbox.entity.User;
import com.example.dropbox.enums.FileStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {

    List<File> findByOwner(User owner);

    List<File> findByParent_Id(UUID parentId);

    List<File> findByStatus(FileStatus status);

    java.util.Optional<File> findByIdAndOwner_Id(UUID id, UUID ownerId);

    @Query("""
            select f from File f
            where f.owner.id = :ownerId
              and f.status <> com.example.dropbox.enums.FileStatus.DELETED
              and (
                    (:parentId is null and f.parent is null)
                    or (f.parent.id = :parentId)
                  )
            order by f.createdAt desc
            """)
    List<File> findVisibleByOwnerAndParent(
            @Param("ownerId") UUID ownerId,
            @Param("parentId") UUID parentId);
}
