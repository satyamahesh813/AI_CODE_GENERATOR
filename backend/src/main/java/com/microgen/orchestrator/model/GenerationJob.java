package com.microgen.orchestrator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
public class GenerationJob {
    private String id = UUID.randomUUID().toString();
    private String prompt;
    private String serviceName;
    private String status; // PENDING, COMPLETED, FAILED, FAILED_GOVERNANCE
    private LocalDateTime createdAt = LocalDateTime.now();
    private Map<String, String> generatedFiles;
    private String error;

    public GenerationJob(String prompt) {
        this.prompt = prompt;
        this.status = "PENDING";
    }
}
