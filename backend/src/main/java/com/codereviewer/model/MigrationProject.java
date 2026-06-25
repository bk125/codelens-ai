package com.codereviewer.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "migration_projects")
public class MigrationProject {

    public enum Status {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "source_language", length = 50)
    private String sourceLanguage;

    @Column(name = "target_language", nullable = false, length = 50)
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "processed_files")
    private int processedFiles;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Stores the output ZIP as bytes in DB (for small projects)
    // For production you'd use S3/GCS — for now DB is fine
    @Column(name = "result_zip", columnDefinition = "LONGBLOB")
    private byte[] resultZip;

    @Column(name = "migration_plan", columnDefinition = "LONGTEXT")
    private String migrationPlan;

    @Column(name = "file_summary_json", columnDefinition = "LONGTEXT")
    private String fileSummaryJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public MigrationProject() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final MigrationProject o = new MigrationProject();
        public Builder user(User u)             { o.user = u; return this; }
        public Builder projectName(String s)    { o.projectName = s; return this; }
        public Builder sourceLanguage(String s) { o.sourceLanguage = s; return this; }
        public Builder targetLanguage(String s) { o.targetLanguage = s; return this; }
        public Builder status(Status s)         { o.status = s; return this; }
        public Builder totalFiles(int n)        { o.totalFiles = n; return this; }
        public MigrationProject build()         { return o; }
    }

    // Getters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getProjectName() { return projectName; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public Status getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public String getErrorMessage() { return errorMessage; }
    public byte[] getResultZip() { return resultZip; }
    public String getMigrationPlan() { return migrationPlan; }
    public String getFileSummaryJson() { return fileSummaryJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    // Setters
    public void setStatus(Status s) { this.status = s; }
    public void setProcessedFiles(int n) { this.processedFiles = n; }
    public void setErrorMessage(String s) { this.errorMessage = s; }
    public void setResultZip(byte[] b) { this.resultZip = b; }
    public void setMigrationPlan(String s) { this.migrationPlan = s; }
    public void setFileSummaryJson(String s) { this.fileSummaryJson = s; }
    public void setSourceLanguage(String s) { this.sourceLanguage = s; }
    public void setTotalFiles(int n) { this.totalFiles = n; }
    public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }
}
