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
@ConditionalOnProperty(name = "llm.provider", havingValue = "huggingface")
public class HuggingFaceLlmClient implements LlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${huggingface.api.key:}")
    private String apiKey;

    @Value("${huggingface.model:mistralai/Codestral-22B-v0.1}")
    private String model;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1}")
    private String apiUrl;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_HF_KEY")) {
            return "ERROR: Missing HuggingFace API Key. Get one at https://huggingface.co/settings/tokens";
        }

        // HuggingFace now uses OpenAI-compatible chat completions endpoint
        String url = apiUrl + "/chat/completions";

        // Build messages array with system and user prompts
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messagesArray = requestBody.putArray("messages");

        // Add system message
        ObjectNode systemMessage = messagesArray.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        // Add user message
        ObjectNode userMessage = messagesArray.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        // Add parameters
        requestBody.put("max_tokens", 4000);
        requestBody.put("temperature", 0.7);
        requestBody.put("top_p", 0.95);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response);

            // OpenAI-compatible format: {"choices": [{"message": {"content": "..."}}]}
            if (responseJson.has("choices") && responseJson.get("choices").isArray()
                    && responseJson.get("choices").size() > 0) {
                return responseJson.get("choices").get(0)
                        .path("message")
                        .path("content")
                        .asText();
            }

            return "ERROR: Unexpected HuggingFace response format: " + response;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("HuggingFace API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                return "ERROR: Invalid HuggingFace API key. Get one at https://huggingface.co/settings/tokens";
            } else if (e.getStatusCode().value() == 503) {
                return "ERROR: Model is loading. HuggingFace models need to warm up. Try again in 20-30 seconds.";
            } else if (e.getStatusCode().value() == 404) {
                return "ERROR: Model not found. Check if '" + model + "' is available on HuggingFace router.";
            }
            return "ERROR: HuggingFace API error: " + e.getMessage();
        } catch (Exception e) {
            System.err.println("HuggingFace API Call failed: " + e.getMessage());
            return "ERROR: Failed to communicate with HuggingFace: " + e.getMessage();
        }
    }
}
