package com.codereviewer.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "repo_scans")
public class RepoScan {

    public enum Status {
        PENDING, FETCHING, SCANNING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Column(name = "repo_name", length = 200)
    private String repoName;

    @Column(name = "branch", length = 100)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "processed_files")
    private int processedFiles;

    @Column(name = "current_file", length = 500)
    private String currentFile;

    @Column(name = "overall_risk_score")
    private Double overallRiskScore;

    @Column(name = "critical_count")
    private int criticalCount;

    @Column(name = "high_count")
    private int highCount;

    @Column(name = "medium_count")
    private int mediumCount;

    @Column(name = "low_count")
    private int lowCount;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "findings_json", columnDefinition = "LONGTEXT")
    private String findingsJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public RepoScan() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final RepoScan o = new RepoScan();
        public Builder user(User u)        { o.user = u; return this; }
        public Builder repoUrl(String s)   { o.repoUrl = s; return this; }
        public Builder repoName(String s)  { o.repoName = s; return this; }
        public Builder branch(String s)    { o.branch = s; return this; }
        public Builder status(Status s)    { o.status = s; return this; }
        public Builder totalFiles(int n)   { o.totalFiles = n; return this; }
        public RepoScan build()            { return o; }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRepoUrl() { return repoUrl; }
    public String getRepoName() { return repoName; }
    public String getBranch() { return branch; }
    public Status getStatus() { return status; }
    public int getTotalFiles() { return totalFiles; }
    public int getProcessedFiles() { return processedFiles; }
    public String getCurrentFile() { return currentFile; }
    public Double getOverallRiskScore() { return overallRiskScore; }
    public int getCriticalCount() { return criticalCount; }
    public int getHighCount() { return highCount; }
    public int getMediumCount() { return mediumCount; }
    public int getLowCount() { return lowCount; }
    public String getSummary() { return summary; }
    public String getFindingsJson() { return findingsJson; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setBranch(String s) { this.branch = s; }
    public void setStatus(Status s) { this.status = s; }
    public void setTotalFiles(int n) { this.totalFiles = n; }
    public void setProcessedFiles(int n) { this.processedFiles = n; }
    public void setCurrentFile(String s) { this.currentFile = s; }
    public void setOverallRiskScore(Double d) { this.overallRiskScore = d; }
    public void setCriticalCount(int n) { this.criticalCount = n; }
    public void setHighCount(int n) { this.highCount = n; }
    public void setMediumCount(int n) { this.mediumCount = n; }
    public void setLowCount(int n) { this.lowCount = n; }
    public void setSummary(String s) { this.summary = s; }
    public void setFindingsJson(String s) { this.findingsJson = s; }
    public void setErrorMessage(String s) { this.errorMessage = s; }
    public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }
}
