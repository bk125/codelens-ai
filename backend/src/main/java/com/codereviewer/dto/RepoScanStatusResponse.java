package com.codereviewer.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RepoScanStatusResponse {
    private Long id;
    private String repoUrl;
    private String repoName;
    private String branch;
    private String status;
    private int totalFiles;
    private int processedFiles;
    private int progressPercent;
    private String currentFile;
    private Double overallRiskScore;
    private int criticalCount;
    private int highCount;
    private int mediumCount;
    private int lowCount;
    private String summary;
    private List<SecurityFinding> findings;
    private String errorMessage;
    private boolean reportReady;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RepoScanStatusResponse o = new RepoScanStatusResponse();
        public Builder id(Long v)                  { o.id = v; return this; }
        public Builder repoUrl(String v)            { o.repoUrl = v; return this; }
        public Builder repoName(String v)           { o.repoName = v; return this; }
        public Builder branch(String v)             { o.branch = v; return this; }
        public Builder status(String v)             { o.status = v; return this; }
        public Builder totalFiles(int v)            { o.totalFiles = v; return this; }
        public Builder processedFiles(int v)        { o.processedFiles = v; return this; }
        public Builder progressPercent(int v)       { o.progressPercent = v; return this; }
        public Builder currentFile(String v)        { o.currentFile = v; return this; }
        public Builder overallRiskScore(Double v)   { o.overallRiskScore = v; return this; }
        public Builder criticalCount(int v)         { o.criticalCount = v; return this; }
        public Builder highCount(int v)             { o.highCount = v; return this; }
        public Builder mediumCount(int v)           { o.mediumCount = v; return this; }
        public Builder lowCount(int v)              { o.lowCount = v; return this; }
        public Builder summary(String v)            { o.summary = v; return this; }
        public Builder findings(List<SecurityFinding> v) { o.findings = v; return this; }
        public Builder errorMessage(String v)       { o.errorMessage = v; return this; }
        public Builder reportReady(boolean v)       { o.reportReady = v; return this; }
        public Builder createdAt(LocalDateTime v)   { o.createdAt = v; return this; }
        public Builder completedAt(LocalDateTime v) { o.completedAt = v; return this; }
        public RepoScanStatusResponse build()       { return o; }
    }

    public Long getId() { return id; }
    public String getRepoUrl() { return repoUrl; }
    public String getRepoName() { return repoName; }
    public String getBranch() { return branch; }
    public String getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public int getProgressPercent() { return progressPercent; }
    public String getCurrentFile() { return currentFile; }
    public Double getOverallRiskScore() { return overallRiskScore; }
    public int getCriticalCount() { return criticalCount; }
    public int getHighCount() { return highCount; }
    public int getMediumCount() { return mediumCount; }
    public int getLowCount() { return lowCount; }
    public String getSummary() { return summary; }
    public List<SecurityFinding> getFindings() { return findings; }
    public String getErrorMessage() { return errorMessage; }
    public boolean isReportReady() { return reportReady; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
