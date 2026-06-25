package com.codereviewer.service;

import com.codereviewer.model.MigrationProject;
import com.codereviewer.repository.MigrationProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.*;

@Service
public class MigrationProcessor {
    private static final Logger log = LoggerFactory.getLogger(MigrationProcessor.class);
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "go", "rs", "cs", "cpp", "c", "cc",
            "cxx", "php", "rb", "kt", "swift", "scala", "r", "lua", "dart"
    );
    private static final int MAX_FILE_BYTES = 50_000;
    private static final Map<String, String> LANG_TO_EXT = Map.ofEntries(
            Map.entry("python",     "py"),
            Map.entry("java",       "java"),
            Map.entry("javascript", "js"),
            Map.entry("typescript", "ts"),
            Map.entry("go",         "go"),
            Map.entry("rust",       "rs"),
            Map.entry("csharp",     "cs"),
            Map.entry("c#",         "cs"),
            Map.entry("cpp",        "cpp"),
            Map.entry("c++",        "cpp"),
            Map.entry("kotlin",     "kt"),
            Map.entry("swift",      "swift"),
            Map.entry("php",        "php"),
            Map.entry("ruby",       "rb")
    );

    private final MigrationProjectRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.key:}")
    private String apiKey;

    @Value("${ollama.api.url}")
    private String apiUrl;

    @Value("${ollama.api.model}")
    private String model;

    @Value("${migration.throttle.delay-ms:1500}")
    private long throttleDelay;

    @Autowired
    public MigrationProcessor(MigrationProjectRepository repository,
                              RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Async("migrationExecutor")
    public void processMigration(Long projectId, byte[] zipBytes,
                                 String targetLanguage, List<String> codeFiles) {
        log.info("Async migration started for project: {}", projectId);

        MigrationProject project = repository.findById(projectId).orElse(null);
        if (project == null) return;

        try {
            project.setStatus(MigrationProject.Status.PROCESSING);
            repository.save(project);

            List<Map<String, String>> fileSummaries = new ArrayList<>();
            ByteArrayOutputStream outputZipBytes = new ByteArrayOutputStream();

            try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes));
                 ZipOutputStream zipOut = new ZipOutputStream(outputZipBytes)) {

                Map<String, byte[]> allEntries = new LinkedHashMap<>();
                ZipEntry entry;
                while ((entry = zipIn.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        allEntries.put(entry.getName(), zipIn.readAllBytes());
                    }
                    zipIn.closeEntry();
                }

                String detectedSource = detectSourceLanguage(allEntries, codeFiles);
                project.setSourceLanguage(detectedSource);
                repository.save(project);

                int processed = 0;
                for (Map.Entry<String, byte[]> fileEntry : allEntries.entrySet()) {
                    // Check if migration was cancelled (record deleted from DB)
                    MigrationProject currentProject = repository.findById(projectId).orElse(null);
                    if (currentProject == null) {
                        log.info("Migration {} cancelled by user. Exiting processor.", projectId);
                        return;
                    }
                    project = currentProject;

                    String filePath = fileEntry.getKey();
                    byte[] fileBytes = fileEntry.getValue();
                    String ext = getExtension(filePath);

                    if (CODE_EXTENSIONS.contains(ext)) {
                        try {
                            Thread.sleep(throttleDelay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        String originalCode = new String(fileBytes, StandardCharsets.UTF_8);
                        String convertedCode = convertFile(originalCode, filePath,
                                detectedSource, targetLanguage);

                        String newPath = changeExtension(filePath, targetLanguage);
                        zipOut.putNextEntry(new ZipEntry(newPath));
                        zipOut.write(convertedCode.getBytes(StandardCharsets.UTF_8));
                        zipOut.closeEntry();

                        fileSummaries.add(Map.of(
                                "original", filePath,
                                "converted", newPath,
                                "status", "migrated"
                        ));

                        processed++;
                        project.setProcessedFiles(processed);
                        repository.save(project);

                        log.info("Migrated {}/{}: {}", processed, codeFiles.size(), filePath);
                    } else {
                        zipOut.putNextEntry(new ZipEntry(filePath));
                        zipOut.write(fileBytes);
                        zipOut.closeEntry();

                        fileSummaries.add(Map.of(
                                "original", filePath,
                                "converted", filePath,
                                "status", "copied"
                        ));
                    }
                }

                String plan = generateMigrationPlan(
                        project.getProjectName(), detectedSource,
                        targetLanguage, fileSummaries);

                zipOut.putNextEntry(new ZipEntry("MIGRATION_PLAN.md"));
                zipOut.write(plan.getBytes(StandardCharsets.UTF_8));
                zipOut.closeEntry();

                project.setMigrationPlan(plan);
            }

            project.setResultZip(outputZipBytes.toByteArray());
            project.setFileSummaryJson(objectMapper.writeValueAsString(fileSummaries));
            project.setStatus(MigrationProject.Status.COMPLETED);
            project.setCompletedAt(LocalDateTime.now());
            repository.save(project);

            log.info("Migration completed for project: {}", projectId);

        } catch (Exception e) {
            log.error("Migration failed for project: {}", projectId, e);
            project.setStatus(MigrationProject.Status.FAILED);
            project.setErrorMessage(e.getMessage());
            repository.save(project);
        }
    }

    private String convertFile(String code, String filePath,
                                String sourceLanguage, String targetLanguage) {
        try {
            if (code.length() > MAX_FILE_BYTES) {
                log.warn("File {} is large ({}), truncating to {}",
                        filePath, code.length(), MAX_FILE_BYTES);
                code = code.substring(0, MAX_FILE_BYTES) +
                       "\n// ... [truncated - file too large]";
            }

            String prompt = buildConversionPrompt(code, filePath, sourceLanguage, targetLanguage);
            String requestBody = buildLlmRequest(prompt);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, org.springframework.http.HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(requestBody, headers), String.class);

            String text = extractLlmText(response.getBody());
            if (text == null || text.isBlank()) return "// Conversion failed for: " + filePath;

            return cleanConvertedCode(text);
        } catch (Exception e) {
            log.error("Failed to convert file: {}", filePath, e);
            return "// Migration failed for this file: " + e.getMessage() +
                   "\n// Original file: " + filePath +
                   "\n\n" + code;
        }
    }

    private String buildConversionPrompt(String code, String filePath,
                                          String source, String target) {
        return "You are an expert polyglot software engineer migrating a production codebase.\n\n" +
               "Convert the following " + source + " file to " + target + ".\n" +
               "File: " + filePath + "\n\n" +
               "Rules:\n" +
               "1. Preserve all logic, algorithms, and structure exactly\n" +
               "2. Use idiomatic " + target + " patterns and conventions\n" +
               "3. Add brief comments only where the language difference is significant\n" +
               "4. Return ONLY the converted code — no explanations, no markdown fences\n" +
               "5. The output must be complete and runnable " + target + " code\n\n" +
               "Code to convert:\n" + code;
    }

    private String buildLlmRequest(String prompt) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.1);
        root.put("max_tokens", 8192);

        com.fasterxml.jackson.databind.node.ArrayNode messages = root.putArray("messages");
        com.fasterxml.jackson.databind.node.ObjectNode sys = messages.addObject();
        sys.put("role", "system");
        sys.put("content",
                "You are a code migration expert. You convert code between programming languages. " +
                "You output ONLY clean converted code with no markdown, no explanations, no code fences.");

        com.fasterxml.jackson.databind.node.ObjectNode usr = messages.addObject();
        usr.put("role", "user");
        usr.put("content", prompt);

        return objectMapper.writeValueAsString(root);
    }

    private String extractLlmText(String responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode content = root.path("choices").get(0)
                    .path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) { return null; }
    }

    private String cleanConvertedCode(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```\\w*\\s*\\n?", "");
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```")).trim();
            }
        }
        return cleaned;
    }

    private String generateMigrationPlan(String projectName, String source,
                                          String target,
                                          List<Map<String, String>> fileSummaries) {
        try {
            long migratedCount = fileSummaries.stream()
                    .filter(f -> "migrated".equals(f.get("status"))).count();
            long copiedCount = fileSummaries.stream()
                    .filter(f -> "copied".equals(f.get("status"))).count();

            String prompt = buildPlanPrompt(projectName, source, target,
                    (int) migratedCount, (int) copiedCount, fileSummaries);

            String requestBody = buildLlmRequest(prompt);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, org.springframework.http.HttpMethod.POST,
                    new org.springframework.http.HttpEntity<>(requestBody, headers), String.class);

            String plan = extractLlmText(response.getBody());
            return plan != null ? plan : buildFallbackPlan(projectName, source,
                    target, fileSummaries);
        } catch (Exception e) {
            log.error("Failed to generate migration plan", e);
            return buildFallbackPlan(projectName, source, target, fileSummaries);
        }
    }

    private String buildPlanPrompt(String projectName, String source, String target,
                                    int migrated, int copied,
                                    List<Map<String, String>> files) {
        StringBuilder fileList = new StringBuilder();
        files.forEach(f -> fileList.append("- ")
                .append(f.get("original")).append(" → ")
                .append(f.get("converted"))
                .append(" [").append(f.get("status")).append("]\n"));

        return "Generate a comprehensive, professional migration plan document in Markdown format.\n\n" +
               "Project: " + projectName + "\n" +
               "Migration: " + source + " → " + target + "\n" +
               "Files migrated: " + migrated + "\n" +
               "Files copied as-is: " + copied + "\n\n" +
               "Files:\n" + fileList + "\n\n" +
               "The plan must include:\n" +
               "1. Executive Summary\n" +
               "2. Migration Overview (what was done, languages involved)\n" +
               "3. Step-by-step Post-Migration Checklist (what the developer must do next)\n" +
               "4. Key Language Differences to be aware of (" + source + " vs " + target + ")\n" +
               "5. Dependencies to add/replace (build tool, package manager, libraries)\n" +
               "6. Testing Strategy (what to test first, how to validate the migration)\n" +
               "7. Common Pitfalls to watch out for\n" +
               "8. File-by-file summary table\n\n" +
               "Be specific, practical, and actionable. This is for a real developer to follow.";
    }

    private String buildFallbackPlan(String projectName, String source,
                                      String target,
                                      List<Map<String, String>> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Migration Plan: ").append(projectName).append("\n\n");
        sb.append("## Summary\n");
        sb.append("- **Source:** ").append(source).append("\n");
        sb.append("- **Target:** ").append(target).append("\n");
        sb.append("- **Total Files:** ").append(files.size()).append("\n\n");
        sb.append("## Files Migrated\n\n");
        sb.append("| Original | Converted | Status |\n");
        sb.append("|---|---|---|\n");
        files.forEach(f -> sb.append("| ").append(f.get("original"))
                .append(" | ").append(f.get("converted"))
                .append(" | ").append(f.get("status")).append(" |\n"));
        return sb.toString();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private String changeExtension(String filePath, String targetLanguage) {
        String newExt = LANG_TO_EXT.getOrDefault(
                targetLanguage.toLowerCase(), targetLanguage.toLowerCase());
        int dot = filePath.lastIndexOf('.');
        String base = dot >= 0 ? filePath.substring(0, dot) : filePath;
        return base + "." + newExt;
    }

    public List<String> listCodeFiles(byte[] zipBytes) throws IOException {
        List<String> codeFiles = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String ext = getExtension(entry.getName());
                    if (CODE_EXTENSIONS.contains(ext)) {
                        codeFiles.add(entry.getName());
                    }
                }
                zis.closeEntry();
            }
        }
        return codeFiles;
    }

    private String detectSourceLanguage(Map<String, byte[]> entries,
                                         List<String> codeFiles) {
        if (codeFiles.isEmpty()) return "unknown";
        String firstExt = getExtension(codeFiles.get(0));
        Map<String, String> extToLang = Map.of(
                "java", "Java", "py", "Python", "js", "JavaScript",
                "ts", "TypeScript", "go", "Go", "rs", "Rust",
                "cs", "C#", "cpp", "C++", "kt", "Kotlin", "rb", "Ruby"
        );
        return extToLang.getOrDefault(firstExt, firstExt.toUpperCase());
    }
}
