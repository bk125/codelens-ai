package com.codereviewer.repository;

import com.codereviewer.model.CodeReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {

    List<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<CodeReview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<CodeReview> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT COUNT(r) FROM CodeReview r WHERE r.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.score) FROM CodeReview r WHERE r.user.id = :userId")
    Double averageScoreByUserId(@Param("userId") Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
