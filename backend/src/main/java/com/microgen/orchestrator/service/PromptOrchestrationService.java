package com.microgen.orchestrator.service;

import com.microgen.orchestrator.adapter.LlmClient;
import com.microgen.orchestrator.engine.CodeGenerationEngine;
import com.microgen.orchestrator.engine.ZipUtility;
import com.microgen.orchestrator.model.GenerationJob;
import com.microgen.orchestrator.model.IntentModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromptOrchestrationService {

    private final PromptParsingService parsingService;
    private final LlmClient llmClient;
    private final CodeGenerationEngine generationEngine;
    private final GovernanceService governanceService;

    // Pure in-memory bypass
    private final Map<String, GenerationJob> jobs = new ConcurrentHashMap<>();

    public GenerationJob createJob(String userPrompt) {
        GenerationJob job = new GenerationJob();
        job.setId(UUID.randomUUID().toString());
        job.setPrompt(userPrompt);
        job.setStatus("PENDING");
        jobs.put(job.getId(), job);
        return job;
    }

    public GenerationJob processJob(String jobId) {
        GenerationJob job = jobs.get(jobId);

        IntentModel intent = parsingService.parse(job.getPrompt());

        // Handle case where intent parsing fails or returns an error message
        if (intent.getServiceName() == null || intent.getServiceName().startsWith("ERROR")) {
            job.setStatus("FAILED");
            job.setError(intent.getServiceName());
            return job;
        }

        // Store service name for meaningful download filename
        job.setServiceName(intent.getServiceName());

        String businessLogicSystemPrompt = """
                You are a Senior Backend Engineer specializing in Spring Boot microservices.

                Generate a COMPLETE, production-ready Spring Boot microservice based on the user's requirements.

                OUTPUT FORMAT - Use this EXACT format for each file:

                /// START FILE: path/to/FileName.java
                package com.example.service;

                import statements...

                @Annotation
                public class FileName {
                    // Complete implementation
                }
                /// END FILE

                CRITICAL REQUIREMENTS:
                1. Generate ALL necessary files for a working microservice:
                   - pom.xml (with all required dependencies)
                   - application.yml or application.properties (complete configuration)
                   - Configuration classes (@Configuration)
                   - DTOs/Models with validation annotations
                   - Service layer (@Service)
                   - Controller layer (@RestController)
                   - Exception handlers (@ControllerAdvice)
                   - Any other components mentioned in requirements

                2. Code Quality Standards:
                   - Use Spring Boot 3.x best practices
                   - Include proper error handling
                   - Add validation using Jakarta Validation
                   - Use SLF4J for logging
                   - Follow RESTful conventions
                   - Include Javadoc for complex logic

                3. Configuration:
                   - Use application.yml format (NOT .properties)
                   - Include all necessary Spring Boot properties
                   - Add sensible defaults
                   - Document configuration with comments

                4. Dependencies (pom.xml):
                   - Spring Boot 3.x parent
                   - Include ONLY dependencies mentioned in requirements
                   - Use latest stable versions
                   - Organize with proper groupId/artifactId

                5. Package Structure:
                   - Use package name from intent: [packageName]
                   - Follow standard structure: controller, service, model, config, exception

                DO NOT:
                - Add explanatory text outside code blocks
                - Generate test files (focus on main code)
                - Include placeholder comments like "// TODO"
                - Use deprecated APIs

                Generate COMPLETE, RUNNABLE code that can be immediately deployed.
                """;
        String llmOutput = llmClient.generate(businessLogicSystemPrompt, job.getPrompt());

        if (llmOutput.startsWith("ERROR")) {
            job.setStatus("FAILED");
            job.setError(llmOutput);
            return job;
        }

        Map<String, String> files = generationEngine.generateProject(intent, llmOutput);

        job.setGeneratedFiles(files);

        // 4. Governance Scan
        java.util.List<String> violations = governanceService.scan(files);
        if (!violations.isEmpty()) {
            job.setStatus("FAILED_GOVERNANCE");
        } else {
            job.setStatus("COMPLETED");
        }

        return job;
    }

    public GenerationJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public byte[] getProjectZip(String jobId) throws Exception {
        GenerationJob job = jobs.get(jobId);
        return ZipUtility.createZip(job.getGeneratedFiles());
    }
}
