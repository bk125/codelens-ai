package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeIssue {

    private int line;
    private String severity;
    private String problem;

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
}
