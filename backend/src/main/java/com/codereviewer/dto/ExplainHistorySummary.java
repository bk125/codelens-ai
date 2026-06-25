package com.codereviewer.dto;

import java.time.LocalDateTime;

public class ExplainHistorySummary {
    private Long id;
    private String language;
    private String overview;
    private LocalDateTime createdAt;

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final ExplainHistorySummary obj = new ExplainHistorySummary();
        public Builder id(Long id)             { obj.id = id; return this; }
        public Builder language(String s)      { obj.language = s; return this; }
        public Builder overview(String s)      { obj.overview = s; return this; }
        public Builder createdAt(LocalDateTime t){ obj.createdAt = t; return this; }
        public ExplainHistorySummary build()   { return obj; }
    }
    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public String getOverview() { return overview; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
