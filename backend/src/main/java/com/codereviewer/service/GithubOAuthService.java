package com.codereviewer.service;

import com.codereviewer.exception.AiResponseParsingException;
import com.codereviewer.model.User;
import com.codereviewer.repository.UserRepository;
import com.codereviewer.security.AesEncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles GitHub OAuth (Authorization Code flow) used exclusively for the
 * Security Scan feature. Not connected to the app's primary login —
 * users authenticate normally first, then optionally connect GitHub
 * from the Security Scan page only.
 */
@Service
public class GithubOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuthService.class);
    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_API_URL = "https://api.github.com/user";

    private final UserRepository userRepository;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${app.cors.allowed-origins}")
    private String frontendBaseUrl;

    @Autowired
    public GithubOAuthService(UserRepository userRepository,
                               AesEncryptionUtil aesEncryptionUtil,
                               RestTemplate restTemplate,
                               ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.aesEncryptionUtil = aesEncryptionUtil;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Builds the GitHub authorize URL. The `state` parameter carries the
     * logged-in user's id (signed by being embedded in a short-lived value)
     * so the callback knows which user to attach the token to.
     */
    public String buildAuthorizeUrl(Long userId) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "repo read:user")
                .queryParam("state", userId.toString())
                .build()
                .toUriString();
    }

    /**
     * Exchanges the OAuth code for an access token, fetches the GitHub
     * username, encrypts and stores the token on the User row.
     * Returns the frontend redirect URL (success or failure) to send the
     * browser back to.
     */
    public String handleCallback(String code, String state) {
        try {
            Long userId = Long.parseLong(state);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user in OAuth state"));

            String accessToken = exchangeCodeForToken(code);
            String githubUsername = fetchGithubUsername(accessToken);

            user.setGithubAccessTokenEncrypted(aesEncryptionUtil.encrypt(accessToken));
            user.setGithubUsername(githubUsername);
            user.setGithubConnectedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("GitHub connected for user: {} (github: {})", user.getEmail(), githubUsername);
            return frontendBaseUrl + "/security-scan?github=connected";

        } catch (Exception e) {
            log.error("GitHub OAuth callback failed", e);
            return frontendBaseUrl + "/security-scan?github=error";
        }
    }

    public void disconnect(User user) {
        user.setGithubAccessTokenEncrypted(null);
        user.setGithubUsername(null);
        user.setGithubConnectedAt(null);
        userRepository.save(user);
    }

    /**
     * Returns the decrypted token for a user, or empty if not connected.
     * Used internally by the repo scanner — never exposed via any API response.
     */
    public Optional<String> getDecryptedToken(User user) {
        if (user.getGithubAccessTokenEncrypted() == null) return Optional.empty();
        return Optional.of(aesEncryptionUtil.decrypt(user.getGithubAccessTokenEncrypted()));
    }

    private String exchangeCodeForToken(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        var body = objectMapper.createObjectNode();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        ResponseEntity<String> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
                String.class);

        JsonNode json = objectMapper.readTree(response.getBody());
        if (json.has("error")) {
            throw new AiResponseParsingException("GitHub OAuth error: " + json.get("error_description").asText());
        }
        return json.get("access_token").asText();
    }

    private String fetchGithubUsername(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");

        ResponseEntity<String> response = restTemplate.exchange(
                USER_API_URL, HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        JsonNode json = objectMapper.readTree(response.getBody());
        return json.get("login").asText();
    }
}
