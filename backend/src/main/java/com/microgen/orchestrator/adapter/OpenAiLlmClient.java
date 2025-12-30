package com.microgen.orchestrator.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiLlmClient implements LlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${llm.api.base}")
    private String apiBase;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String model;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String url = apiBase + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String body = e.getResponseBodyAsString();
            if (body.contains("insufficient_quota")) {
                return "ERROR: LLM API limit reached (insufficient quota). Please check your billing or use a local LLM like Ollama.";
            }
            return "ERROR: LLM API request failed: " + e.getStatusText() + " - " + body;
        } catch (Exception e) {
            System.err.println("LLM API Call failed: " + e.getMessage());
            if ("YOUR_API_KEY_HERE".equals(apiKey)) {
                return "ERROR: Missing API key. Please set a valid key in application.properties.";
            }
            return "ERROR: Generic failure communicating with LLM: " + e.getMessage();
        }
    }
}
