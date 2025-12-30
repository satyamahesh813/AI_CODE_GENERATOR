package com.microgen.orchestrator.service;

import com.microgen.orchestrator.model.IntentModel;
import org.springframework.stereotype.Service;

@Service
@lombok.RequiredArgsConstructor
public class PromptParsingService {
    private final com.microgen.orchestrator.adapter.LlmClient llmClient;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public IntentModel parse(String prompt) {
        String systemPrompt = """
                You are a JSON extraction expert. Extract microservice configuration from the user prompt.

                CRITICAL: Return ONLY a valid JSON object. No explanations, no markdown, no code blocks.

                Required JSON structure:
                {
                  "serviceName": "string (e.g., kafka-service, order-service)",
                  "packageName": "string (e.g., com.example.kafka)",
                  "language": "JAVA",
                  "framework": "SPRING_BOOT",
                  "architecture": "LAYERED",
                  "serviceType": "GENERAL",
                  "auth": "NONE",
                  "database": "H2",
                  "persistence": "NONE",
                  "port": 8080
                }

                Example 1 - Input: "Create a Kafka microservice"
                Output: {"serviceName":"kafka-service","packageName":"com.example.kafka","language":"JAVA","framework":"SPRING_BOOT","architecture":"LAYERED","serviceType":"GENERAL","auth":"NONE","database":"H2","persistence":"NONE","port":8080}

                Example 2 - Input: "Order service with MySQL and JWT"
                Output: {"serviceName":"order-service","packageName":"com.example.order","language":"JAVA","framework":"SPRING_BOOT","architecture":"LAYERED","serviceType":"GENERAL","auth":"JWT","database":"MYSQL","persistence":"JPA","port":8080}

                Now extract from the user's prompt. Return ONLY the JSON object.
                """;

        try {
            String json = llmClient.generate(systemPrompt, prompt);

            if (json.startsWith("ERROR")) {
                return createMinimalIntent();
            }

            // Extract JSON from various formats
            json = extractJson(json);

            return objectMapper.readValue(json, IntentModel.class);
        } catch (Exception e) {
            System.err.println("Failed to parse LLM intent: " + e.getMessage());
            System.err.println("Using minimal intent - code generation will be fully dynamic based on prompt");
            return createMinimalIntent();
        }
    }

    private String extractJson(String response) {
        String json = response.trim();

        // Remove markdown code blocks
        if (json.contains("```json")) {
            int start = json.indexOf("```json") + 7;
            int end = json.lastIndexOf("```");
            if (end > start) {
                json = json.substring(start, end).trim();
            }
        } else if (json.contains("```")) {
            int start = json.indexOf("```") + 3;
            int end = json.lastIndexOf("```");
            if (end > start) {
                json = json.substring(start, end).trim();
            }
        }

        // Extract just the JSON object
        if (json.contains("{") && json.contains("}")) {
            int start = json.indexOf("{");
            int end = json.lastIndexOf("}") + 1;
            json = json.substring(start, end);
        }

        return json;
    }

    private IntentModel createMinimalIntent() {
        // Minimal intent - allows code generation LLM to handle everything dynamically
        IntentModel intent = new IntentModel();
        intent.setServiceName("microservice"); // Generic, will be overridden by generated code
        intent.setPackageName("com.example.service");
        // All other fields use defaults from IntentModel constructor
        System.out.println("Created minimal intent - relying on code generation LLM for all dynamic details");
        return intent;
    }
}
