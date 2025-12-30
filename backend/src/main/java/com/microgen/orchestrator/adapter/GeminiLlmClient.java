package com.microgen.orchestrator.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiLlmClient implements LlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash-latest}")
    private String model;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_GEMINI_KEY")) {
            return "ERROR: Missing Gemini API Key. Please set 'gemini.api.key' in application.properties.";
        }
        if (model == null || model.isEmpty()) {
            return "ERROR: Missing Gemini Model. Please set 'gemini.model' in application.properties (e.g., gemini-1.5-flash).";
        }

        // Standardizing on v1 stable API
        String url = "https://generativelanguage.googleapis.com/v1/models/" + model + ":generateContent?key=" + apiKey;

        // Combine prompts for Gemini (simple approach) or use multi-turn
        String combinedPrompt = systemPrompt + "\n\nUser Request: " + userPrompt;

        // Construct Request Body
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode contents = rootNode.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", combinedPrompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response);

            // Extract text from Gemini response structure:
            // candidates[0].content.parts[0].text
            return responseJson
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            return "ERROR: Gemini model '" + model
                    + "' not found on API v1. Please check if this model is available in your region or try 'gemini-1.5-pro'. Body: "
                    + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("Gemini API Call failed: " + e.getMessage());
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_KEY")) {
                return "ERROR: Missing Gemini API Key. Please get one for free at aistudio.google.com and set it in application.properties.";
            }
            return "ERROR: Failed to communicate with Gemini: " + e.getMessage();
        }
    }
}
