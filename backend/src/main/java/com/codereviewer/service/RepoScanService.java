package com.codereviewer.service;

import com.codereviewer.dto.*;
import com.codereviewer.exception.CodeReviewNotFoundException;
import com.codereviewer.model.RepoScan;
import com.codereviewer.model.User;
import com.codereviewer.repository.RepoScanRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RepoScanService {

    private static final Logger log = LoggerFactory.getLogger(RepoScanService.class);

    private final RepoScanRepository repository;
    private final RepoScanProcessor processor;
    private final GithubOAuthService githubOAuthService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RepoScanService(RepoScanRepository repository,
                            RepoScanProcessor processor,
                            GithubOAuthService githubOAuthService,
                            ObjectMapper objectMapper) {
        this.repository = repository;
        this.processor = processor;
        this.githubOAuthService = githubOAuthService;
        this.objectMapper = objectMapper;
    }

    public RepoScanStatusResponse startScan(RepoScanRequest request, User user) {
        // Validate GitHub is connected
        Optional<String> token = githubOAuthService.getDecryptedToken(user);
        if (token.isEmpty()) {
            throw new IllegalStateException("GitHub account not connected. Please connect GitHub first.");
        }

        String repoUrl = request.getRepoUrl().trim();
        if (repoUrl.endsWith("/")) repoUrl = repoUrl.substring(0, repoUrl.length() - 1);

        // Parse owner/repo from URL
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) throw new IllegalArgumentException("Invalid GitHub URL");
        String repoName = parts[0] + "/" + parts[1];

        log.info("Starting repo scan for {} by user: {}", repoName, user.getEmail());

        RepoScan scan = RepoScan.builder()
                .user(user)
                .repoUrl(repoUrl)
                .repoName(repoName)
                .branch(request.getBranch())
                .status(RepoScan.Status.PENDING)
                .build();

        scan = repository.save(scan);

        // Kick off async processing
        processor.processScan(scan.getId(), repoName, request.getBranch(), token.get());

        return toStatus(scan, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public RepoScanStatusResponse getStatus(Long id, User user) {
        RepoScan scan = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Scan not found: " + id));
        return toStatus(scan, parseFindings(scan.getFindingsJson()));
    }

    @Transactional(readOnly = true)
    public List<RepoScanHistorySummary> getHistory(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toSummary).toList();
    }

    public void deleteScan(Long id, User user) {
        repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Scan not found: " + id));
        repository.deleteByIdAndUserId(id, user.getId());
    }

    private RepoScanStatusResponse toStatus(RepoScan s, List<SecurityFinding> findings) {
        int percent = 0;
        if (s.getStatus() == RepoScan.Status.COMPLETED) {
            percent = 100;
        } else if (s.getTotalFiles() > 0) {
            percent = (s.getProcessedFiles() * 100) / s.getTotalFiles();
        }

        return RepoScanStatusResponse.builder()
                .id(s.getId())
                .repoUrl(s.getRepoUrl())
                .repoName(s.getRepoName())
                .branch(s.getBranch())
                .status(s.getStatus().name())
                .totalFiles(s.getTotalFiles())
                .processedFiles(s.getProcessedFiles())
                .progressPercent(percent)
                .currentFile(s.getCurrentFile())
                .overallRiskScore(s.getOverallRiskScore())
                .criticalCount(s.getCriticalCount())
                .highCount(s.getHighCount())
                .mediumCount(s.getMediumCount())
                .lowCount(s.getLowCount())
                .summary(s.getSummary())
                .findings(findings)
                .errorMessage(s.getErrorMessage())
                .reportReady(s.getStatus() == RepoScan.Status.COMPLETED)
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }

    private RepoScanHistorySummary toSummary(RepoScan s) {
        return RepoScanHistorySummary.builder()
                .id(s.getId())
                .repoUrl(s.getRepoUrl())
                .repoName(s.getRepoName())
                .status(s.getStatus().name())
                .totalFiles(s.getTotalFiles())
                .overallRiskScore(s.getOverallRiskScore())
                .criticalCount(s.getCriticalCount())
                .highCount(s.getHighCount())
                .createdAt(s.getCreatedAt())
                .completedAt(s.getCompletedAt())
                .build();
    }

    private List<SecurityFinding> parseFindings(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<SecurityFinding>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse findings JSON", e);
            return Collections.emptyList();
        }
    }
}
