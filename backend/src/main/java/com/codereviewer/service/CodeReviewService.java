package com.codereviewer.service;

import com.codereviewer.dto.*;
import com.codereviewer.exception.CodeReviewNotFoundException;
import com.codereviewer.model.CodeReview;
import com.codereviewer.model.User;
import com.codereviewer.repository.CodeReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final AiReviewService aiReviewService;
    private final CodeReviewRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public CodeReviewService(AiReviewService aiReviewService,
                              CodeReviewRepository repository,
                              ObjectMapper objectMapper) {
        this.aiReviewService = aiReviewService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public CodeReviewResponse submitReview(CodeReviewRequest request, User user) {
        // Language is optional — AI will auto-detect if not provided
        String languageHint = request.getLanguage();
        log.info("Processing review. Language hint: '{}', User: {}", languageHint, user.getEmail());

        AiReviewResult aiResult = aiReviewService.reviewCode(request.getCode(), languageHint);

        // Use AI-detected language for storage (not the hint)
        String detectedLanguage = aiResult.getLanguage() != null
                ? aiResult.getLanguage()
                : (languageHint != null ? languageHint : "unknown");

        String title = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : generateTitle(detectedLanguage, request.getCode());

        String reviewJson = serializeResult(aiResult);

        CodeReview saved = repository.save(
                CodeReview.builder()
                        .user(user)
                        .language(detectedLanguage)
                        .originalCode(request.getCode())
                        .reviewResult(reviewJson)
                        .score(aiResult.getScore())
                        .title(title)
                        .build()
        );

        log.info("Saved review id: {} | Language detected: {}", saved.getId(), detectedLanguage);
        return toResponse(saved, aiResult);
    }

    @Transactional(readOnly = true)
    public List<ReviewHistorySummary> getHistoryByUser(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toHistorySummary).toList();
    }

    @Transactional(readOnly = true)
    public Page<ReviewHistorySummary> getHistoryByUserPaged(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toHistorySummary);
    }

    @Transactional(readOnly = true)
    public CodeReviewResponse getReviewById(Long id, User user) {
        CodeReview review = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException(id));
        AiReviewResult aiResult = deserializeResult(review.getReviewResult());
        return toResponse(review, aiResult);
    }

    public CodeReviewResponse reReview(Long id, User user, CodeReviewRequest request) {
        repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException(id));
        return submitReview(request, user);
    }

    public void deleteReview(Long id, User user) {
        repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException(id));
        repository.deleteByIdAndUserId(id, user.getId());
    }

    @Transactional(readOnly = true)
    public SessionStatsResponse getStats(User user) {
        long total = repository.countByUserId(user.getId());
        Double avg = repository.averageScoreByUserId(user.getId());
        return SessionStatsResponse.builder()
                .totalReviews(total)
                .averageScore(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .sessionId(user.getName())
                .build();
    }

    private String generateTitle(String language, String code) {
        String firstLine = code.lines().findFirst().orElse("").trim();
        if (firstLine.length() > 60) firstLine = firstLine.substring(0, 57) + "...";
        String lang = language.substring(0, 1).toUpperCase() + language.substring(1);
        return lang + ": " + firstLine;
    }

    private String serializeResult(AiReviewResult result) {
        try { return objectMapper.writeValueAsString(result); }
        catch (Exception e) { log.error("Failed to serialize AI result", e); return "{}"; }
    }

    private AiReviewResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, AiReviewResult.class); }
        catch (Exception e) { log.error("Failed to deserialize result", e); return new AiReviewResult(); }
    }

    private CodeReviewResponse toResponse(CodeReview entity, AiReviewResult ai) {
        return CodeReviewResponse.builder()
                .id(entity.getId())
                .sessionId(entity.getUser().getName())
                .language(entity.getLanguage())
                .title(entity.getTitle())
                .originalCode(entity.getOriginalCode())
                .score(ai.getScore())
                .summary(ai.getSummary())
                .issues(ai.getIssues())
                .improvements(ai.getImprovements())
                .bestPractices(ai.getBestPractices())
                .optimizedCode(ai.getOptimizedCode())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ReviewHistorySummary toHistorySummary(CodeReview entity) {
        String summary = "";
        try {
            AiReviewResult r = objectMapper.readValue(entity.getReviewResult(), AiReviewResult.class);
            summary = r.getSummary() != null ? r.getSummary() : "";
        } catch (Exception ignored) {}

        return ReviewHistorySummary.builder()
                .id(entity.getId())
                .language(entity.getLanguage())
                .title(entity.getTitle())
                .score(entity.getScore() != null ? entity.getScore() : 0)
                .summary(summary)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
