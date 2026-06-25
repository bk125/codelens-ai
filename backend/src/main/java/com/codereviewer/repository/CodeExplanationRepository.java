package com.codereviewer.repository;

import com.codereviewer.model.CodeExplanation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeExplanationRepository extends JpaRepository<CodeExplanation, Long> {
    List<CodeExplanation> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CodeExplanation> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
