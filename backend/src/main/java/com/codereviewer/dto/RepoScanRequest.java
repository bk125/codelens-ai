package com.codereviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RepoScanRequest {

    @NotBlank(message = "Repository URL is required")
    @Pattern(
        regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$",
        message = "Must be a valid GitHub repository URL, e.g. https://github.com/owner/repo"
    )
    private String repoUrl;

    // Optional — defaults to the repo's default branch if not provided
    private String branch;

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
}
