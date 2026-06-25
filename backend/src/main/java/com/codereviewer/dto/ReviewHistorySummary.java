package com.codereviewer.dto;

import java.time.LocalDateTime;

public class ReviewHistorySummary {

    private Long id;
    private String language;
    private String title;
    private int score;
    private String summary;
    private LocalDateTime createdAt;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ReviewHistorySummary obj = new ReviewHistorySummary();

        public Builder id(Long id)              { obj.id = id; return this; }
        public Builder language(String s)       { obj.language = s; return this; }
        public Builder title(String s)          { obj.title = s; return this; }
        public Builder score(int n)             { obj.score = n; return this; }
        public Builder summary(String s)        { obj.summary = s; return this; }
        public Builder createdAt(LocalDateTime t){ obj.createdAt = t; return this; }
        public ReviewHistorySummary build()     { return obj; }
    }

    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    public int getScore() { return score; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
