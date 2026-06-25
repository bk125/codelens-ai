package com.codereviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ExplainRequest {

    @NotBlank(message = "Code cannot be empty")
    @Size(max = 50000, message = "Code must not exceed 50,000 characters")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
