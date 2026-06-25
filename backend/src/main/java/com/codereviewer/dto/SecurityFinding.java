package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single vulnerability finding within one scanned file.
 * Maps directly to the AI's JSON output for the security prompt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityFinding {

    private String file;
    private String type;
    private String severity;     // CRITICAL | HIGH | MEDIUM | LOW
    private int line;
    private String cwe;          // e.g. "CWE-89"
    private String description;

    @JsonProperty("exploit_scenario")
    private String exploitScenario;

    private String fix;

    @JsonProperty("fixed_code_snippet")
    private String fixedCodeSnippet;

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    public String getCwe() { return cwe; }
    public void setCwe(String cwe) { this.cwe = cwe; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExploitScenario() { return exploitScenario; }
    public void setExploitScenario(String exploitScenario) { this.exploitScenario = exploitScenario; }
    public String getFix() { return fix; }
    public void setFix(String fix) { this.fix = fix; }
    public String getFixedCodeSnippet() { return fixedCodeSnippet; }
    public void setFixedCodeSnippet(String fixedCodeSnippet) { this.fixedCodeSnippet = fixedCodeSnippet; }
}
