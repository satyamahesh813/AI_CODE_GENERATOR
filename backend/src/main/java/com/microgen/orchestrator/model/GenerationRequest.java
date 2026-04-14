package com.microgen.orchestrator.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerationRequest {

    @NotBlank(message = "Prompt must not be blank")
    @Size(min = 5, max = 8000, message = "Prompt must be between 5 and 8000 characters")
    private String prompt;

    // Core
    private String auth;
    private String database;
    private String persistence;

    // Messaging
    private String messaging;

    // Caching
    private String cache;

    // Build & Deploy
    private String buildTool;

    // Observability
    private String observability;

    // Architecture & Language
    private String architecture;
    private String language;
    private String serviceType;
}
