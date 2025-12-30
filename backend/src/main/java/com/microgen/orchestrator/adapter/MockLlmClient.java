package com.microgen.orchestrator.adapter;

// @Component
public class MockLlmClient implements LlmClient {
    @Override
    public String generate(String systemPrompt, String userPrompt) {
        String lowerPrompt = userPrompt.toLowerCase();
        if (systemPrompt.contains("JSON object")) {
            // Dynamic Intent Extraction Mock
            if (lowerPrompt.contains("customer") || lowerPrompt.contains("auth")) {
                boolean hasOAuth2 = lowerPrompt.contains("oauth2");
                boolean hasJPA = lowerPrompt.contains("jpa") || lowerPrompt.contains("database");
                return """
                        {
                            "serviceName": "customer-service",
                            "packageName": "com.customer.gen",
                            "language": "JAVA",
                            "framework": "SPRING_BOOT",
                            "architecture": "LAYERED",
                            "serviceType": "AUTH",
                            "auth": "%s",
                            "database": "MYSQL",
                            "persistence": "%s",
                            "port": 8082,
                            "entities": [
                                {
                                    "name": "Customer",
                                    "fields": [
                                        {"name": "id", "type": "Long"},
                                        {"name": "username", "type": "String"},
                                        {"name": "password", "type": "String"},
                                        {"name": "email", "type": "String"}
                                    ]
                                }
                            ]
                        }
                        """.formatted(hasOAuth2 ? "OAUTH2" : "JWT", hasJPA ? "JPA" : "NONE");
            }
            // Default generic response
            return """
                    {
                        "serviceName": "micro-service",
                        "packageName": "com.example.service",
                        "language": "JAVA",
                        "framework": "SPRING_BOOT",
                        "architecture": "LAYERED",
                        "serviceType": "GENERAL",
                        "auth": "NONE",
                        "database": "H2",
                        "persistence": "NONE",
                        "port": 8082
                    }
                    """;
        }

        if (lowerPrompt.contains("trigger violation") || lowerPrompt.contains("akia")) {
            return "// Security violation in generated code\npublic String aws_key = \"AKIAQWERT12345678901234567890\";";
        }
        return "// High-quality business logic generated based on: " + userPrompt
                + "\n@org.springframework.web.bind.annotation.GetMapping(\"/api/v1/status\")\npublic String getStatus() { return \"Service is Operational\"; }";
    }
}
