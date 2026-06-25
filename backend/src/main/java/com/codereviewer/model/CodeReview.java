package com.codereviewer.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_reviews")
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "language", nullable = false, length = 20)
    private String language;

    @Column(name = "original_code", columnDefinition = "LONGTEXT", nullable = false)
    private String originalCode;

    @Column(name = "review_result", columnDefinition = "LONGTEXT", nullable = false)
    private String reviewResult;

    @Column(name = "score")
    private Integer score;

    @Column(name = "title", length = 200)
    private String title;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodeReview() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CodeReview obj = new CodeReview();
        public Builder user(User u)           { obj.user = u; return this; }
        public Builder language(String s)     { obj.language = s; return this; }
        public Builder originalCode(String s) { obj.originalCode = s; return this; }
        public Builder reviewResult(String s) { obj.reviewResult = s; return this; }
        public Builder score(Integer n)       { obj.score = n; return this; }
        public Builder title(String s)        { obj.title = s; return this; }
        public CodeReview build()             { return obj; }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getLanguage() { return language; }
    public String getOriginalCode() { return originalCode; }
    public String getReviewResult() { return reviewResult; }
    public Integer getScore() { return score; }
    public String getTitle() { return title; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setReviewResult(String s) { this.reviewResult = s; }
    public void setScore(Integer n) { this.score = n; }
    public void setTitle(String s) { this.title = s; }
}
