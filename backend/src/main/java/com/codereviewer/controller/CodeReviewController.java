package com.codereviewer.controller;

import com.codereviewer.dto.*;
import com.codereviewer.model.User;
import com.codereviewer.service.CodeReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class CodeReviewController {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewController.class);

    private final CodeReviewService reviewService;

    @Autowired
    public CodeReviewController(CodeReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CodeReviewResponse>> submitReview(
            @Valid @RequestBody CodeReviewRequest request,
            @AuthenticationPrincipal User user) {
        log.info("POST /reviews - language: {}, user: {}", request.getLanguage(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review completed", reviewService.submitReview(request, user)));
    }

    @PostMapping("/{id}/re-review")
    public ResponseEntity<ApiResponse<CodeReviewResponse>> reReview(
            @PathVariable Long id,
            @Valid @RequestBody CodeReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Re-review completed", reviewService.reReview(id, user, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeReviewResponse>> getReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewById(id, user)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ReviewHistorySummary>>> getHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getHistoryByUser(user)));
    }

    @GetMapping("/history/paged")
    public ResponseEntity<ApiResponse<Page<ReviewHistorySummary>>> getHistoryPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getHistoryByUserPaged(user, page, size)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SessionStatsResponse>> getStats(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getStats(user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        reviewService.deleteReview(id, user);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}
