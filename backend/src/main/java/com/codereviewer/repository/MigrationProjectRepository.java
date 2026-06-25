package com.codereviewer.repository;

import com.codereviewer.model.MigrationProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MigrationProjectRepository extends JpaRepository<MigrationProject, Long> {

    // Exclude resultZip blob from list queries for performance
    @Query("SELECT m FROM MigrationProject m WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<MigrationProject> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<MigrationProject> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
