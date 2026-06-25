package com.codereviewer.repository;

import com.codereviewer.model.RepoScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoScanRepository extends JpaRepository<RepoScan, Long> {
    List<RepoScan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<RepoScan> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
