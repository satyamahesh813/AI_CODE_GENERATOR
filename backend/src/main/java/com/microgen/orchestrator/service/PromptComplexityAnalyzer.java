package com.microgen.orchestrator.service;

import com.microgen.orchestrator.model.IntentModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes ANY user prompt for complexity and production-grade patterns.
 * Technology-agnostic — works for REST APIs, Kafka, gRPC, batch jobs, etc.
 */
@Service
public class PromptComplexityAnalyzer {

    public enum ComplexityLevel { SIMPLE, STANDARD, ADVANCED, PRODUCTION_GRADE }

    public record AnalysisResult(
            ComplexityLevel level,
            List<String> detectedPatterns,
            String productionGuidance
    ) {}

    public AnalysisResult analyze(String prompt, IntentModel intent) {
        String p = prompt.toLowerCase();
        List<String> patterns = new ArrayList<>();

        // ── Universal pattern detection ───────────────────────────────────────

        // Reliability patterns
        boolean hasRetry         = contains(p, "retry", "retries", "retry logic", "retry strategy", "backoff", "exponential");
        boolean hasDLQ           = contains(p, "dead letter", "dlq", "dead-letter");
        boolean hasCircuitBreaker= contains(p, "circuit breaker", "circuit-breaker", "resilience4j", "hystrix");
        boolean hasIdempotency   = contains(p, "idempoten", "deduplication", "duplicate", "exactly-once", "exactly once");
        boolean hasTransaction   = contains(p, "transaction", "transactional", "acid", "rollback", "commit");
        boolean hasFaultTolerance= contains(p, "fault toleran", "fault-toleran", "resilient", "failure scenario");

        // Architecture patterns
        boolean hasHexagonal     = contains(p, "hexagonal", "ports and adapters", "clean architecture");
        boolean hasCQRS          = contains(p, "cqrs", "command query");
        boolean hasEventSourcing = contains(p, "event sourcing", "event-sourcing");
        boolean hasOutbox        = contains(p, "outbox", "inbox pattern", "transactional outbox");
        boolean hasSaga          = contains(p, "saga", "choreography", "orchestration pattern");
        boolean hasMicroservice  = contains(p, "microservice", "micro-service", "distributed");

        // Data patterns
        boolean hasCaching       = contains(p, "cache", "caching", "redis", "caffeine", "hazelcast");
        boolean hasPagination    = contains(p, "pagination", "pageable", "page size");
        boolean hasSearch        = contains(p, "search", "elasticsearch", "full-text");
        boolean hasAudit         = contains(p, "audit", "audit log", "audit trail");
        boolean hasSoftDelete    = contains(p, "soft delete", "soft-delete", "is_deleted");

        // Security patterns
        boolean hasAuth          = contains(p, "jwt", "oauth", "authentication", "authorization", "security", "sasl", "ssl", "api key");
        boolean hasRateLimit     = contains(p, "rate limit", "rate-limit", "throttl");
        boolean hasEncryption    = contains(p, "encrypt", "decrypt", "aes", "rsa");
        boolean hasValidation    = contains(p, "validat", "javax.validation", "jakarta.validation", "@valid");

        // Observability patterns
        boolean hasMetrics       = contains(p, "metric", "prometheus", "micrometer", "counter", "gauge", "timer");
        boolean hasTracing       = contains(p, "tracing", "opentelemetry", "otel", "zipkin", "jaeger", "correlation");
        boolean hasLogging       = contains(p, "structured log", "correlation id", "mdc", "log aggregat");
        boolean hasHealthCheck   = contains(p, "health check", "health endpoint", "actuator", "liveness", "readiness");

        // Messaging patterns
        boolean hasMessaging     = contains(p, "kafka", "rabbitmq", "sqs", "activemq", "message queue", "event stream", "topic", "consumer", "producer");
        boolean hasSchemaEvol    = contains(p, "schema evolution", "schema registry", "backward compat", "forward compat");
        boolean hasBackpressure  = contains(p, "backpressure", "back pressure", "pause", "resume consumption", "flow control");

        // Concurrency patterns
        boolean hasConcurrency   = contains(p, "concurrent", "thread-safe", "async", "parallel", "non-blocking", "reactive", "webflux");
        boolean hasLocking       = contains(p, "optimistic lock", "pessimistic lock", "distributed lock", "redis lock");

        // Deployment patterns
        boolean hasDocker        = contains(p, "docker", "container", "docker-compose", "kubernetes", "k8s");
        boolean hasCI            = contains(p, "ci/cd", "ci cd", "github actions", "pipeline", "jenkins");
        boolean hasScaling       = contains(p, "scal", "horizontal", "load balanc", "high availab");

        // Testing patterns
        boolean hasTesting       = contains(p, "unit test", "integration test", "test", "junit", "mockito", "testcontainer");
        boolean hasChaos         = contains(p, "chaos", "fault injection", "failure simulation");

        // Production indicators
        boolean isProductionGrade = contains(p, "production", "production-grade", "production-ready",
                "high-scale", "enterprise", "fault-tolerant", "millions of", "high availability");

        // ── Register patterns ─────────────────────────────────────────────────
        if (hasRetry)          patterns.add("RETRY_WITH_BACKOFF");
        if (hasDLQ)            patterns.add("DEAD_LETTER_QUEUE");
        if (hasCircuitBreaker) patterns.add("CIRCUIT_BREAKER");
        if (hasIdempotency)    patterns.add("IDEMPOTENCY");
        if (hasTransaction)    patterns.add("TRANSACTIONS");
        if (hasFaultTolerance) patterns.add("FAULT_TOLERANCE");
        if (hasHexagonal)      patterns.add("HEXAGONAL_ARCHITECTURE");
        if (hasCQRS)           patterns.add("CQRS");
        if (hasEventSourcing)  patterns.add("EVENT_SOURCING");
        if (hasOutbox)         patterns.add("OUTBOX_PATTERN");
        if (hasSaga)           patterns.add("SAGA_PATTERN");
        if (hasCaching)        patterns.add("CACHING");
        if (hasAudit)          patterns.add("AUDIT_LOGGING");
        if (hasSoftDelete)     patterns.add("SOFT_DELETE");
        if (hasAuth)           patterns.add("SECURITY");
        if (hasRateLimit)      patterns.add("RATE_LIMITING");
        if (hasMetrics)        patterns.add("METRICS");
        if (hasTracing)        patterns.add("DISTRIBUTED_TRACING");
        if (hasHealthCheck)    patterns.add("HEALTH_CHECKS");
        if (hasMessaging)      patterns.add("MESSAGING");
        if (hasSchemaEvol)     patterns.add("SCHEMA_EVOLUTION");
        if (hasBackpressure)   patterns.add("BACKPRESSURE");
        if (hasConcurrency)    patterns.add("CONCURRENCY");
        if (hasLocking)        patterns.add("DISTRIBUTED_LOCKING");
        if (hasDocker)         patterns.add("CONTAINERIZATION");
        if (hasTesting)        patterns.add("TESTING");
        if (hasChaos)          patterns.add("CHAOS_RESILIENCE");
        if (isProductionGrade) patterns.add("PRODUCTION_GRADE");

        // ── Complexity level ──────────────────────────────────────────────────
        ComplexityLevel level;
        int score = patterns.size() + (isProductionGrade ? 5 : 0);
        if (score >= 8)      level = ComplexityLevel.PRODUCTION_GRADE;
        else if (score >= 4) level = ComplexityLevel.ADVANCED;
        else if (score >= 2) level = ComplexityLevel.STANDARD;
        else                 level = ComplexityLevel.SIMPLE;

        // ── Build production guidance ─────────────────────────────────────────
        StringBuilder g = new StringBuilder();

        if (level == ComplexityLevel.PRODUCTION_GRADE || level == ComplexityLevel.ADVANCED) {
            g.append("PRODUCTION-GRADE REQUIREMENTS DETECTED. Implement ALL of the following:\n\n");
        }

        if (hasRetry) g.append(retryGuidance());
        if (hasDLQ) g.append(dlqGuidance(intent));
        if (hasCircuitBreaker) g.append(circuitBreakerGuidance());
        if (hasIdempotency) g.append(idempotencyGuidance());
        if (hasTransaction) g.append(transactionGuidance());
        if (hasOutbox) g.append(outboxGuidance());
        if (hasSaga) g.append(sagaGuidance());
        if (hasCaching) g.append(cachingGuidance(intent));
        if (hasAudit) g.append(auditGuidance());
        if (hasAuth) g.append(securityGuidance(intent));
        if (hasRateLimit) g.append(rateLimitGuidance());
        if (hasMetrics) g.append(metricsGuidance());
        if (hasTracing) g.append(tracingGuidance());
        if (hasHealthCheck) g.append(healthCheckGuidance());
        if (hasBackpressure) g.append(backpressureGuidance());
        if (hasConcurrency) g.append(concurrencyGuidance());
        if (hasLocking) g.append(lockingGuidance());
        if (hasSchemaEvol) g.append(schemaEvolutionGuidance());
        if (hasDocker) g.append(dockerGuidance(intent));
        if (hasTesting) g.append(testingGuidance());
        if (hasChaos) g.append(chaosGuidance());
        if (hasHexagonal) g.append(hexagonalGuidance(intent));
        if (hasCQRS) g.append(cqrsGuidance());

        if (level == ComplexityLevel.PRODUCTION_GRADE) {
            g.append(productionChecklistGuidance());
        }

        return new AnalysisResult(level, patterns, g.toString());
    }

    // ─── Pattern-specific guidance (technology-agnostic) ─────────────────────

    private String retryGuidance() {
        return """
            RETRY STRATEGY:
            - Implement exponential backoff: initial=100ms, multiplier=2, max=30s
            - Max attempts: 3 immediate retries, then escalate
            - Use Spring Retry @Retryable or Resilience4j Retry
            - For messaging: use retry topics (retry-5s, retry-1m, retry-10m) before DLQ
            - Log each retry attempt with attempt number, error, and next retry time
            - Distinguish retryable errors (transient) from non-retryable (validation, poison)
            """;
    }

    private String dlqGuidance(IntentModel intent) {
        return """
            DEAD LETTER QUEUE:
            - DLQ payload must include: originalMessage, errorMessage, stackTrace, timestamp, retryCount, sourceSystem
            - DLQ topic/queue name = original + ".DLT" or "-dlq"
            - Never lose a message — DLQ is the safety net
            - Provide replay API: POST /admin/dlq/replay/{id}
            - Track DLQ messages in DB with status: PENDING_REPLAY, REPLAYED, QUARANTINED
            """;
    }

    private String circuitBreakerGuidance() {
        return """
            CIRCUIT BREAKER (Resilience4j):
            - Add resilience4j-spring-boot3 dependency
            - @CircuitBreaker(name = "downstream", fallbackMethod = "fallback")
            - Config: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=30s
            - Fallback: return cached result or graceful degradation response
            - Expose circuit breaker state via /actuator/circuitbreakers
            """;
    }

    private String idempotencyGuidance() {
        return """
            IDEMPOTENCY:
            - ProcessedEvent entity: eventId (PK/unique), processedAt, status, checksum
            - Before processing: SELECT WHERE eventId = ? — if exists, skip and return cached result
            - After processing: INSERT eventId atomically (use INSERT IGNORE or ON CONFLICT DO NOTHING)
            - Use Redis SETNX for fast in-memory check (TTL = 24h), DB for persistence
            - IdempotencyService: isProcessed(id), markProcessed(id, result), getResult(id)
            - Return same response for duplicate requests (idempotent response caching)
            """;
    }

    private String transactionGuidance() {
        return """
            TRANSACTION MANAGEMENT:
            - Use @Transactional on service methods that modify multiple resources
            - Set appropriate isolation level: READ_COMMITTED for most cases
            - Set timeout: @Transactional(timeout = 30)
            - Handle TransactionSystemException and DataIntegrityViolationException
            - For distributed transactions: use Saga pattern or outbox pattern instead of 2PC
            - Never call @Transactional methods from within the same class (Spring proxy limitation)
            """;
    }

    private String outboxGuidance() {
        return """
            OUTBOX PATTERN:
            - OutboxEvent table: id (UUID), aggregateType, aggregateId, eventType, payload (JSON), status, createdAt, publishedAt
            - @Transactional: save domain entity + insert OutboxEvent in SAME transaction
            - OutboxPublisher: @Scheduled(fixedDelay=1000) polls PENDING events, publishes to broker
            - After successful publish: UPDATE status=PUBLISHED, publishedAt=now()
            - Retry failed publishes: status=FAILED, retryCount++
            - Guarantees at-least-once delivery even if broker is temporarily down
            """;
    }

    private String sagaGuidance() {
        return """
            SAGA PATTERN:
            - Choreography-based: each service publishes events, others react
            - Each step has a compensating transaction for rollback
            - SagaState entity: sagaId, currentStep, status (STARTED/COMPLETED/COMPENSATING/FAILED)
            - On failure: publish compensation events in reverse order
            - Use correlation ID to track saga across services
            """;
    }

    private String cachingGuidance(IntentModel intent) {
        String cacheType = intent.getCache() != null && !intent.getCache().equalsIgnoreCase("NONE")
                ? intent.getCache() : "Redis";
        return """
            CACHING (%s):
            - @EnableCaching on main application class
            - CacheConfig: configure CacheManager with TTL per cache region
            - @Cacheable(value = "entities", key = "#id") on read methods
            - @CacheEvict(value = "entities", key = "#id") on update/delete methods
            - @CachePut on create to populate cache immediately
            - Cache aside pattern: check cache → if miss, load from DB → put in cache
            - Never cache mutable shared state without proper eviction strategy
            """.formatted(cacheType);
    }

    private String auditGuidance() {
        return """
            AUDIT LOGGING:
            - AuditLog entity: id, entityType, entityId, action (CREATE/UPDATE/DELETE), oldValue, newValue, userId, timestamp, ipAddress
            - Use Spring Data JPA @EntityListeners(AuditingEntityListener.class)
            - @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy on entities
            - AuditService: log every state change with before/after snapshot
            - Store audit logs in separate table — never delete audit records
            """;
    }

    private String securityGuidance(IntentModel intent) {
        String authType = intent.getAuth() != null && !intent.getAuth().equalsIgnoreCase("NONE")
                ? intent.getAuth() : "JWT";
        return """
            SECURITY (%s):
            - SecurityConfig: configure SecurityFilterChain with proper endpoint protection
            - Never expose sensitive data in responses or logs
            - Validate all inputs: @Valid + custom validators for business rules
            - Use BCryptPasswordEncoder for password hashing (strength=12)
            - CORS: configure allowed origins explicitly — never use wildcard in production
            - HTTPS only in production: configure SSL in application.yml
            - Rate limiting on auth endpoints to prevent brute force
            """.formatted(authType);
    }

    private String rateLimitGuidance() {
        return """
            RATE LIMITING:
            - Use Bucket4j or Resilience4j RateLimiter
            - RateLimitFilter: OncePerRequestFilter, check per IP or per user
            - Config: 100 requests/minute per IP, 1000/minute per authenticated user
            - Return 429 Too Many Requests with Retry-After header
            - Store rate limit state in Redis for distributed rate limiting
            """;
    }

    private String metricsGuidance() {
        return """
            METRICS (Micrometer/Prometheus):
            - Inject MeterRegistry in services
            - Counter: requests.total (tags: endpoint, status, method)
            - Timer: request.duration (tags: endpoint)
            - Gauge: active.connections, queue.size
            - Custom business metrics: orders.processed, payments.failed, etc.
            - Expose: management.endpoints.web.exposure.include=health,info,prometheus,metrics
            - Add @Timed annotation on controller methods for automatic timing
            """;
    }

    private String tracingGuidance() {
        return """
            DISTRIBUTED TRACING:
            - Add micrometer-tracing-bridge-otel + opentelemetry-exporter-otlp
            - CorrelationId: extract from X-Correlation-ID header or generate UUID
            - Add correlationId to MDC: MDC.put("correlationId", id) at entry point
            - Use @Observed on service methods for automatic span creation
            - Propagate trace context across service calls via HTTP headers
            - application.yml: management.tracing.sampling.probability=1.0
            """;
    }

    private String healthCheckGuidance() {
        return """
            HEALTH CHECKS:
            - spring-boot-starter-actuator dependency
            - Custom HealthIndicator for each external dependency (DB, Redis, broker)
            - Liveness: /actuator/health/liveness — is app alive?
            - Readiness: /actuator/health/readiness — is app ready to serve traffic?
            - management.endpoint.health.show-details=always
            - management.health.db.enabled=true, management.health.redis.enabled=true
            """;
    }

    private String backpressureGuidance() {
        return """
            BACKPRESSURE:
            - Monitor in-flight request count with AtomicInteger
            - If count > maxConcurrent: pause upstream (pause Kafka consumer, return 503, etc.)
            - Resume when count drops below threshold
            - Use Semaphore for bounded concurrency: new Semaphore(maxConcurrent)
            - Expose current load via metrics gauge
            """;
    }

    private String concurrencyGuidance() {
        return """
            CONCURRENCY:
            - Use thread-safe collections: ConcurrentHashMap, CopyOnWriteArrayList
            - Avoid shared mutable state — prefer immutable objects
            - @Async methods: configure ThreadPoolTaskExecutor with bounded queue
            - Use CompletableFuture for async orchestration
            - Synchronize only the minimum critical section
            - Test with concurrent load to detect race conditions
            """;
    }

    private String lockingGuidance() {
        return """
            DISTRIBUTED LOCKING:
            - Use Redisson or Redis SETNX for distributed locks
            - Lock key: "lock:{resourceType}:{resourceId}"
            - Always set lock TTL to prevent deadlocks (e.g. 30s)
            - Use try-finally to always release lock
            - @DistributedLock custom annotation with AOP for clean usage
            - Optimistic locking in JPA: @Version field on entities
            """;
    }

    private String schemaEvolutionGuidance() {
        return """
            SCHEMA EVOLUTION:
            - @JsonIgnoreProperties(ignoreUnknown = true) on all DTOs/events
            - ObjectMapper: configure FAIL_ON_UNKNOWN_PROPERTIES = false
            - Use Optional<T> for fields that may not exist in older versions
            - Version field in events: if version > supported, log warning, process with defaults
            - FlexibleDeserializer: try typed deserialization, fall back to JsonNode on failure
            - Never remove fields from DTOs — mark as @Deprecated instead
            """;
    }

    private String dockerGuidance(IntentModel intent) {
        return """
            DOCKER & DEPLOYMENT:
            - Multi-stage Dockerfile: builder stage (Maven/Gradle) + runtime stage (JRE)
            - docker-compose.yml: app + all dependencies (DB, Redis, broker, etc.)
            - Health checks in docker-compose: healthcheck with retries
            - Environment variables for all config — no hardcoded values
            - .env file for local development secrets
            - Resource limits in docker-compose: mem_limit, cpus
            """;
    }

    private String testingGuidance() {
        return """
            TESTING STRATEGY:
            - Unit tests: test service layer with Mockito mocks, no Spring context
            - Integration tests: @SpringBootTest with @Testcontainers for real DB/Redis/broker
            - Use @DataJpaTest for repository layer tests
            - Test happy path + all error scenarios + edge cases
            - Verify retry behavior, DLQ routing, idempotency in integration tests
            - Aim for 80%+ coverage on business logic
            """;
    }

    private String chaosGuidance() {
        return """
            CHAOS/FAULT TOLERANCE:
            - Simulate broker unavailability: handle ConnectException with retry
            - Simulate DB failure: handle DataAccessException gracefully
            - Simulate slow downstream: configure timeouts on all HTTP/DB calls
            - Simulate duplicate delivery: idempotency check must handle this
            - All external calls must have: timeout + retry + circuit breaker + fallback
            """;
    }

    private String hexagonalGuidance(IntentModel intent) {
        return """
            HEXAGONAL ARCHITECTURE:
            Package structure:
            - domain/model/          → pure POJOs, no framework annotations
            - domain/port/in/        → use case interfaces (e.g. ProcessOrderUseCase)
            - domain/port/out/       → repository/gateway interfaces (e.g. OrderRepository)
            - domain/service/        → implements use case interfaces, pure business logic
            - infrastructure/web/    → REST controllers, call use cases via interfaces
            - infrastructure/persistence/ → JPA/Redis implementations of repository ports
            - infrastructure/messaging/   → Kafka/RabbitMQ adapters
            - infrastructure/config/ → Spring configuration classes
            - application/           → Application.java, @SpringBootApplication
            Rule: domain layer has ZERO framework imports (no Spring, no JPA annotations)
            """;
    }

    private String cqrsGuidance() {
        return """
            CQRS PATTERN:
            - Separate Command and Query models
            - Commands: CreateOrderCommand, UpdateOrderCommand (write side)
            - Queries: GetOrderQuery, ListOrdersQuery (read side)
            - CommandHandler: validates + executes command + publishes event
            - QueryHandler: reads from optimized read model (can be different DB/table)
            - CommandBus / QueryBus: route to correct handler
            - Read model can be denormalized for query performance
            """;
    }

    private String productionChecklistGuidance() {
        return """
            PRODUCTION CHECKLIST (implement ALL):
            ✓ All external calls have timeouts configured
            ✓ All secrets via environment variables (never hardcoded)
            ✓ Graceful shutdown: @PreDestroy, server.shutdown=graceful
            ✓ Connection pooling configured (HikariCP, Lettuce pool)
            ✓ Structured JSON logging with correlationId in every log line
            ✓ All endpoints return proper HTTP status codes
            ✓ Global exception handler (@ControllerAdvice) with consistent error response
            ✓ Input validation on all public APIs
            ✓ No sensitive data in logs (mask passwords, tokens, PII)
            ✓ Health check endpoints for liveness and readiness probes
            ✓ Metrics exposed for monitoring
            ✓ Docker-ready with proper ENTRYPOINT and health checks
            """;
    }

    private boolean contains(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
