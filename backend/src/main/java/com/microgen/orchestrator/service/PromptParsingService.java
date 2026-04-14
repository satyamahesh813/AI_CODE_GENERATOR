package com.microgen.orchestrator.service;

import com.microgen.orchestrator.model.IntentModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Parses a user prompt into an IntentModel.
 *
 * Strategy (two-phase, resilient):
 *  1. LOCAL rule-based parsing — always runs, never fails, extracts intent
 *     directly from keywords in the prompt. This is the primary source.
 *  2. LLM JSON enrichment — attempted as a best-effort overlay. If the LLM
 *     returns valid JSON it can refine the service/package name. If it fails
 *     or returns garbage, the local parse result is used as-is.
 *
 * This means config panel selections + prompt keywords ALWAYS work correctly
 * regardless of LLM quality or response format.
 */
@Service
@RequiredArgsConstructor
public class PromptParsingService {

    private final com.microgen.orchestrator.adapter.LlmClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentModel parse(String prompt) {
        // Phase 1: local rule-based parse — always succeeds
        IntentModel intent = parseLocally(prompt);

        // Phase 2: try LLM for service/package name enrichment only
        // Skip for long/complex prompts — local parse is sufficient and saves tokens
        try {
            if (prompt.length() < 300) {
                String llmJson = llmClient.generate(buildMinimalSystemPrompt(), prompt);
                if (llmJson != null && !llmJson.startsWith("ERROR")) {
                    String extracted = extractJsonObject(llmJson);
                    if (extracted != null) {
                        var node = objectMapper.readTree(extracted);
                        if (node.has("serviceName") && !node.get("serviceName").asText().isBlank()) {
                            intent.setServiceName(toKebabCase(node.get("serviceName").asText()));
                        }
                        if (node.has("packageName") && !node.get("packageName").asText().isBlank()) {
                            intent.setPackageName(node.get("packageName").asText().toLowerCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("LLM intent enrichment skipped (using local parse): " + e.getMessage());
        }

        // Derive packageName from serviceName if still generic
        if ("com.microgen.generated".equals(intent.getPackageName())) {
            String domain = intent.getServiceName().replace("-service", "").replace("-", "");
            intent.setPackageName("com.example." + domain);
        }

        System.out.println("Intent resolved: " + intent.getServiceName()
                + " | auth=" + intent.getAuth()
                + " | db=" + intent.getDatabase()
                + " | messaging=" + intent.getMessaging()
                + " | cache=" + intent.getCache()
                + " | observability=" + intent.getObservability());

        return intent;
    }

    // ─── Phase 1: Local keyword-based parsing ────────────────────────────────

    private IntentModel parseLocally(String prompt) {
        String p = prompt.toLowerCase();
        IntentModel intent = new IntentModel(); // starts with all NONE defaults

        // ── Service name ──────────────────────────────────────────────────────
        intent.setServiceName(deriveServiceName(p));

        // ── Messaging ─────────────────────────────────────────────────────────
        if (contains(p, "kafka", "dead letter", "dlq", "consumer", "producer", "event stream", "topic")) {
            intent.setMessaging("KAFKA");
        } else if (contains(p, "rabbitmq", "rabbit", "amqp")) {
            intent.setMessaging("RABBITMQ");
        } else if (contains(p, "sqs", "aws queue", "amazon sqs")) {
            intent.setMessaging("SQS");
        } else if (contains(p, "activemq", "active mq", "jms")) {
            intent.setMessaging("ACTIVEMQ");
        }

        // ── Database ──────────────────────────────────────────────────────────
        if (contains(p, "mysql")) {
            intent.setDatabase("MYSQL");
        } else if (contains(p, "postgres", "postgresql")) {
            intent.setDatabase("POSTGRESQL");
        } else if (contains(p, "mongodb", "mongo")) {
            intent.setDatabase("MONGODB");
        } else if (contains(p, "redis db", "redis database", "redis store")) {
            intent.setDatabase("REDIS");
        } else if (contains(p, "h2", "in-memory db", "in memory db")) {
            intent.setDatabase("H2");
        }

        // ── Persistence ───────────────────────────────────────────────────────
        if (contains(p, "jpa", "hibernate", "entity", "repository")) {
            intent.setPersistence("JPA");
        } else if (contains(p, "mybatis")) {
            intent.setPersistence("MYBATIS");
        } else if (contains(p, "r2dbc", "reactive", "webflux", "non-blocking")) {
            intent.setPersistence("R2DBC");
        } else if (!intent.getDatabase().equals("NONE") && !intent.getDatabase().equals("MONGODB")) {
            // If a relational DB is mentioned, default to JPA
            if (contains(p, "mysql", "postgres", "postgresql", "h2")) {
                intent.setPersistence("JPA");
            }
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        if (contains(p, "jwt", "json web token")) {
            intent.setAuth("JWT");
        } else if (contains(p, "oauth2", "oauth", "social login", "sso")) {
            intent.setAuth("OAUTH2");
        } else if (contains(p, "basic auth", "http basic")) {
            intent.setAuth("BASIC");
        } else if (contains(p, "api key", "apikey", "x-api-key")) {
            intent.setAuth("API_KEY");
        }

        // ── Cache ─────────────────────────────────────────────────────────────
        if (contains(p, "redis cache", "redis cach", "cache redis", "caching redis")) {
            intent.setCache("REDIS");
        } else if (contains(p, "caffeine")) {
            intent.setCache("CAFFEINE");
        } else if (contains(p, "hazelcast")) {
            intent.setCache("HAZELCAST");
        } else if (contains(p, "cache", "caching") && !contains(p, "redis db", "redis database")) {
            // Generic "cache" mention — default to Redis
            intent.setCache("REDIS");
        }

        // ── Observability ─────────────────────────────────────────────────────
        if (contains(p, "opentelemetry", "otel")) {
            intent.setObservability("OPENTELEMETRY");
        } else if (contains(p, "zipkin", "tracing", "distributed trace")) {
            intent.setObservability("ZIPKIN");
        } else if (contains(p, "prometheus", "metrics", "monitoring")) {
            intent.setObservability("PROMETHEUS");
        } else if (contains(p, "actuator", "health check", "health endpoint")) {
            intent.setObservability("ACTUATOR");
        }

        // ── Architecture ──────────────────────────────────────────────────────
        if (contains(p, "cqrs", "command query")) {
            intent.setArchitecture("CQRS");
        } else if (contains(p, "hexagonal", "ports and adapters", "clean architecture")) {
            intent.setArchitecture("HEXAGONAL");
        } else if (contains(p, "event-driven", "event driven", "event sourcing")
                || "KAFKA".equals(intent.getMessaging())
                || "RABBITMQ".equals(intent.getMessaging())) {
            intent.setArchitecture("EVENT_DRIVEN");
        }

        // ── Service type ──────────────────────────────────────────────────────
        if (contains(p, "auth service", "authentication", "login service", "user auth")) {
            intent.setServiceType("AUTH");
        } else if (contains(p, "payment", "billing", "invoice")) {
            intent.setServiceType("PAYMENT");
        } else if (contains(p, "order service", "order management")) {
            intent.setServiceType("ORDER");
        } else if (contains(p, "inventory", "stock", "warehouse")) {
            intent.setServiceType("INVENTORY");
        } else if (contains(p, "gateway", "api gateway", "reverse proxy")) {
            intent.setServiceType("GATEWAY");
        }

        // ── Language ──────────────────────────────────────────────────────────
        if (contains(p, "kotlin")) {
            intent.setLanguage("KOTLIN");
        }

        // ── Build tool ────────────────────────────────────────────────────────
        if (contains(p, "gradle")) {
            intent.setBuildTool("GRADLE");
        }

        return intent;
    }

    private String deriveServiceName(String p) {
        // Try to extract a meaningful service name from the prompt
        if (contains(p, "kafka")) return "kafka-consumer-service";
        if (contains(p, "order")) return "order-service";
        if (contains(p, "payment", "billing")) return "payment-service";
        if (contains(p, "inventory", "stock")) return "inventory-service";
        if (contains(p, "user", "auth", "login")) return "user-service";
        if (contains(p, "product")) return "product-service";
        if (contains(p, "notification")) return "notification-service";
        if (contains(p, "gateway")) return "api-gateway-service";
        if (contains(p, "student")) return "student-service";
        if (contains(p, "employee")) return "employee-service";
        if (contains(p, "customer")) return "customer-service";
        if (contains(p, "rabbitmq", "rabbit")) return "messaging-service";

        // Extract first noun-like word from prompt
        String[] words = p.split("\\s+");
        for (String word : words) {
            if (word.length() > 3 && word.matches("[a-z]+")) {
                return word + "-service";
            }
        }
        return "microservice";
    }

    // ─── Phase 2: Minimal LLM prompt for name enrichment only ────────────────

    private String buildMinimalSystemPrompt() {
        return """
                Extract the microservice name from the user prompt.
                Return ONLY this JSON object, nothing else:
                {"serviceName":"kebab-case-name","packageName":"com.example.domain"}
                Rules:
                - serviceName: lowercase kebab-case, end with -service
                - packageName: com.example.<main-domain-word>
                - Return ONLY the JSON, no explanation, no markdown
                """;
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private boolean contains(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String extractJsonObject(String response) {
        if (response == null) return null;
        String s = response.trim();

        // Strip markdown fences
        if (s.contains("```")) {
            int start = s.indexOf("```");
            int end   = s.lastIndexOf("```");
            if (end > start) {
                s = s.substring(start + 3, end).trim();
                if (s.startsWith("json")) s = s.substring(4).trim();
            }
        }

        // Extract first JSON object
        int open = s.indexOf('{');
        int close = s.lastIndexOf('}');
        if (open != -1 && close > open) {
            return s.substring(open, close + 1);
        }
        return null;
    }

    private String toKebabCase(String input) {
        return input.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
