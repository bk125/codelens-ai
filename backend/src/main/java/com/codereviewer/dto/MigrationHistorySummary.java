package com.codereviewer.dto;

import java.time.LocalDateTime;

public class MigrationHistorySummary {
    private Long id;
    private String projectName;
    private String sourceLanguage;
    private String targetLanguage;
    private String status;
    private int totalFiles;
    private int processedFiles;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final MigrationHistorySummary o = new MigrationHistorySummary();
        public Builder id(Long v)               { o.id = v; return this; }
        public Builder projectName(String v)    { o.projectName = v; return this; }
        public Builder sourceLanguage(String v) { o.sourceLanguage = v; return this; }
        public Builder targetLanguage(String v) { o.targetLanguage = v; return this; }
        public Builder status(String v)         { o.status = v; return this; }
        public Builder totalFiles(int v)        { o.totalFiles = v; return this; }
        public Builder processedFiles(int v)    { o.processedFiles = v; return this; }
        public Builder createdAt(LocalDateTime v){ o.createdAt = v; return this; }
        public Builder completedAt(LocalDateTime v){ o.completedAt = v; return this; }
        public MigrationHistorySummary build()  { return o; }
    }

    public Long getId() { return id; }
    public String getProjectName() { return projectName; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public String getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
