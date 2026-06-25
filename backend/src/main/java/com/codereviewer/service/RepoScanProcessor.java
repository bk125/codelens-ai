package com.codereviewer.service;

import com.codereviewer.dto.AiSecurityResult;
import com.codereviewer.dto.SecurityFinding;
import com.codereviewer.model.RepoScan;
import com.codereviewer.repository.RepoScanRepository;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RepoScanProcessor {

    private static final Logger log = LoggerFactory.getLogger(RepoScanProcessor.class);

    private static final int MAX_FILES = 50;

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "go", "rs", "cs", "cpp", "c",
            "php", "rb", "kt", "swift", "scala", "dart"
    );

    private static final Set<String> SKIP_PATHS = Set.of(
            "node_modules", "vendor", ".git", "dist", "build",
            "target", "__pycache__", ".angular", "coverage"
    );

    private static final Map<String, String> TYPE_TO_SEVERITY = Map.ofEntries(
            Map.entry("SQL_INJECTION",           "CRITICAL"),
            Map.entry("COMMAND_INJECTION",        "CRITICAL"),
            Map.entry("HARDCODED_SECRET",         "CRITICAL"),
            Map.entry("HARDCODED_CREDENTIAL",     "CRITICAL"),
            Map.entry("INSECURE_DESERIALIZATION", "CRITICAL"),
            Map.entry("BROKEN_AUTH",              "HIGH"),
            Map.entry("BROKEN_ACCESS_CONTROL",    "HIGH"),
            Map.entry("XSS",                      "HIGH"),
            Map.entry("OPEN_REDIRECT",            "HIGH"),
            Map.entry("PATH_TRAVERSAL",           "HIGH"),
            Map.entry("SSRF",                     "HIGH"),
            Map.entry("XXE",                      "HIGH"),
            Map.entry("SECURITY_MISCONFIG",       "MEDIUM"),
            Map.entry("SENSITIVE_DATA_EXPOSURE",  "MEDIUM"),
            Map.entry("WEAK_CRYPTO",              "MEDIUM"),
            Map.entry("MISSING_AUTH_CHECK",       "MEDIUM"),
            Map.entry("INSECURE_RANDOM",          "LOW"),
            Map.entry("VERBOSE_ERROR",            "LOW"),
            Map.entry("MISSING_RATE_LIMIT",       "LOW")
    );

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.DOTALL);
    private static final Pattern JSON_OBJ   = Pattern.compile("\\{[\\s\\S]*\\}", Pattern.DOTALL);

    private final RepoScanRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.key}")
    private String apiKey;

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.api.model}")
    private String model;

    @Value("${migration.throttle.delay-ms:1500}")
    private long throttleDelay;

    @Autowired
    public RepoScanProcessor(RepoScanRepository repository,
                              RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("migrationExecutor")
    public void processScan(Long scanId, String repoName, String branch, String githubToken) {
        log.info("Async security scan started for repo: {}, scanId: {}", repoName, scanId);

        RepoScan scan = repository.findById(scanId).orElse(null);
        if (scan == null) return;

        try {
            scan.setStatus(RepoScan.Status.FETCHING);
            repository.save(scan);

            String resolvedBranch = branch != null && !branch.isBlank()
                    ? branch : fetchDefaultBranch(repoName, githubToken);
            scan.setBranch(resolvedBranch);

            List<String> codeFiles = fetchCodeFiles(repoName, resolvedBranch, githubToken);

            if (codeFiles.isEmpty()) {
                scan.setStatus(RepoScan.Status.FAILED);
                scan.setErrorMessage("No scannable code files found in this repository (max " + MAX_FILES + " files).");
                repository.save(scan);
                return;
            }

            scan.setTotalFiles(codeFiles.size());
            scan.setStatus(RepoScan.Status.SCANNING);
            repository.save(scan);

            log.info("Found {} code files to scan in {}", codeFiles.size(), repoName);

            List<SecurityFinding> allFindings = new ArrayList<>();
            int processed = 0;

            for (String filePath : codeFiles) {
                if (repository.findById(scanId).isEmpty()) {
                    log.info("Scan {} was deleted, stopping processor", scanId);
                    return;
                }

                scan.setCurrentFile(filePath);
                scan.setProcessedFiles(processed);
                repository.save(scan);

                try {
                    Thread.sleep(throttleDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String content = fetchFileContent(repoName, filePath, resolvedBranch, githubToken);
                if (content == null || content.isBlank()) {
                    processed++;
                    continue;
                }

                List<SecurityFinding> fileFindings = scanFile(filePath, content);
                for (SecurityFinding f : fileFindings) {
                    f.setFile(filePath);
                    f.setSeverity(normalizeSeverity(f.getType(), f.getSeverity()));
                }
                allFindings.addAll(fileFindings);

                processed++;
                scan.setProcessedFiles(processed);
                repository.save(scan);

                log.info("Scanned {}/{}: {} — {} findings", processed, codeFiles.size(),
                        filePath, fileFindings.size());
            }

            int critical = 0, high = 0, medium = 0, low = 0;
            for (SecurityFinding f : allFindings) {
                switch (f.getSeverity()) {
                    case "CRITICAL" -> critical++;
                    case "HIGH"     -> high++;
                    case "MEDIUM"   -> medium++;
                    default         -> low++;
                }
            }

            double riskScore = calculateRiskScore(critical, high, medium, low);
            String summary = buildSummary(repoName, allFindings.size(), critical, high, medium, low, riskScore);

            scan.setProcessedFiles(processed);
            scan.setCurrentFile(null);
            scan.setOverallRiskScore(riskScore);
            scan.setCriticalCount(critical);
            scan.setHighCount(high);
            scan.setMediumCount(medium);
            scan.setLowCount(low);
            scan.setSummary(summary);
            scan.setFindingsJson(objectMapper.writeValueAsString(allFindings));
            scan.setStatus(RepoScan.Status.COMPLETED);
            scan.setCompletedAt(LocalDateTime.now());
            repository.save(scan);

            log.info("Scan {} completed. {} total findings. Risk score: {}", scanId, allFindings.size(), riskScore);

        } catch (Exception e) {
            log.error("Scan {} failed", scanId, e);
            scan.setStatus(RepoScan.Status.FAILED);
            scan.setErrorMessage(e.getMessage());
            repository.save(scan);
        }
    }

    // ── GitHub API ────────────────────────────────────────────────────────────

    private String fetchDefaultBranch(String repoName, String token) {
        try {
            String url = "https://api.github.com/repos/" + repoName;
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, githubHeaders(token), String.class);
            JsonNode json = objectMapper.readTree(resp.getBody());
            return json.get("default_branch").asText("main");
        } catch (Exception e) {
            log.warn("Could not fetch default branch for {}, using main", repoName);
            return "main";
        }
    }

    private List<String> fetchCodeFiles(String repoName, String branch, String token) {
        try {
            String url = "https://api.github.com/repos/" + repoName
                    + "/git/trees/" + branch + "?recursive=1";
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, githubHeaders(token), String.class);

            JsonNode tree = objectMapper.readTree(resp.getBody()).get("tree");
            List<String> codeFiles = new ArrayList<>();

            if (tree != null && tree.isArray()) {
                for (JsonNode node : tree) {
                    String path = node.get("path").asText();
                    String type = node.get("type").asText();

                    if (!"blob".equals(type)) continue;
                    if (shouldSkipPath(path)) continue;

                    String ext = getExtension(path);
                    if (CODE_EXTENSIONS.contains(ext)) {
                        codeFiles.add(path);
                        if (codeFiles.size() >= MAX_FILES) break;
                    }
                }
            }
            return codeFiles;
        } catch (Exception e) {
            log.error("Failed to fetch file tree for {}", repoName, e);
            throw new RuntimeException("Could not fetch repository file tree: " + e.getMessage(), e);
        }
    }

    private String fetchFileContent(String repoName, String filePath, String branch, String token) {
        try {
            String url = "https://raw.githubusercontent.com/" + repoName
                    + "/" + branch + "/" + filePath;
            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.GET, githubHeaders(token), String.class);

            String content = resp.getBody();
            if (content != null && content.length() > 8000) {
                content = content.substring(0, 8000) + "\n// [truncated]";
            }
            return content;
        } catch (Exception e) {
            log.warn("Could not fetch content of {}: {}", filePath, e.getMessage());
            return null;
        }
    }

    private HttpEntity<Void> githubHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return new HttpEntity<>(headers);
    }

    // ── AI Security Scan ─────────────────────────────────────────────────────

    private List<SecurityFinding> scanFile(String filePath, String content) {
        try {
            String ext = getExtension(filePath);
            String prompt = buildSecurityPrompt(filePath, ext, content);

            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("temperature", 0.1);
            root.put("max_tokens", 4096);

            ArrayNode messages = root.putArray("messages");

            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content",
                    "You are a senior application security engineer with deep expertise in framework-level security. "
                    + "You perform SOURCE-TO-SINK static analysis only — you MUST trace an unbroken path from an "
                    + "untrusted external input (HTTP request parameter, request body, query string, header, cookie, "
                    + "file upload, environment variable) all the way to an unsafe sink (raw SQL string concatenation, "
                    + "Runtime.exec(), eval(), direct file write, etc). "
                    + "CRITICAL RULES you must follow before flagging any vulnerability:\n"
                    + "1. FRAMEWORK SAFETY: If a database call uses Spring Data JPA (JpaRepository, CrudRepository), "
                    + "Hibernate named queries, or any ORM method (findById, findByX, save, JPQL, Criteria API), "
                    + "it is NOT SQL injection — these frameworks use parameterised queries internally. "
                    + "Only flag SQL injection if you see explicit string concatenation fed into "
                    + "EntityManager.createNativeQuery(), JdbcTemplate.query() with string concatenation, "
                    + "Statement.execute(), or similar raw JDBC calls.\n"
                    + "2. TYPE SAFETY: Java primitive types and wrapper types (Long, Integer, UUID, Boolean) "
                    + "CANNOT carry SQL injection payloads. Do NOT flag a method call as SQL injection if the "
                    + "parameter type is Long, int, Integer, UUID, or any other non-String primitive.\n"
                    + "3. FRAMEWORK AWARENESS: Spring Security annotations (@PreAuthorize, @Secured, @RolesAllowed), "
                    + "Spring Validation (@Valid, @NotNull, @Pattern), and Bean Validation automatically mitigate "
                    + "many vulnerability classes. Account for these before flagging.\n"
                    + "4. NO HALLUCINATION: If you cannot find a direct, verifiable source-to-sink path in the code "
                    + "shown, return an empty vulnerabilities array. Partial evidence is NOT sufficient. "
                    + "A method name containing 'find', 'get', or 'query' is NOT evidence of injection — "
                    + "you must see the actual unsafe sink implementation.\n"
                    + "5. CONFIDENCE THRESHOLD: Only report findings you would confidently defend in a code review "
                    + "with a senior engineer. If a finding could be argued either way, omit it.\n"
                    + "Respond with ONLY a raw JSON object. No markdown, no code fences, no explanation. "
                    + "Start directly with { and end with }.");

            ObjectNode usr = messages.addObject();
            usr.put("role", "user");
            usr.put("content", prompt);

            String body = objectMapper.writeValueAsString(root);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            String rawText = extractText(response.getBody());
            if (rawText == null || rawText.isBlank()) return Collections.emptyList();

            return parseFindings(rawText);

        } catch (Exception e) {
            log.warn("Security scan failed for file {}: {}", filePath, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildSecurityPrompt(String filePath, String ext, String codeContent) {
        return "Perform a rigorous SOURCE-TO-SINK security analysis on this "
                + ext.toUpperCase() + " file: " + filePath + "\n\n"

                + "MANDATORY VERIFICATION before flagging any vulnerability:\n"
                + "  a) UNTRUSTED SOURCE: Identify the exact external input entry point "
                + "(HTTP param, request body, query string, cookie, header, file upload, env var). "
                + "Java primitives (Long, Integer, UUID, boolean) are NOT untrusted sources for injection.\n"
                + "  b) UNBROKEN PATH: There must be a direct, traceable path from the source to an unsafe sink.\n"
                + "  c) NO FRAMEWORK INTERCEPTION: Confirm no ORM, framework, or library sanitises data before the sink.\n"
                + "  d) UNSAFE SINK: The sink must actually execute unsanitised input — Spring Data JPA methods "
                + "(findById, findAll, save, delete, any derived query), Hibernate, and JPQL named parameters "
                + "are SAFE and must NEVER be flagged as SQL injection.\n\n"

                + "FRAMEWORK SAFETY RULES — violations of these are FALSE POSITIVES, never report them:\n"
                + "- Spring Data JPA repository methods are parameterised internally. NEVER flag as SQL injection.\n"
                + "- Long/Integer/UUID/Boolean/Enum method parameters CANNOT carry SQL injection. NEVER flag.\n"
                + "- @PreAuthorize/@Secured/hasRole() indicates access control is handled. Do not flag without proof it is missing.\n"
                + "- @Valid + DTO validation handles input sanitisation. Do not flag validated fields.\n"
                + "- Only flag SQL_INJECTION when you see: EntityManager.createNativeQuery() with string concatenation, "
                + "JdbcTemplate.query/update() with string concatenation, or Statement.execute() with string concatenation.\n\n"

                + "VALID vulnerability types (report ONLY with full source-to-sink evidence):\n"
                + "SQL_INJECTION, COMMAND_INJECTION, HARDCODED_SECRET, HARDCODED_CREDENTIAL, "
                + "INSECURE_DESERIALIZATION, BROKEN_ACCESS_CONTROL, PATH_TRAVERSAL, SSRF, XXE, "
                + "WEAK_CRYPTO, INSECURE_RANDOM, SENSITIVE_DATA_EXPOSURE\n\n"

                + "JSON response format:\n"
                + "{\n"
                + "  \"vulnerabilities\": [\n"
                + "    {\n"
                + "      \"type\": \"<type from list above>\",\n"
                + "      \"severity\": \"<CRITICAL|HIGH|MEDIUM|LOW>\",\n"
                + "      \"line\": <integer>,\n"
                + "      \"cwe\": \"<CWE-ID>\",\n"
                + "      \"description\": \"<exact source, exact sink, and why framework does NOT protect here>\",\n"
                + "      \"exploit_scenario\": \"<concrete attack with example payload>\",\n"
                + "      \"fix\": \"<specific remediation>\",\n"
                + "      \"fixed_code_snippet\": \"<corrected code>\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "No evidence of source-to-sink path? Return: {\"vulnerabilities\": []}\n\n"
                + "Code to analyse:\n" + codeContent;
    }

    private List<SecurityFinding> parseFindings(String raw) {
        try {
            String cleaned = raw.trim();
            Matcher block = JSON_BLOCK.matcher(cleaned);
            if (block.find()) cleaned = block.group(1).trim();
            else {
                Matcher obj = JSON_OBJ.matcher(cleaned);
                if (obj.find()) cleaned = obj.group().trim();
            }

            AiSecurityResult result = objectMapper.readValue(cleaned, AiSecurityResult.class);
            return result.getVulnerabilities() != null
                    ? result.getVulnerabilities() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to parse security scan result: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String normalizeSeverity(String type, String aiSeverity) {
        if (type != null) {
            String fixed = TYPE_TO_SEVERITY.get(type.toUpperCase());
            if (fixed != null) return fixed;
        }
        if (aiSeverity != null) {
            String upper = aiSeverity.toUpperCase();
            if (List.of("CRITICAL","HIGH","MEDIUM","LOW").contains(upper)) return upper;
        }
        return "MEDIUM";
    }

    private double calculateRiskScore(int critical, int high, int medium, int low) {
        double raw = (critical * 4.0) + (high * 2.0) + (medium * 1.0) + (low * 0.25);
        return Math.min(10.0, Math.round(raw * 10.0) / 10.0);
    }

    private String buildSummary(String repoName, int total, int critical,
                                 int high, int medium, int low, double score) {
        if (total == 0) return "No security vulnerabilities detected in " + repoName + ".";
        return repoName + " has " + total + " security issue(s) — "
                + critical + " critical, " + high + " high, "
                + medium + " medium, " + low + " low. Risk score: " + score + "/10.";
    }

    private boolean shouldSkipPath(String path) {
        for (String skip : SKIP_PATHS) {
            if (path.contains("/" + skip + "/") || path.startsWith(skip + "/")) return true;
        }
        return false;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").get(0)
                    .path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            return null;
        }
    }
}