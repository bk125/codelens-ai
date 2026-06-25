package com.codereviewer.service;

import com.codereviewer.dto.AiReviewResult;
import com.codereviewer.exception.AiResponseParsingException;
import com.codereviewer.util.JsonParserUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewService.class );

    private final RestTemplate restTemplate;
    private final JsonParserUtil jsonParserUtil;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.key}")
    private String apiKey;

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.api.model}")
    private String model;

    @Autowired
    public AiReviewService(RestTemplate restTemplate,
                           JsonParserUtil jsonParserUtil,
                           ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.jsonParserUtil = jsonParserUtil;
        this.objectMapper = objectMapper;
    }

    public AiReviewResult reviewCode(String code, String languageHint) {
        log.info("Sending code for AI review via Ollama (Gemma). Language hint: '{}', Code length: {}",
                languageHint, code.length());

        try {
            String requestBody = buildRequestBody(code, languageHint);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            String rawText = extractTextFromResponse(response.getBody());
            
            if (rawText == null || rawText.isBlank()) {
                throw new AiResponseParsingException("Ollama returned an empty response");
            }

            return jsonParserUtil.parseAiReviewResponse(rawText);

        } catch (AiResponseParsingException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Ollama API HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiResponseParsingException(
                    "Ollama API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Unexpected error during AI review", e);
            throw new AiResponseParsingException("AI service error: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String code, String languageHint) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1); 
        root.put("max_tokens", 8192);

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "You are an expert engineer. Respond ONLY with a valid JSON object. " +
                "CRITICAL: The 'optimized_code' MUST be the COMPLETE file content. " +
                "Do NOT use markdown blocks inside JSON values.");

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", buildPrompt(code, languageHint));

        return objectMapper.writeValueAsString(root);
    }

    private String extractTextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").get(0).path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrompt(String code, String languageHint) {
        String lang = (languageHint != null && !languageHint.isBlank()) ? languageHint : "auto-detect";
        return "Review this " + lang + " code. Output ONLY JSON.\n"
                + "Schema:\n"
                + "{\n"
                + "  \"score\": 0-10,\n"
                + "  \"language\": \"string\",\n"
                + "  \"summary\": \"string\",\n"
                + "  \"issues\": [{ \"line\": number, \"severity\": \"string\", \"problem\": \"string\" }],\n"
                + "  \"improvements\": [\"string\"],\n"
                + "  \"best_practices\": [\"string\"],\n"
                + "  \"optimized_code\": \"<COMPLETE full code here>\"\n"
                + "}\n\n"
                + "Code:\n" + code;
    }
}
