package com.codereviewer.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExplainResponse {
    private Long id;
    private String language;
    private String originalCode;
    private String overview;
    private String howItWorks;
    private List<FunctionBreakdown> functions;
    private String complexity;
    private List<String> useCases;
    private List<String> keyConcepts;
    private LocalDateTime createdAt;

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ExplainResponse obj = new ExplainResponse();
        public Builder id(Long id)                        { obj.id = id; return this; }
        public Builder language(String s)                 { obj.language = s; return this; }
        public Builder originalCode(String s)             { obj.originalCode = s; return this; }
        public Builder overview(String s)                 { obj.overview = s; return this; }
        public Builder howItWorks(String s)               { obj.howItWorks = s; return this; }
        public Builder functions(List<FunctionBreakdown> l) { obj.functions = l; return this; }
        public Builder complexity(String s)               { obj.complexity = s; return this; }
        public Builder useCases(List<String> l)           { obj.useCases = l; return this; }
        public Builder keyConcepts(List<String> l)        { obj.keyConcepts = l; return this; }
        public Builder createdAt(LocalDateTime t)         { obj.createdAt = t; return this; }
        public ExplainResponse build()                    { return obj; }
    }

    public Long getId() { return id; }
    public String getLanguage() { return language; }
    public String getOriginalCode() { return originalCode; }
    public String getOverview() { return overview; }
    public String getHowItWorks() { return howItWorks; }
    public List<FunctionBreakdown> getFunctions() { return functions; }
    public String getComplexity() { return complexity; }
    public List<String> getUseCases() { return useCases; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
