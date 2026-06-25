package com.codereviewer.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_explanations")
public class CodeExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "language", length = 50)
    private String language;

    @Column(name = "original_code", columnDefinition = "LONGTEXT", nullable = false)
    private String originalCode;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "how_it_works", columnDefinition = "TEXT")
    private String howItWorks;

    @Column(name = "functions_json", columnDefinition = "LONGTEXT")
    private String functionsJson; // JSON array of function breakdowns

    @Column(name = "complexity", length = 500)
    private String complexity;

    @Column(name = "use_cases_json", columnDefinition = "TEXT")
    private String useCasesJson; // JSON array

    @Column(name = "key_concepts_json", columnDefinition = "TEXT")
    private String keyConceptsJson; // JSON array

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CodeExplanation() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CodeExplanation obj = new CodeExplanation();
        public Builder user(User u)              { obj.user = u; return this; }
        public Builder language(String s)        { obj.language = s; return this; }
        public Builder originalCode(String s)    { obj.originalCode = s; return this; }
        public Builder overview(String s)        { obj.overview = s; return this; }
        public Builder howItWorks(String s)      { obj.howItWorks = s; return this; }
        public Builder functionsJson(String s)   { obj.functionsJson = s; return this; }
        public Builder complexity(String s)      { obj.complexity = s; return this; }
        public Builder useCasesJson(String s)    { obj.useCasesJson = s; return this; }
        public Builder keyConceptsJson(String s) { obj.keyConceptsJson = s; return this; }
        public CodeExplanation build()           { return obj; }
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getLanguage() { return language; }
    public String getOriginalCode() { return originalCode; }
    public String getOverview() { return overview; }
    public String getHowItWorks() { return howItWorks; }
    public String getFunctionsJson() { return functionsJson; }
    public String getComplexity() { return complexity; }
    public String getUseCasesJson() { return useCasesJson; }
    public String getKeyConceptsJson() { return keyConceptsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
