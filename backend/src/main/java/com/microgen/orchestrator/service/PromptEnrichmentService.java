package com.microgen.orchestrator.service;

import com.microgen.orchestrator.model.IntentModel;
import com.microgen.orchestrator.service.PromptComplexityAnalyzer.AnalysisResult;
import com.microgen.orchestrator.service.PromptComplexityAnalyzer.ComplexityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Transforms any user prompt into a structured engineering specification
 * before sending to the LLM.
 *
 * The user's original prompt is ALWAYS the primary instruction — never replaced.
 * Enrichment adds: resolved tech context, pattern-specific guidance, and
 * strict output constraints based on detected complexity.
 */
@Service
@RequiredArgsConstructor
public class PromptEnrichmentService {

    private final PromptComplexityAnalyzer complexityAnalyzer;

    public String enrich(String rawPrompt, IntentModel intent) {
        // Analyze the prompt for complexity and patterns
        AnalysisResult analysis = complexityAnalyzer.analyze(rawPrompt, intent);

        StringBuilder spec = new StringBuilder();

        // ── 1. User's original requirements — verbatim, always first ──────────
        spec.append("IMPLEMENT THE FOLLOWING EXACTLY AS SPECIFIED:\n");
        spec.append("=".repeat(70)).append("\n");
        spec.append(rawPrompt.trim()).append("\n");
        spec.append("=".repeat(70)).append("\n\n");

        // ── 2. Complexity level signal ────────────────────────────────────────
        spec.append("COMPLEXITY LEVEL: ").append(analysis.level()).append("\n");
        if (!analysis.detectedPatterns().isEmpty()) {
            spec.append("DETECTED PATTERNS: ").append(String.join(", ", analysis.detectedPatterns())).append("\n");
        }
        spec.append("\n");

        // ── 3. Resolved technical context ─────────────────────────────────────
        spec.append("RESOLVED TECHNICAL CONTEXT:\n");
        spec.append("  Service Name  : ").append(intent.getServiceName()).append("\n");
        spec.append("  Package       : ").append(intent.getPackageName()).append("\n");
        spec.append("  Language      : ").append(intent.getLanguage()).append("\n");
        spec.append("  Build Tool    : ").append(intent.getBuildTool()).append("\n");
        spec.append("  Architecture  : ").append(intent.getArchitecture()).append("\n");
        spec.append("  Port          : ").append(intent.getPort()).append("\n");
        if (!isNone(intent.getDatabase()))      spec.append("  Database      : ").append(intent.getDatabase()).append("\n");
        if (!isNone(intent.getPersistence()))   spec.append("  Persistence   : ").append(intent.getPersistence()).append("\n");
        if (!isNone(intent.getMessaging()))     spec.append("  Messaging     : ").append(intent.getMessaging()).append("\n");
        if (!isNone(intent.getCache()))         spec.append("  Cache         : ").append(intent.getCache()).append("\n");
        if (!isNone(intent.getAuth()))          spec.append("  Auth          : ").append(intent.getAuth()).append("\n");
        if (!isNone(intent.getObservability())) spec.append("  Observability : ").append(intent.getObservability()).append("\n");
        spec.append("\n");

        // ── 4. Pattern-specific production guidance ───────────────────────────
        if (!analysis.productionGuidance().isBlank()) {
            spec.append(analysis.productionGuidance()).append("\n");
        }

        // ── 5. Strict exclusions ──────────────────────────────────────────────
        spec.append("DO NOT GENERATE:\n");
        if (isNone(intent.getAuth()))          spec.append("  - NO security/auth classes unless specified in the prompt above\n");
        if (isNone(intent.getMessaging()))     spec.append("  - NO messaging classes unless specified in the prompt above\n");
        if (isNone(intent.getPersistence()))   spec.append("  - NO ORM/repository classes unless specified in the prompt above\n");
        if (isNone(intent.getCache()))         spec.append("  - NO cache config unless specified in the prompt above\n");
        if (isNone(intent.getObservability())) spec.append("  - NO metrics/tracing unless specified in the prompt above\n");
        if (isNone(intent.getDatabase()))      spec.append("  - NO datasource config unless specified in the prompt above\n");
        spec.append("  - NO TODO comments or placeholder implementations\n");
        spec.append("  - NO explanatory text outside code blocks\n\n");

        // ── 6. Quality bar based on complexity ────────────────────────────────
        if (analysis.level() == ComplexityLevel.PRODUCTION_GRADE) {
            spec.append("QUALITY BAR: PRODUCTION-GRADE\n");
            spec.append("  - Every class must be complete and compilable\n");
            spec.append("  - No stubs, no empty method bodies\n");
            spec.append("  - All error paths must be handled\n");
            spec.append("  - All external calls must have timeouts\n");
            spec.append("  - All secrets via environment variables\n\n");
        } else if (analysis.level() == ComplexityLevel.ADVANCED) {
            spec.append("QUALITY BAR: ADVANCED\n");
            spec.append("  - Complete implementations, no stubs\n");
            spec.append("  - Proper error handling on all paths\n\n");
        }

        // ── 7. Output format ──────────────────────────────────────────────────
        spec.append("OUTPUT FORMAT (mandatory for every file):\n");
        spec.append("  /// START FILE: FileName.java\n");
        spec.append("  <complete, compilable file content>\n");
        spec.append("  /// END FILE\n\n");
        spec.append("RULES:\n");
        spec.append("  - Bare filename only (e.g. OrderService.java) — no paths\n");
        spec.append("  - Every Java file: package ").append(intent.getPackageName()).append(".{subpackage};\n");
        spec.append("  - Always include: pom.xml, Application.java, application.yml\n");
        spec.append("  - Spring Boot 3.x APIs, Lombok, SLF4J\n");
        spec.append("  - Generate ALL files needed — do not stop early\n");

        return spec.toString();
    }

    private boolean isNone(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("NONE");
    }
}
