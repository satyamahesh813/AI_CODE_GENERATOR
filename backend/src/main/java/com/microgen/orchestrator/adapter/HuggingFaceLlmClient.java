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

    @Value("${huggingface.model:Qwen/Qwen2.5-Coder-32B-Instruct}")
    private String model;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1}")
    private String apiUrl;

    // Safe default: 4096 works for all models. Override via huggingface.max_tokens
    @Value("${huggingface.max_tokens:4096}")
    private int maxTokens;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.contains("YOUR_HF_KEY")) {
            return "ERROR: Missing HuggingFace API Key. Get one at https://huggingface.co/settings/tokens";
        }

        String url = apiUrl + "/chat/completions";

        // Truncate prompts if combined length is too large (prevents 400 on small models)
        String safeSystem = truncate(systemPrompt, 800);
        String safeUser   = truncate(userPrompt, 3000);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);

        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject().put("role", "system").put("content", safeSystem);
        messages.addObject().put("role", "user").put("content", safeUser);

        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", 0.2);
        requestBody.put("top_p", 0.95);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode responseJson = objectMapper.readTree(response);

            if (responseJson.has("choices") && responseJson.get("choices").isArray()
                    && responseJson.get("choices").size() > 0) {
                return responseJson.get("choices").get(0)
                        .path("message").path("content").asText();
            }

            return "ERROR: Unexpected HuggingFace response format: " + response;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            System.err.println("HuggingFace API Error " + status + ": " + body);

            return switch (status) {
                case 400 -> "ERROR: Bad request to HuggingFace (model=" + model
                        + "). The prompt may be too long or max_tokens too high for this model. "
                        + "Try switching to a smaller model or reducing prompt size. Details: " + body;
                case 401 -> "ERROR: Invalid HuggingFace API key. Get one at https://huggingface.co/settings/tokens";
                case 403 -> "ERROR: HuggingFace access denied. The model '" + model
                        + "' may require a Pro subscription or gated access.";
                case 429 -> "ERROR: HuggingFace rate limit exceeded. Wait a minute and try again.";
                case 503 -> "ERROR: Model '" + model + "' is loading. Try again in 20-30 seconds.";
                case 404 -> "ERROR: Model '" + model + "' not found on HuggingFace router. "
                        + "Check available models at https://huggingface.co/models";
                default  -> "ERROR: HuggingFace API error " + status + ": " + body;
            };
        } catch (Exception e) {
            System.err.println("HuggingFace API Call failed: " + e.getMessage());
            return "ERROR: Failed to communicate with HuggingFace: " + e.getMessage();
        }
    }

    /** Truncates a string to maxChars, appending a note if truncated. */
    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars) + "\n[... truncated for model context limit ...]";
    }
}
