package com.codereviewer.service;

import com.codereviewer.dto.MigrationHistorySummary;
import com.codereviewer.dto.MigrationStatusResponse;
import com.codereviewer.exception.CodeReviewNotFoundException;
import com.codereviewer.model.MigrationProject;
import com.codereviewer.model.User;
import com.codereviewer.repository.MigrationProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.*;

@Service
public class CodeMigrationService {

    private static final Logger log = LoggerFactory.getLogger(CodeMigrationService.class);

    private final MigrationProjectRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MigrationProcessor migrationProcessor;

    @Autowired
    public CodeMigrationService(MigrationProjectRepository repository,
                                 RestTemplate restTemplate,
                                 ObjectMapper objectMapper,
                                MigrationProcessor migrationProcessor) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.migrationProcessor = migrationProcessor;
    }

    // ── Start Migration ───────────────────────────────────────────────────────

    @Transactional
    public MigrationStatusResponse startMigration(MultipartFile zipFile, String targetLanguage, User user) throws IOException {
        log.info("Starting migration for user: {}, target: {}, file: {}",
                user.getEmail(), targetLanguage, zipFile.getOriginalFilename());

        // Validate it's a zip
        String filename = zipFile.getOriginalFilename() != null
                ? zipFile.getOriginalFilename() : "project.zip";
        if (!filename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Only ZIP files are supported");
        }

        // Count code files in the zip first
        byte[] zipBytes = zipFile.getBytes();
        List<String> codeFiles = migrationProcessor.listCodeFiles(zipBytes);

        if (codeFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "No code files found in the ZIP.");
        }

        // Create DB record
        MigrationProject project = MigrationProject.builder()
                .user(user)
                .projectName(filename.replace(".zip", ""))
                .targetLanguage(targetLanguage)
                .status(MigrationProject.Status.PENDING)
                .totalFiles(codeFiles.size())
                .build();

        project = repository.save(project);

        // Start async processing — pass zip bytes directly
        migrationProcessor.processMigration(project.getId(), zipBytes, targetLanguage, codeFiles);

        return toStatus(project);
    }

    // ── Public Query Methods ──────────────────────────────────────────────────

    // ── Public Query Methods ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MigrationStatusResponse getStatus(Long id, User user) {
        MigrationProject p = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Migration not found: " + id));
        return toStatus(p);
    }

    @Transactional(readOnly = true)
    public List<MigrationHistorySummary> getHistory(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public byte[] getResultZip(Long id, User user) {
        MigrationProject p = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Migration not found: " + id));
        if (p.getStatus() != MigrationProject.Status.COMPLETED || p.getResultZip() == null) {
            throw new IllegalStateException("Migration result not ready yet");
        }
        return p.getResultZip();
    }

    @Transactional
    public void deleteMigration(Long id, User user) {
        repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Migration not found: " + id));
        repository.deleteByIdAndUserId(id, user.getId());
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private MigrationStatusResponse toStatus(MigrationProject p) {
        int percent = p.getTotalFiles() == 0 ? 0
                : (p.getProcessedFiles() * 100 / p.getTotalFiles());
        if (p.getStatus() == MigrationProject.Status.COMPLETED) percent = 100;

        return MigrationStatusResponse.builder()
                .id(p.getId())
                .projectName(p.getProjectName())
                .sourceLanguage(p.getSourceLanguage())
                .targetLanguage(p.getTargetLanguage())
                .status(p.getStatus().name())
                .totalFiles(p.getTotalFiles())
                .processedFiles(p.getProcessedFiles())
                .progressPercent(percent)
                .errorMessage(p.getErrorMessage())
                .migrationPlan(p.getMigrationPlan())
                .fileSummaryJson(p.getFileSummaryJson())
                .downloadReady(p.getStatus() == MigrationProject.Status.COMPLETED
                               && p.getResultZip() != null)
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }

    private MigrationHistorySummary toSummary(MigrationProject p) {
        return MigrationHistorySummary.builder()
                .id(p.getId())
                .projectName(p.getProjectName())
                .sourceLanguage(p.getSourceLanguage())
                .targetLanguage(p.getTargetLanguage())
                .status(p.getStatus().name())
                .totalFiles(p.getTotalFiles())
                .processedFiles(p.getProcessedFiles())
                .createdAt(p.getCreatedAt())
                .completedAt(p.getCompletedAt())
                .build();
    }
}