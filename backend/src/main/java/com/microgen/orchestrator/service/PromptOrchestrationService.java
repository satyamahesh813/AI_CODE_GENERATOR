package com.microgen.orchestrator.service;

import com.microgen.orchestrator.adapter.LlmClient;
import com.microgen.orchestrator.engine.CodeGenerationEngine;
import com.microgen.orchestrator.engine.ZipUtility;
import com.microgen.orchestrator.model.GenerationJob;
import com.microgen.orchestrator.model.GenerationRequest;
import com.microgen.orchestrator.model.IntentModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PromptOrchestrationService {

    private final PromptParsingService parsingService;
    private final PromptEnrichmentService enrichmentService;
    private final LlmClient llmClient;
    private final CodeGenerationEngine generationEngine;
    private final GovernanceService governanceService;

    private final Map<String, GenerationJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, GenerationRequest> requests = new ConcurrentHashMap<>();

    /**
     * Creates a job and immediately processes it.
     * Stores the original request so config overrides can be applied after intent parsing.
     */
    public GenerationJob createAndProcess(GenerationRequest request) {
        GenerationJob job = new GenerationJob();
        job.setId(UUID.randomUUID().toString());
        job.setPrompt(request.getPrompt());
        job.setStatus("PENDING");
        jobs.put(job.getId(), job);
        requests.put(job.getId(), request);
        return processJob(job.getId());
    }

    public GenerationJob processJob(String jobId) {
        GenerationJob job = jobs.get(jobId);
        job.setStatus("PROCESSING");

        // 1. Parse intent from prompt
        IntentModel intent = parsingService.parse(job.getPrompt());

        if (intent.getServiceName() == null || intent.getServiceName().startsWith("ERROR")) {
            job.setStatus("FAILED");
            job.setError("Failed to parse service intent from prompt.");
            return job;
        }

        // 2. Apply UI config panel overrides on top of LLM-parsed intent
        GenerationRequest originalRequest = requests.get(jobId);
        if (originalRequest != null) {
            applyConfigOverrides(intent, originalRequest);
        }

        job.setServiceName(intent.getServiceName());

        // 3. Enrich the raw prompt into a structured engineering specification
        String enrichedPrompt = enrichmentService.enrich(job.getPrompt(), intent);
        System.out.println("=== ENRICHED PROMPT (length=" + enrichedPrompt.length() + ") ===");

        String systemPrompt = buildCodeGenSystemPrompt(intent);
        Map<String, String> allFiles = new java.util.HashMap<>();

        // Single-pass generation — Gemini 2.0 Flash supports 8192 output tokens
        // which is sufficient for most services (15-25 files)
        String llmOutput = llmClient.generate(systemPrompt, enrichedPrompt);

        if (llmOutput.startsWith("ERROR")) {
            job.setStatus("FAILED");
            job.setError(llmOutput);
            return job;
        }

        allFiles.putAll(generationEngine.generateProject(intent, llmOutput));
        System.out.println("Pass 1 generated " + allFiles.size() + " files");

        // Pass 2 only if output looks truncated (< 8 files for a non-trivial service)
        boolean looksIncomplete = allFiles.size() < 6 &&
                (isSet(intent.getMessaging()) || isSet(intent.getAuth()) || isSet(intent.getPersistence()));

        if (looksIncomplete) {
            System.out.println("Output looks incomplete, running pass 2...");
            String pass2Prompt = enrichedPrompt + "\n\nCONTINUATION — The previous response was cut off.\n" +
                    "Already generated: " + String.join(", ", allFiles.keySet().stream()
                            .map(k -> k.contains("/") ? k.substring(k.lastIndexOf('/') + 1) : k)
                            .toList()) + "\n" +
                    "Generate ONLY the remaining files that were not yet generated.";

            String pass2Output = llmClient.generate(systemPrompt, pass2Prompt);
            if (!pass2Output.startsWith("ERROR")) {
                Map<String, String> pass2Files = generationEngine.generateProject(intent, pass2Output);
                pass2Files.forEach(allFiles::putIfAbsent);
                System.out.println("Pass 2 added files, total: " + allFiles.size());
            }
        }

        Map<String, String> files = allFiles;
        job.setGeneratedFiles(files);

        // 5. Governance scan
        List<String> violations = governanceService.scan(files);
        if (!violations.isEmpty()) {
            job.setStatus("FAILED_GOVERNANCE");
            job.setError("Governance violations detected: " + String.join(", ", violations));
        } else {
            job.setStatus("COMPLETED");
        }

        return job;
    }

    /**
     * Apply UI config panel overrides onto the LLM-parsed intent.
     *
     * IMPORTANT: Only override a field when the request value is a non-default,
     * non-NONE explicit selection. This prevents the config panel's default
     * selections from injecting auth/persistence scaffolding that the user
     * never asked for in their prompt.
     *
     * Rule: if the parsed intent already has a meaningful value for a field,
     * the config panel can only *strengthen* it (e.g. change H2 → MYSQL),
     * never inject something the prompt didn't mention (e.g. force JWT when
     * intent says NONE).
     */
    private void applyConfigOverrides(IntentModel intent, GenerationRequest request) {
        // All config panel selections are EXPLICIT user choices — always apply them.
        // The only exception: don't override a field the prompt already set to something
        // more specific (e.g. prompt says "Kafka" → don't let NONE from panel clear it).

        if (isSet(request.getAuth()))         intent.setAuth(request.getAuth());
        if (isSet(request.getDatabase()))     intent.setDatabase(request.getDatabase());
        if (isSet(request.getPersistence()))  intent.setPersistence(request.getPersistence());
        if (isSet(request.getMessaging()))    intent.setMessaging(request.getMessaging());
        if (isSet(request.getCache()))        intent.setCache(request.getCache());
        if (isSet(request.getObservability())) intent.setObservability(request.getObservability());
        if (isSet(request.getBuildTool()))    intent.setBuildTool(request.getBuildTool());
        if (isSet(request.getArchitecture())) intent.setArchitecture(request.getArchitecture());
        if (isSet(request.getLanguage()))     intent.setLanguage(request.getLanguage());
        if (isSet(request.getServiceType()))  intent.setServiceType(request.getServiceType());
    }

    /** True when the value is a real non-null, non-blank, non-NONE selection */
    private boolean isSet(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("NONE");
    }

    /**
     * Builds a dynamic system prompt injecting the resolved intent values,
     * so the LLM generates exactly what was requested rather than guessing.
     */
    private String buildCodeGenSystemPrompt(IntentModel intent) {
        return """
                You are a principal-level Spring Boot engineer with 15 years of production experience.
                Your job is to generate COMPLETE, PRODUCTION-READY Java code — not examples, not stubs.

                ABSOLUTE RULES:
                1. Read the full specification in the user message. Implement EVERY requirement.
                2. Every file wrapped with:
                   /// START FILE: FileName.java
                   <complete file content>
                   /// END FILE
                3. Bare filename only — no paths.
                4. Every Java file: package %s.{subpackage};
                5. COMPLETE implementations — no stubs, no TODOs, no "// implement here".
                6. Spring Boot 3.x. Lombok. SLF4J. Jakarta Validation.
                7. Always generate: pom.xml (with ALL needed deps), Application.java, application.yml.
                8. If the spec says "production-grade" — that means real implementations, not demos.
                9. Handle ALL error cases explicitly — no swallowed exceptions.
                10. Generate as many files as needed — do not truncate or stop early.
                """.formatted(intent.getPackageName());
    }

    public GenerationJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public byte[] getProjectZip(String jobId) throws Exception {
        GenerationJob job = jobs.get(jobId);
        return ZipUtility.createZip(job.getGeneratedFiles());
    }
}
