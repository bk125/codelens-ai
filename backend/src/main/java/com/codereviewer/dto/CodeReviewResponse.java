package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public class CodeReviewResponse {

    private Long id;
    private String sessionId;
    private String language;
    private String title;
    private String originalCode;
    private int score;
    private String summary;
    private List<CodeIssue> issues;
    private List<String> improvements;

    @JsonProperty("best_practices")
    private List<String> bestPractices;

    @JsonProperty("optimized_code")
    private String optimizedCode;

    private LocalDateTime createdAt;

    // Builder pattern (manual)
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CodeReviewResponse obj = new CodeReviewResponse();

        public Builder id(Long id)                         { obj.id = id; return this; }
        public Builder sessionId(String s)                 { obj.sessionId = s; return this; }
        public Builder language(String s)                  { obj.language = s; return this; }
        public Builder title(String s)                     { obj.title = s; return this; }
        public Builder originalCode(String s)              { obj.originalCode = s; return this; }
        public Builder score(int n)                        { obj.score = n; return this; }
        public Builder summary(String s)                   { obj.summary = s; return this; }
        public Builder issues(List<CodeIssue> l)           { obj.issues = l; return this; }
        public Builder improvements(List<String> l)        { obj.improvements = l; return this; }
        public Builder bestPractices(List<String> l)       { obj.bestPractices = l; return this; }
        public Builder optimizedCode(String s)             { obj.optimizedCode = s; return this; }
        public Builder createdAt(LocalDateTime t)          { obj.createdAt = t; return this; }
        public CodeReviewResponse build()                  { return obj; }
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getOriginalCode() { return originalCode; }
    public int getScore() { return score; }
    public String getSummary() { return summary; }
    public List<CodeIssue> getIssues() { return issues; }
    public List<String> getImprovements() { return improvements; }
    public List<String> getBestPractices() { return bestPractices; }
    public String getOptimizedCode() { return optimizedCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
