package com.microgen.orchestrator.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openrouter", matchIfMissing = false)
public class OpenRouterLlmClient implements LlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model:google/gemini-2.0-flash-exp:free}")
    private String model;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String url = "https://openrouter.ai/api/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("HTTP-Referer", "http://localhost:8081"); // Required by OpenRouter
        headers.set("X-Title", "Microgen Solutions"); // Required by OpenRouter

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            return "ERROR: OpenRouter API request failed: " + e.getStatusCode() + " - " + body;
        } catch (Exception e) {
            System.err.println("OpenRouter API Call failed: " + e.getMessage());
            if (apiKey == null || apiKey.isEmpty() || "YOUR_OPENROUTER_KEY".equals(apiKey)) {
                return "ERROR: Missing OpenRouter API key. Please set 'openrouter.api.key' in application.properties.";
            }
            return "ERROR: Generic failure communicating with OpenRouter: " + e.getMessage();
        }
    }
}
