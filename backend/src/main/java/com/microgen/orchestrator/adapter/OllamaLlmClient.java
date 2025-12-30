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
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = false)
public class OllamaLlmClient implements LlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.api.url:http://host.docker.internal:11434/api/chat}")
    private String apiUrl;

    @Value("${ollama.model:llama3}")
    private String model;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("message").path("content").asText();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            return "ERROR: Ollama API request failed: " + e.getStatusCode() + " - " + body;
        } catch (Exception e) {
            System.err.println("Ollama API Call failed: " + e.getMessage());
            return "ERROR: Generic failure communicating with Ollama: " + e.getMessage();
        }
    }
}
