package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiExplanationResult {

    private String language;
    private String overview;

    @JsonProperty("how_it_works")
    private String howItWorks;

    private List<FunctionBreakdown> functions;
    private String complexity;

    @JsonProperty("use_cases")
    private List<String> useCases;

    @JsonProperty("key_concepts")
    private List<String> keyConcepts;

    public String getLanguage() { return language; }
    public void setLanguage(String s) { this.language = s; }
    public String getOverview() { return overview; }
    public void setOverview(String s) { this.overview = s; }
    public String getHowItWorks() { return howItWorks; }
    public void setHowItWorks(String s) { this.howItWorks = s; }
    public List<FunctionBreakdown> getFunctions() { return functions; }
    public void setFunctions(List<FunctionBreakdown> l) { this.functions = l; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String s) { this.complexity = s; }
    public List<String> getUseCases() { return useCases; }
    public void setUseCases(List<String> l) { this.useCases = l; }
    public List<String> getKeyConcepts() { return keyConcepts; }
    public void setKeyConcepts(List<String> l) { this.keyConcepts = l; }
}
