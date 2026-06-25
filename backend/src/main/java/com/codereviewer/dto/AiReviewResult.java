package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReviewResult {

    private int score;
    private String language;
    private String summary;
    private List<CodeIssue> issues;
    private List<String> improvements;

    @JsonProperty("best_practices")
    private List<String> bestPractices;

    @JsonProperty("optimized_code")
    private String optimizedCode;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<CodeIssue> getIssues() { return issues; }
    public void setIssues(List<CodeIssue> issues) { this.issues = issues; }

    public List<String> getImprovements() { return improvements; }
    public void setImprovements(List<String> improvements) { this.improvements = improvements; }

    public List<String> getBestPractices() { return bestPractices; }
    public void setBestPractices(List<String> bestPractices) { this.bestPractices = bestPractices; }

    public String getOptimizedCode() { return optimizedCode; }
    public void setOptimizedCode(String optimizedCode) { this.optimizedCode = optimizedCode; }
}
