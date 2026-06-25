package com.codereviewer.util;

import com.codereviewer.dto.AiReviewResult;
import com.codereviewer.exception.AiResponseParsingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JsonParserUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonParserUtil.class);

    private static final Pattern JSON_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.DOTALL);
    private static final Pattern JSON_OBJECT_PATTERN =
            Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    @Autowired
    public JsonParserUtil(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiReviewResult parseAiReviewResponse(String rawResponse) {
        log.debug("Parsing AI review response of length: {}", rawResponse.length());
        String cleaned = stripMarkdown(rawResponse);

        try {
            AiReviewResult result = objectMapper.readValue(cleaned, AiReviewResult.class);
            validateAndCleanReview(result);
            return result;
        } catch (Exception e) {
            log.warn("Standard parse failed for review, attempting deep repair: {}", e.getMessage());
            return deepRepairReview(cleaned, rawResponse);
        }
    }

    private String stripMarkdown(String response) {
        String trimmed = response.trim();
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(trimmed);
        if (blockMatcher.find()) return blockMatcher.group(1).trim();

        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(trimmed);
        if (objectMatcher.find()) return objectMatcher.group().trim();

        return trimmed;
    }

    private String stripInnerCodeFences(String code) {
        if (code == null) return "";
        String trimmed = code.trim();
        Matcher fence = Pattern.compile("^```(?:\\w+)?\\s*\\n?([\\s\\S]*?)\\n?```\\s*$", Pattern.DOTALL).matcher(trimmed);
        if (fence.matches()) return fence.group(1).trim();

        if (trimmed.startsWith("```")) trimmed = trimmed.replaceFirst("^```\\w*\\s*", "");
        if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
        return trimmed;
    }

    private AiReviewResult deepRepairReview(String cleaned, String raw) {
        try {
            String repaired = tryRepairingJson(cleaned);
            JsonNode root = objectMapper.readTree(repaired);

            AiReviewResult result = new AiReviewResult();
            if (root.has("score")) result.setScore(root.get("score").asInt(0));
            if (root.has("language")) result.setLanguage(root.get("language").asText("unknown"));
            if (root.has("summary")) result.setSummary(root.get("summary").asText("Review completed."));
            if (root.has("optimized_code")) result.setOptimizedCode(root.get("optimized_code").asText(""));

            try {
                if (root.has("issues")) result.setIssues(objectMapper.convertValue(root.get("issues"),
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, com.codereviewer.dto.CodeIssue.class)));
                if (root.has("improvements")) result.setImprovements(objectMapper.convertValue(root.get("improvements"),
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
                if (root.has("best_practices")) result.setBestPractices(objectMapper.convertValue(root.get("best_practices"),
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class)));
            } catch (Exception e) {
                log.warn("Could not parse some list fields during deep repair");
            }

            validateAndCleanReview(result);
            return result;
        } catch (Exception e) {
            log.error("Deep repair failed for review: {}", e.getMessage());
            throw new AiResponseParsingException("Review repair failed: " + e.getMessage(), e);
        }
    }

    private String tryRepairingJson(String json) {
        String t = json.trim();
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"' && !escaped) {
                inString = !inString;
            }
            
            if (inString && (c == '\n' || c == '\r')) {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
            
            if (c == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        
        String result = sb.toString().trim();
        if (!result.endsWith("}")) {
            if (inString) result += "\"}";
            else result += "}";
        }
        return result;
    }

    private void validateAndCleanReview(AiReviewResult r) {
        if (r.getScore() < 0 || r.getScore() > 10) r.setScore(Math.max(0, Math.min(10, r.getScore())));
        if (r.getIssues() == null) r.setIssues(Collections.emptyList());
        if (r.getImprovements() == null) r.setImprovements(Collections.emptyList());
        if (r.getBestPractices() == null) r.setBestPractices(Collections.emptyList());
        if (r.getSummary() == null) r.setSummary("Review completed.");
        if (r.getLanguage() == null) r.setLanguage("unknown");
        r.setOptimizedCode(stripInnerCodeFences(r.getOptimizedCode()));
    }
}
