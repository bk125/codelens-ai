package com.codereviewer.dto;

import java.time.LocalDateTime;

public class RepoScanHistorySummary {
    private Long id;
    private String repoUrl;
    private String repoName;
    private String status;
    private int totalFiles;
    private Double overallRiskScore;
    private int criticalCount;
    private int highCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final RepoScanHistorySummary o = new RepoScanHistorySummary();
        public Builder id(Long v)                { o.id = v; return this; }
        public Builder repoUrl(String v)         { o.repoUrl = v; return this; }
        public Builder repoName(String v)        { o.repoName = v; return this; }
        public Builder status(String v)          { o.status = v; return this; }
        public Builder totalFiles(int v)         { o.totalFiles = v; return this; }
        public Builder overallRiskScore(Double v){ o.overallRiskScore = v; return this; }
        public Builder criticalCount(int v)      { o.criticalCount = v; return this; }
        public Builder highCount(int v)          { o.highCount = v; return this; }
        public Builder createdAt(LocalDateTime v){ o.createdAt = v; return this; }
        public Builder completedAt(LocalDateTime v){ o.completedAt = v; return this; }
        public RepoScanHistorySummary build()    { return o; }
    }

    public Long getId() { return id; }
    public String getRepoUrl() { return repoUrl; }
    public String getRepoName() { return repoName; }
    public String getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public Double getOverallRiskScore() { return overallRiskScore; }
    public int getCriticalCount() { return criticalCount; }
    public int getHighCount() { return highCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
