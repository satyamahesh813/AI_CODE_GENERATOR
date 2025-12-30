package com.microgen.orchestrator.adapter;

public interface LlmClient {
    String generate(String systemPrompt, String userPrompt);
}
