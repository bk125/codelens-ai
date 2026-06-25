package com.codereviewer.controller;

import com.codereviewer.dto.ApiResponse;
import com.codereviewer.dto.GithubStatusResponse;
import com.codereviewer.model.User;
import com.codereviewer.service.GithubOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/github")
public class GithubOAuthController {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuthController.class);
    private final GithubOAuthService oauthService;

    @Autowired
    public GithubOAuthController(GithubOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    /**
     * Returns the GitHub OAuth authorize URL.
     * Frontend redirects the browser to this URL to start the flow.
     */
    @GetMapping("/authorize")
    public ResponseEntity<ApiResponse<String>> getAuthorizeUrl(
            @AuthenticationPrincipal User user) {
        String url = oauthService.buildAuthorizeUrl(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Authorize URL", url));
    }

    /**
     * GitHub redirects here after the user approves the OAuth prompt.
     * We exchange the code for a token, store it, then redirect back to the frontend.
     * This endpoint is public (no JWT needed) because GitHub calls it directly.
     */
    @GetMapping("/callback")
    public void callback(@RequestParam String code,
                         @RequestParam String state,
                         HttpServletResponse response) throws IOException {
        log.info("GitHub OAuth callback received. State (userId): {}", state);
        String redirectUrl = oauthService.handleCallback(code, state);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Returns whether the current user has GitHub connected.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<GithubStatusResponse>> getStatus(
            @AuthenticationPrincipal User user) {
        GithubStatusResponse status = GithubStatusResponse.builder()
                .connected(user.isGithubConnected())
                .githubUsername(user.getGithubUsername())
                .build();
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * Disconnects GitHub — clears the stored token from the user row.
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnect(
            @AuthenticationPrincipal User user) {
        oauthService.disconnect(user);
        return ResponseEntity.ok(ApiResponse.success("GitHub disconnected", null));
    }
}
