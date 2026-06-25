package com.codereviewer.dto;

import java.time.LocalDateTime;

public class MigrationStatusResponse {
    private Long id;
    private String projectName;
    private String sourceLanguage;
    private String targetLanguage;
    private String status;
    private int totalFiles;
    private int processedFiles;
    private int progressPercent;
    private String errorMessage;
    private String migrationPlan;
    private String fileSummaryJson;
    private boolean downloadReady;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final MigrationStatusResponse o = new MigrationStatusResponse();
        public Builder id(Long v)                { o.id = v; return this; }
        public Builder projectName(String v)     { o.projectName = v; return this; }
        public Builder sourceLanguage(String v)  { o.sourceLanguage = v; return this; }
        public Builder targetLanguage(String v)  { o.targetLanguage = v; return this; }
        public Builder status(String v)          { o.status = v; return this; }
        public Builder totalFiles(int v)         { o.totalFiles = v; return this; }
        public Builder processedFiles(int v)     { o.processedFiles = v; return this; }
        public Builder progressPercent(int v)    { o.progressPercent = v; return this; }
        public Builder errorMessage(String v)    { o.errorMessage = v; return this; }
        public Builder migrationPlan(String v)   { o.migrationPlan = v; return this; }
        public Builder fileSummaryJson(String v) { o.fileSummaryJson = v; return this; }
        public Builder downloadReady(boolean v)  { o.downloadReady = v; return this; }
        public Builder createdAt(LocalDateTime v){ o.createdAt = v; return this; }
        public Builder completedAt(LocalDateTime v){ o.completedAt = v; return this; }
        public MigrationStatusResponse build()   { return o; }
    }

    public Long getId() { return id; }
    public String getProjectName() { return projectName; }
    public String getSourceLanguage() { return sourceLanguage; }
    public String getTargetLanguage() { return targetLanguage; }
    public String getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public int getProgressPercent() { return progressPercent; }
    public String getErrorMessage() { return errorMessage; }
    public String getMigrationPlan() { return migrationPlan; }
    public String getFileSummaryJson() { return fileSummaryJson; }
    public boolean isDownloadReady() { return downloadReady; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
