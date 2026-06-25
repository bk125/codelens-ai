package com.codereviewer.controller;

import com.codereviewer.dto.*;
import com.codereviewer.model.User;
import com.codereviewer.service.RepoScanService;
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
@RequestMapping("/api/v1/security-scan")
public class RepoScanController {

    private static final Logger log = LoggerFactory.getLogger(RepoScanController.class);
    private final RepoScanService scanService;

    @Autowired
    public RepoScanController(RepoScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<RepoScanStatusResponse>> startScan(
            @Valid @RequestBody RepoScanRequest request,
            @AuthenticationPrincipal User user) {
        log.info("POST /security-scan/start - repo: {}, user: {}", request.getRepoUrl(), user.getEmail());
        RepoScanStatusResponse status = scanService.startScan(request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Scan started", status));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RepoScanStatusResponse>> getStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(scanService.getStatus(id, user)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<RepoScanHistorySummary>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(scanService.getHistory(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteScan(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        scanService.deleteScan(id, user);
        return ResponseEntity.ok(ApiResponse.success("Scan deleted", null));
    }
}
