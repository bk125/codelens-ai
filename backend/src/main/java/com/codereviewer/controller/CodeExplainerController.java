package com.codereviewer.controller;

import com.codereviewer.dto.*;
import com.codereviewer.model.User;
import com.codereviewer.service.CodeExplainerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/explainer")
public class CodeExplainerController {

    private static final Logger log = LoggerFactory.getLogger(CodeExplainerController.class);
    private final CodeExplainerService explainerService;

    @Autowired
    public CodeExplainerController(CodeExplainerService explainerService) {
        this.explainerService = explainerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExplainResponse>> explain(
            @Valid @RequestBody ExplainRequest request,
            @AuthenticationPrincipal User user) {
        log.info("POST /explainer - user: {}, code length: {}", user.getEmail(), request.getCode().length());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Explanation ready", explainerService.explain(request, user)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ExplainHistorySummary>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(explainerService.getHistory(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExplainResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(explainerService.getById(id, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        explainerService.delete(id, user);
        return ResponseEntity.ok(ApiResponse.success("Explanation deleted", null));
    }
}
