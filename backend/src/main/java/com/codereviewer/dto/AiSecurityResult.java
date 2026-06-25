package com.codereviewer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Per-file AI security scan response shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSecurityResult {
    private List<SecurityFinding> vulnerabilities;

    public List<SecurityFinding> getVulnerabilities() { return vulnerabilities; }
    public void setVulnerabilities(List<SecurityFinding> v) { this.vulnerabilities = v; }
}
