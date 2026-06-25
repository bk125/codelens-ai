package com.codereviewer.service;

import com.codereviewer.dto.*;
import com.codereviewer.exception.AiResponseParsingException;
import com.codereviewer.exception.CodeReviewNotFoundException;
import com.codereviewer.model.CodeExplanation;
import com.codereviewer.model.User;
import com.codereviewer.repository.CodeExplanationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class CodeExplainerService {

    private static final Logger log = LoggerFactory.getLogger(CodeExplainerService.class );
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");
    private static final Pattern JSON_OBJ   = Pattern.compile("\\{[\\s\\S]*\\}");

    private final CodeExplanationRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.key}")
    private String apiKey;

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.api.model}")
    private String model;
    
    @Autowired
    public CodeExplainerService(CodeExplanationRepository repository,
                                 RestTemplate restTemplate,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public ExplainResponse explain(ExplainRequest request, User user) {
        log.info("Explaining code for user: {}, length: {} via Ollama (Gemma)", user.getEmail(), request.getCode().length());

        AiExplanationResult ai = callOllama(request.getCode());

        CodeExplanation saved = repository.save(
                CodeExplanation.builder()
                        .user(user)
                        .language(ai.getLanguage() != null ? ai.getLanguage() : "unknown")
                        .originalCode(request.getCode())
                        .overview(ai.getOverview())
                        .howItWorks(ai.getHowItWorks())
                        .functionsJson(toJson(ai.getFunctions()))
                        .complexity(ai.getComplexity())
                        .useCasesJson(toJson(ai.getUseCases()))
                        .keyConceptsJson(toJson(ai.getKeyConcepts()))
                        .build()
        );

        log.info("Saved explanation id: {} | language: {}", saved.getId(), saved.getLanguage());
        return toResponse(saved, ai);
    }

    @Transactional(readOnly = true)
    public List<ExplainHistorySummary> getHistory(User user) {
        return repository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ExplainResponse getById(Long id, User user) {
        CodeExplanation e = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Explanation not found: " + id));
        return toResponse(e);
    }

    public void delete(Long id, User user) {
        repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new CodeReviewNotFoundException("Explanation not found: " + id));
        repository.deleteByIdAndUserId(id, user.getId());
    }

    private AiExplanationResult callOllama(String code) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.3);
            root.put("max_tokens", 4096);

            ObjectNode responseFormat = root.putObject("response_format");
            responseFormat.put("type", "json_object");

            ArrayNode messages = root.putArray("messages");

            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content",
                "You are an expert engineer. Respond ONLY with a valid JSON object. No markdown.");

            ObjectNode usr = messages.addObject();
            usr.put("role", "user");
            usr.put("content", buildPrompt(code));

            String body = objectMapper.writeValueAsString(root);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

            String text = extractText(response.getBody());
            if (text == null || text.isBlank())
                throw new AiResponseParsingException("Ollama returned empty explanation");

            return parseResult(text);

        } catch (AiResponseParsingException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Ollama API error: {}", e.getResponseBodyAsString());
            throw new AiResponseParsingException("Ollama API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error during code explanation", e);
            throw new AiResponseParsingException("Explanation failed: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String code) {
        return "Explain this code. Output ONLY JSON.\n" +
               "Schema:\n" +
               "{\n" +
               "  \"language\": \"string\",\n" +
               "  \"overview\": \"string\",\n" +
               "  \"how_it_works\": \"string\",\n" +
               "  \"functions\": [{ \"name\": \"string\", \"purpose\": \"string\", \"parameters\": \"string\", \"returns\": \"string\" }],\n" +
               "  \"complexity\": \"string\",\n" +
               "  \"use_cases\": [\"string\"],\n" +
               "  \"key_concepts\": [\"string\"]\n" +
               "}\n\n" +
               "Code:\n" + code;
    }

    private String extractText(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode content = root.path("choices").get(0)
                    .path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private AiExplanationResult parseResult(String raw) {
        try {
            String cleaned = raw.trim();
            Matcher block = JSON_BLOCK.matcher(cleaned);
            if (block.find()) cleaned = block.group(1).trim();
            else {
                Matcher obj = JSON_OBJ.matcher(cleaned);
                if (obj.find()) cleaned = obj.group().trim();
            }

            AiExplanationResult result = objectMapper.readValue(cleaned, AiExplanationResult.class);

            if (result.getOverview() == null)    result.setOverview("Explanation not available.");
            if (result.getHowItWorks() == null)  result.setHowItWorks("Not available.");
            if (result.getFunctions() == null)   result.setFunctions(Collections.emptyList());
            if (result.getUseCases() == null)    result.setUseCases(Collections.emptyList());
            if (result.getKeyConcepts() == null) result.setKeyConcepts(Collections.emptyList());
            if (result.getComplexity() == null)  result.setComplexity("Not analyzed.");

            return result;
        } catch (Exception e) {
            log.error("Failed to parse explanation result", e);
            throw new AiResponseParsingException("Failed to parse AI explanation", e);
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private <T> List<T> fromJson(String json, TypeReference<List<T>> ref) {
        try {
            if (json == null || json.isBlank()) return Collections.emptyList();
            return objectMapper.readValue(json, ref);
        } catch (Exception e) { return Collections.emptyList(); }
    }

    private ExplainResponse toResponse(CodeExplanation e, AiExplanationResult ai) {
        return ExplainResponse.builder()
                .id(e.getId())
                .language(e.getLanguage())
                .originalCode(e.getOriginalCode())
                .overview(ai.getOverview())
                .howItWorks(ai.getHowItWorks())
                .functions(ai.getFunctions())
                .complexity(ai.getComplexity())
                .useCases(ai.getUseCases())
                .keyConcepts(ai.getKeyConcepts())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ExplainResponse toResponse(CodeExplanation e) {
        List<FunctionBreakdown> fns = fromJson(e.getFunctionsJson(),
                new TypeReference<List<FunctionBreakdown>>() {});
        List<String> useCases = fromJson(e.getUseCasesJson(),
                new TypeReference<List<String>>() {});
        List<String> concepts = fromJson(e.getKeyConceptsJson(),
                new TypeReference<List<String>>() {});

        return ExplainResponse.builder()
                .id(e.getId())
                .language(e.getLanguage())
                .originalCode(e.getOriginalCode())
                .overview(e.getOverview())
                .howItWorks(e.getHowItWorks())
                .functions(fns)
                .complexity(e.getComplexity())
                .useCases(useCases)
                .keyConcepts(concepts)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private ExplainHistorySummary toSummary(CodeExplanation e) {
        return ExplainHistorySummary.builder()
                .id(e.getId())
                .language(e.getLanguage())
                .overview(e.getOverview())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
