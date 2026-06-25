package com.codereviewer.controller;

import com.codereviewer.dto.*;
import com.codereviewer.model.User;
import com.codereviewer.service.CodeMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/migration")
public class CodeMigrationController {

    private static final Logger log = LoggerFactory.getLogger(CodeMigrationController.class);
    private final CodeMigrationService migrationService;

    @Autowired
    public CodeMigrationController(CodeMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    /**
     * Upload ZIP and start migration
     */
    @PostMapping(value = "/start", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MigrationStatusResponse>> startMigration(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetLanguage") String targetLanguage,
            @AuthenticationPrincipal User user) throws IOException {

        log.info("POST /migration/start - file: {}, target: {}, user: {}",
                file.getOriginalFilename(), targetLanguage, user.getEmail());

        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File size exceeds 50MB limit"));
        }

        MigrationStatusResponse status = migrationService.startMigration(file, targetLanguage, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Migration started", status));
    }

    /**
     * Poll migration status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MigrationStatusResponse>> getStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(migrationService.getStatus(id, user)));
    }

    /**
     * Download the migrated ZIP
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadResult(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("GET /migration/{}/download - user: {}", id, user.getEmail());
        byte[] zipBytes = migrationService.getResultZip(id, user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("migrated-project-" + id + ".zip")
                        .build());
        headers.setContentLength(zipBytes.length);

        return ResponseEntity.ok().headers(headers).body(zipBytes);
    }

    /**
     * Get all past migrations for current user
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MigrationHistorySummary>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(migrationService.getHistory(user)));
    }

    /**
     * Delete a migration
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        migrationService.deleteMigration(id, user);
        return ResponseEntity.ok(ApiResponse.success("Migration deleted", null));
    }
}
