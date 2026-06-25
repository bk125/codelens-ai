package com.codereviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CodeReviewRequest {

    @NotBlank(message = "Code cannot be empty")
    @Size(max = 50000, message = "Code must not exceed 50,000 characters")
    private String code;

    // Optional hint — frontend may pass extension-detected language
    // If null/blank, AI will auto-detect from the code itself
    private String language;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
