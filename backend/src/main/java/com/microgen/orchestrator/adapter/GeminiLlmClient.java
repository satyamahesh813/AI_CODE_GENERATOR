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

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLlmClient() {
        // 120s timeout — complex code generation can take time
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash-lite}")
    private String model;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_GEMINI_KEY")) {
            return "ERROR: Missing Gemini API Key. Please set 'gemini.api.key' in application.properties.";
        }
        if (model == null || model.isEmpty()) {
            return "ERROR: Missing Gemini Model. Please set 'gemini.model' in application.properties (e.g., gemini-1.5-flash).";
        }

        // Use v1beta — supports all current Gemini models including 1.5-flash, 2.0-flash
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        // Combine prompts for Gemini (simple approach) or use multi-turn
        String combinedPrompt = systemPrompt + "\n\nUser Request: " + userPrompt;

        // Construct Request Body
        ObjectNode rootNode = objectMapper.createObjectNode();
        ArrayNode contents = rootNode.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", combinedPrompt);

        // Generation config — max tokens, low temperature for deterministic code
        ObjectNode generationConfig = rootNode.putObject("generationConfig");
        generationConfig.put("maxOutputTokens", 65536);
        generationConfig.put("temperature", 0.2);
        generationConfig.put("topP", 0.95);

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
            return "ERROR: The AI model '" + model + "' is not available. " +
                    "Please update gemini.model in application.properties to gemini-2.0-flash-lite or gemini-2.0-flash.";
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            System.err.println("Gemini HTTP " + status + ": " + body);
            if (status == 429) {
                int retrySeconds = extractRetryDelay(body);
                // If daily quota is exhausted (limit: 0), don't retry — it won't help
                if (body.contains("\"limit\": 0") || body.contains("\"quotaValue\": \"0\"")) {
                    return "ERROR: Daily quota exhausted for model '" + model + "'. " +
                            "The free tier limit has been reached for today. " +
                            "Try switching to gemini-2.0-flash-lite in application.properties, or wait until tomorrow.";
                }
                // Per-minute rate limit — wait and retry up to 2 times
                for (int attempt = 1; attempt <= 2; attempt++) {
                    System.out.println("Gemini rate limited (attempt " + attempt + "). Waiting " + retrySeconds + "s...");
                    try {
                        Thread.sleep(retrySeconds * 1000L + 500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    try {
                        HttpEntity<String> retryEntity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);
                        String retryResponse = restTemplate.postForObject(url, retryEntity, String.class);
                        JsonNode retryJson = objectMapper.readTree(retryResponse);
                        System.out.println("Gemini retry attempt " + attempt + " succeeded.");
                        return retryJson.path("candidates").get(0)
                                .path("content").path("parts").get(0)
                                .path("text").asText();
                    } catch (org.springframework.web.client.HttpClientErrorException retryEx) {
                        if (retryEx.getStatusCode().value() == 429) {
                            retrySeconds = extractRetryDelay(retryEx.getResponseBodyAsString());
                            System.out.println("Still rate limited, will retry again in " + retrySeconds + "s...");
                        } else {
                            return "ERROR: Gemini retry failed: " + retryEx.getMessage();
                        }
                    } catch (Exception retryEx) {
                        return "ERROR: Gemini retry failed: " + retryEx.getMessage();
                    }
                }
                return "ERROR: Rate limit persists after retries. Please wait a minute before trying again.";
            }
            return "ERROR: Gemini API error " + status + ": " + friendlyHttpError(status);
        } catch (Exception e) {
            System.err.println("Gemini API Call failed: " + e.getMessage());
            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_GEMINI_KEY")) {
                return "ERROR: API key is not configured. Please add your Gemini API key to application.properties (gemini.api.key=YOUR_KEY). Get a free key at aistudio.google.com.";
            }
            if (e.getMessage() != null && e.getMessage().contains("timeout")) {
                return "ERROR: The AI took too long to respond. Your prompt may be too complex — try simplifying it or breaking it into smaller parts.";
            }
            return "ERROR: Could not connect to the AI service. Please check your internet connection and try again.";
        }
    }

    private String friendlyHttpError(int status) {
        return switch (status) {
            case 400 -> "Invalid request sent to AI. Please try rephrasing your prompt.";
            case 401 -> "API key is invalid or expired. Please check your gemini.api.key in application.properties.";
            case 403 -> "Access denied. Your API key may not have permission for this model.";
            case 500, 502, 503 -> "The AI service is temporarily unavailable. Please try again in a moment.";
            default  -> "Unexpected error from AI service (HTTP " + status + "). Please try again.";
        };
    }

    /** Parses the retry delay in seconds from a Gemini 429 error body. Defaults to 15s. */
    private int extractRetryDelay(String body) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("retry[^0-9]*([0-9]+(?:\\.[0-9]+)?)")
                    .matcher(body);
            if (m.find()) {
                return (int) Math.ceil(Double.parseDouble(m.group(1)));
            }
        } catch (Exception ignored) {}
        return 15;
    }
}
