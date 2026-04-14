package com.microgen.orchestrator.engine;

import com.microgen.orchestrator.model.IntentModel;
import com.microgen.orchestrator.model.EntityModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeGenerationEngine {

    public Map<String, String> generateProject(IntentModel intent, String llmOutput) {
        Map<String, String> files = new HashMap<>();
        String packagePath = "src/main/java/" + intent.getPackageName().replace('.', '/') + "/";

        // 1. Generate guaranteed fallback files FIRST — these are always correct
        String fallbackPom = generateFallbackPom(intent);
        files.put("pom.xml", fallbackPom);
        files.put(packagePath + "Application.java", generateMain(intent));
        files.put("src/main/resources/application.yml", generateApplicationYml(intent));

        // 2. Parse LLM output — LLM files override fallbacks EXCEPT pom.xml
        //    pom.xml is only overridden if the LLM version contains all required deps
        parseAndWriteLlmOutput(intent, llmOutput, files, fallbackPom);

        // 3. Auth scaffolding fallback
        if ("JWT".equalsIgnoreCase(intent.getAuth())) {
            files.putIfAbsent(packagePath + "security/SecurityConfig.java", generateSecurityConfig(intent));
            files.putIfAbsent(packagePath + "security/JwtUtils.java", generateJwtUtils(intent));
            files.putIfAbsent(packagePath + "controller/AuthController.java", generateAuthController(intent));
            files.putIfAbsent(packagePath + "model/AuthRequest.java", generateAuthRequest(intent));
        }

        // 4. Persistence scaffolding fallback
        if ("JPA".equalsIgnoreCase(intent.getPersistence()) && intent.getEntities() != null) {
            for (EntityModel entity : intent.getEntities()) {
                files.putIfAbsent(packagePath + "model/" + entity.getName() + ".java", generateJpaEntity(intent, entity));
                files.putIfAbsent(packagePath + "repository/" + entity.getName() + "Repository.java", generateJpaRepository(intent, entity));
            }
        } else if ("MYBATIS".equalsIgnoreCase(intent.getPersistence())) {
            files.putIfAbsent(packagePath + "model/User.java", generateUserEntity(intent));
            files.putIfAbsent(packagePath + "mapper/UserMapper.java", generateUserMapper(intent));
            files.putIfAbsent("src/main/resources/schema.sql", generateSchema(intent));
        }

        if ("OAUTH2".equalsIgnoreCase(intent.getAuth())) {
            files.putIfAbsent(packagePath + "security/OAuth2SecurityConfig.java", generateOAuth2SecurityConfig(intent));
        }

        // 5. DevOps files
        files.putIfAbsent("Dockerfile", generateDockerfile(intent));
        files.putIfAbsent("docker-compose.yml", generateDockerCompose(intent));
        files.putIfAbsent(".github/workflows/ci.yml", generateCiWorkflow(intent));

        return files;
    }

    // ─── LLM Output Parser ────────────────────────────────────────────────────

    private void parseAndWriteLlmOutput(IntentModel intent, String llmOutput, Map<String, String> files,
                                        String fallbackPom) {
        final String packagePath = "src/main/java/" + intent.getPackageName().replace('.', '/') + "/";

        final List<String> rootFiles = Arrays.asList(
                "pom.xml", "Dockerfile", "docker-compose.yml", "docker-compose.yaml"
        );

        final List<String> resourceFiles = Arrays.asList(
                "application.yml", "application.properties", "application-dev.yml",
                "application-prod.yml", "logback.xml", "logback-spring.xml", "banner.txt"
        );

        String[] parts = llmOutput.split("/// START FILE: ");
        int parsedCount = 0;

        for (String part : parts) {
            if (part.trim().isEmpty()) continue;

            int newline = part.indexOf("\n");
            if (newline == -1) continue;

            String llmFilename = part.substring(0, newline).trim();
            String content     = part.substring(newline + 1);

            if (content.contains("/// END FILE")) {
                content = content.substring(0, content.indexOf("/// END FILE")).trim();
            }
            if (content.isBlank()) continue;

            String basename = llmFilename.contains("/")
                    ? llmFilename.substring(llmFilename.lastIndexOf('/') + 1)
                    : llmFilename;

            if (basename.isBlank() || basename.contains(" ")) continue;

            String resolvedPath = resolveToCanonicalPath(basename, content, intent, packagePath, rootFiles, resourceFiles);

            // Special handling for pom.xml: only use LLM version if it's more complete
            // than our fallback (i.e. it actually contains the required dependencies)
            if ("pom.xml".equals(resolvedPath)) {
                if (isPomMoreComplete(content, fallbackPom)) {
                    files.put(resolvedPath, content);
                    System.out.println("✅ Using LLM pom.xml (more complete than fallback)");
                } else {
                    System.out.println("⚠️ LLM pom.xml rejected (missing deps) — keeping fallback");
                }
                parsedCount++;
                continue;
            }

            // For application.yml: merge LLM version on top of fallback
            if ("src/main/resources/application.yml".equals(resolvedPath)) {
                // LLM version takes precedence for yml — it knows the full config
                files.put(resolvedPath, content);
                parsedCount++;
                System.out.println("✅ [" + llmFilename + "] → [" + resolvedPath + "]");
                continue;
            }

            files.put(resolvedPath, content);
            parsedCount++;
            System.out.println("✅ [" + llmFilename + "] → [" + resolvedPath + "]");
        }

        if (parsedCount == 0) {
            System.err.println("⚠️ No /// START FILE markers found — raw dump fallback");
            files.put(packagePath + "service/GeneratedService.java",
                    "package " + intent.getPackageName() + ".service;\n" +
                    "import org.springframework.stereotype.Service;\n" +
                    "@Service\npublic class GeneratedService {}\n" +
                    "/* RAW OUTPUT:\n" + llmOutput + "\n*/");
        }
    }

    /**
     * Returns true if the LLM-generated pom.xml is more complete than the fallback.
     * Checks that it contains the key dependency markers from the fallback.
     */
    private boolean isPomMoreComplete(String llmPom, String fallbackPom) {
        if (llmPom == null || llmPom.length() < 200) return false;
        // Must have Spring Boot parent
        if (!llmPom.contains("spring-boot-starter-parent")) return false;
        // Must have at least as many dependencies as fallback
        long llmDepCount = llmPom.split("<dependency>").length;
        long fallbackDepCount = fallbackPom.split("<dependency>").length;
        return llmDepCount >= fallbackDepCount;
    }

    /**
     * Resolves a bare filename to its canonical zip path.
     *
     * For Java files the algorithm is:
     *   1. Read the `package` declaration from the file content
     *   2. Strip the base package prefix to get the sub-package segment
     *      e.g. base=com.example.kafka, declared=com.example.kafka.service → sub=service
     *   3. Final path = src/main/java/{base-as-path}/{sub}/{Filename}.java
     *
     * This guarantees ALL Java files land under src/main/java/com/...
     * regardless of what path the LLM emitted.
     */
    private String resolveToCanonicalPath(String basename, String content, IntentModel intent,
                                          String packagePath, List<String> rootFiles,
                                          List<String> resourceFiles) {
        // ── 1. Root-level files (pom.xml, Dockerfile, docker-compose) ─────────
        if (rootFiles.contains(basename)) {
            return basename;
        }

        // ── 2. Resource files → src/main/resources/ ───────────────────────────
        if (resourceFiles.contains(basename)) {
            return "src/main/resources/" + basename;
        }

        // ── 2. Java files ─────────────────────────────────────────────────────
        if (basename.endsWith(".java")) {
            String declaredPackage = extractPackageDeclaration(content);
            String subPackage;

            if (!declaredPackage.isEmpty()) {
                if (declaredPackage.startsWith(intent.getPackageName())) {
                    // Strip the base package to get just the sub-package segment
                    // e.g. com.example.kafka.service → service
                    String remainder = declaredPackage.substring(intent.getPackageName().length());
                    // remainder is either "" (root of base package) or ".service" etc.
                    subPackage = remainder.isEmpty() ? "" : remainder.substring(1).replace('.', '/') + "/";
                } else {
                    // LLM used a completely different package — infer from filename
                    subPackage = resolveSubPackage(basename);
                }
            } else {
                // No package declaration — infer from filename
                subPackage = resolveSubPackage(basename);
            }

            return packagePath + subPackage + basename;
        }

        // ── 3. Resource files ─────────────────────────────────────────────────
        if (basename.endsWith(".yml") || basename.endsWith(".yaml")
                || basename.endsWith(".properties") || basename.endsWith(".sql")) {
            return "src/main/resources/" + basename;
        }

        // ── 4. XML ────────────────────────────────────────────────────────────
        if (basename.endsWith(".xml")) {
            return "src/main/resources/" + basename;
        }

        // ── 5. Everything else → root ─────────────────────────────────────────
        return basename;
    }

    /** Reads the package statement from Java source content. */
    private String extractPackageDeclaration(String content) {
        for (String line : content.split("\n")) {
            String t = line.trim();
            // Found it
            if (t.startsWith("package ") && t.endsWith(";")) {
                return t.substring("package ".length(), t.length() - 1).trim();
            }
            // Skip blank lines and comments — keep scanning
            if (t.isEmpty() || t.startsWith("//") || t.startsWith("/*") || t.startsWith("*")) {
                continue;
            }
            // Hit a real code line that isn't a package declaration — stop
            break;
        }
        return "";
    }

    private String resolveSubPackage(String filename) {
        if (filename.equals("Application.java"))                                        return "";
        if (filename.endsWith("Controller.java"))                                       return "controller/";
        if (filename.endsWith("Repository.java"))                                       return "repository/";
        if (filename.endsWith("Service.java"))                                          return "service/";
        if (filename.endsWith("Config.java") || filename.endsWith("Configuration.java")) return "config/";
        if (filename.endsWith("Exception.java") || filename.endsWith("Handler.java")
                || filename.endsWith("Advice.java"))                                    return "exception/";
        if (filename.endsWith("Util.java") || filename.endsWith("Utils.java")
                || filename.endsWith("Helper.java"))                                    return "util/";
        if (filename.endsWith("DTO.java") || filename.endsWith("Request.java")
                || filename.endsWith("Response.java") || filename.endsWith("Model.java")
                || filename.endsWith("Entity.java"))                                    return "model/";
        if (filename.endsWith("Mapper.java"))                                           return "mapper/";
        if (filename.endsWith("Filter.java") || filename.endsWith("Interceptor.java")
                || filename.endsWith("Security.java"))                                  return "security/";
        if (filename.endsWith("Consumer.java") || filename.endsWith("Producer.java")
                || filename.endsWith("Listener.java"))                                  return "messaging/";
        return "service/";
    }

    // ─── Fallback Generators ──────────────────────────────────────────────────

    private String generateFallbackPom(IntentModel intent) {
        StringBuilder deps = new StringBuilder();

        // Always include web + lombok + validation
        deps.append("""
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                </dependency>
                <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok</artifactId>
                    <optional>true</optional>
                </dependency>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-validation</artifactId>
                </dependency>
                """);

        // Persistence
        if ("JPA".equalsIgnoreCase(intent.getPersistence()) || "R2DBC".equalsIgnoreCase(intent.getPersistence())) {
            deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-jpa</artifactId>
                    </dependency>
                    """);
        } else if ("MYBATIS".equalsIgnoreCase(intent.getPersistence())) {
            deps.append("""
                    <dependency>
                        <groupId>org.mybatis.spring.boot</groupId>
                        <artifactId>mybatis-spring-boot-starter</artifactId>
                        <version>3.0.3</version>
                    </dependency>
                    """);
        }

        // Database
        switch (intent.getDatabase().toUpperCase()) {
            case "MYSQL" -> deps.append("""
                    <dependency>
                        <groupId>com.mysql</groupId>
                        <artifactId>mysql-connector-j</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                    """);
            case "POSTGRESQL" -> deps.append("""
                    <dependency>
                        <groupId>org.postgresql</groupId>
                        <artifactId>postgresql</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                    """);
            case "MONGODB" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-mongodb</artifactId>
                    </dependency>
                    """);
            case "REDIS" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-redis</artifactId>
                    </dependency>
                    """);
            default -> deps.append("""
                    <dependency>
                        <groupId>com.h2database</groupId>
                        <artifactId>h2</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                    """);
        }

        // Auth
        switch (intent.getAuth().toUpperCase()) {
            case "JWT" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-security</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.jsonwebtoken</groupId>
                        <artifactId>jjwt-api</artifactId>
                        <version>0.11.5</version>
                    </dependency>
                    <dependency>
                        <groupId>io.jsonwebtoken</groupId>
                        <artifactId>jjwt-impl</artifactId>
                        <version>0.11.5</version>
                        <scope>runtime</scope>
                    </dependency>
                    """);
            case "OAUTH2" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-security</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
                    </dependency>
                    """);
            case "BASIC", "API_KEY" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-security</artifactId>
                    </dependency>
                    """);
        }

        // Messaging
        switch (intent.getMessaging().toUpperCase()) {
            case "KAFKA" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.kafka</groupId>
                        <artifactId>spring-kafka</artifactId>
                    </dependency>
                    """);
            case "RABBITMQ" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-amqp</artifactId>
                    </dependency>
                    """);
            case "SQS" -> deps.append("""
                    <dependency>
                        <groupId>io.awspring.cloud</groupId>
                        <artifactId>spring-cloud-aws-starter-sqs</artifactId>
                        <version>3.1.1</version>
                    </dependency>
                    """);
            case "ACTIVEMQ" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-activemq</artifactId>
                    </dependency>
                    """);
        }

        // Cache
        switch (intent.getCache().toUpperCase()) {
            case "REDIS" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-cache</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-redis</artifactId>
                    </dependency>
                    """);
            case "CAFFEINE" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-cache</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.github.ben-manes.caffeine</groupId>
                        <artifactId>caffeine</artifactId>
                    </dependency>
                    """);
            case "HAZELCAST" -> deps.append("""
                    <dependency>
                        <groupId>com.hazelcast</groupId>
                        <artifactId>hazelcast-spring</artifactId>
                    </dependency>
                    """);
        }

        // Observability
        switch (intent.getObservability().toUpperCase()) {
            case "ACTUATOR", "PROMETHEUS" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.micrometer</groupId>
                        <artifactId>micrometer-registry-prometheus</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                    """);
            case "ZIPKIN" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.micrometer</groupId>
                        <artifactId>micrometer-tracing-bridge-brave</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.zipkin.reporter2</groupId>
                        <artifactId>zipkin-reporter-brave</artifactId>
                    </dependency>
                    """);
            case "OPENTELEMETRY" -> deps.append("""
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>io.micrometer</groupId>
                        <artifactId>micrometer-tracing-bridge-otel</artifactId>
                    </dependency>
                    """);
        }

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.4.1</version>
                    </parent>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <name>%s</name>
                    <properties>
                        <java.version>21</java.version>
                    </properties>
                    <dependencies>
                %s
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """.formatted(intent.getPackageName(), intent.getServiceName(), intent.getServiceName(), deps);
    }

    private String generateMain(IntentModel intent) {
        return """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;

                @SpringBootApplication
                public class Application {
                    public static void main(String[] args) {
                        SpringApplication.run(Application.class, args);
                    }
                }
                """.formatted(intent.getPackageName());
    }

    private String generateApplicationYml(IntentModel intent) {
        String dbUrl, dbDriver, dbUser, dbPass;

        if ("MYSQL".equalsIgnoreCase(intent.getDatabase())) {
            dbUrl    = "jdbc:mysql://localhost:3306/" + intent.getServiceName().replace("-", "_");
            dbDriver = "com.mysql.cj.jdbc.Driver";
            dbUser   = "root";
            dbPass   = "root";
        } else if ("POSTGRESQL".equalsIgnoreCase(intent.getDatabase())) {
            dbUrl    = "jdbc:postgresql://localhost:5432/" + intent.getServiceName().replace("-", "_");
            dbDriver = "org.postgresql.Driver";
            dbUser   = "postgres";
            dbPass   = "postgres";
        } else {
            dbUrl    = "jdbc:h2:mem:" + intent.getServiceName().replace("-", "_");
            dbDriver = "org.h2.Driver";
            dbUser   = "sa";
            dbPass   = "password";
        }

        return """
                server:
                  port: %d
                spring:
                  application:
                    name: %s
                  datasource:
                    url: %s
                    driver-class-name: %s
                    username: %s
                    password: %s
                  jpa:
                    hibernate:
                      ddl-auto: update
                    show-sql: false
                """.formatted(intent.getPort(), intent.getServiceName(), dbUrl, dbDriver, dbUser, dbPass);
    }

    private String generateDockerfile(IntentModel intent) {
        return """
                FROM eclipse-temurin:21-jdk-alpine
                VOLUME /tmp
                COPY target/*.jar app.jar
                ENTRYPOINT ["java","-jar","/app.jar"]
                """;
    }

    private String generateDockerCompose(IntentModel intent) {
        String dbName = intent.getServiceName().replace("-", "_");

        if ("MYSQL".equalsIgnoreCase(intent.getDatabase())) {
            return """
                    version: '3.8'
                    services:
                      app:
                        build: .
                        ports:
                          - "%d:%d"
                        depends_on:
                          - db
                        environment:
                          - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/%s
                      db:
                        image: mysql:8.0
                        environment:
                          - MYSQL_ROOT_PASSWORD=root
                          - MYSQL_DATABASE=%s
                    """.formatted(intent.getPort(), intent.getPort(), dbName, dbName);
        } else if ("POSTGRESQL".equalsIgnoreCase(intent.getDatabase())) {
            return """
                    version: '3.8'
                    services:
                      app:
                        build: .
                        ports:
                          - "%d:%d"
                        depends_on:
                          - db
                        environment:
                          - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/%s
                      db:
                        image: postgres:15-alpine
                        environment:
                          - POSTGRES_PASSWORD=postgres
                          - POSTGRES_DB=%s
                    """.formatted(intent.getPort(), intent.getPort(), dbName, dbName);
        }

        return """
                version: '3.8'
                services:
                  app:
                    build: .
                    ports:
                      - "%d:%d"
                """.formatted(intent.getPort(), intent.getPort());
    }

    private String generateCiWorkflow(IntentModel intent) {
        return """
                name: Java CI with Maven
                on: [push]
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                    - uses: actions/checkout@v3
                    - name: Set up JDK 21
                      uses: actions/setup-java@v3
                      with:
                        java-version: '21'
                        distribution: 'temurin'
                        cache: 'maven'
                    - name: Build with Maven
                      run: mvn -B package --file pom.xml
                """;
    }

    private String generateSecurityConfig(IntentModel intent) {
        return """
                package %s.security;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.config.annotation.web.builders.HttpSecurity;
                import org.springframework.security.web.SecurityFilterChain;
                @Configuration
                public class SecurityConfig {
                    @Bean
                    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                        http.csrf(csrf -> csrf.disable())
                            .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/api/auth/**").permitAll()
                                .anyRequest().authenticated());
                        return http.build();
                    }
                }
                """.formatted(intent.getPackageName());
    }

    private String generateJwtUtils(IntentModel intent) {
        return """
                package %s.security;
                import io.jsonwebtoken.Jwts;
                import io.jsonwebtoken.SignatureAlgorithm;
                import org.springframework.stereotype.Component;
                import java.util.Date;
                @Component
                public class JwtUtils {
                    private final String jwtKey = System.getenv().getOrDefault("JWT_SECRET", "dev-key-change-me");
                    public String generateToken(String username) {
                        return Jwts.builder()
                            .setSubject(username)
                            .setIssuedAt(new Date())
                            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                            .signWith(SignatureAlgorithm.HS256, jwtKey)
                            .compact();
                    }
                }
                """.formatted(intent.getPackageName());
    }

    private String generateAuthController(IntentModel intent) {
        return """
                package %s.controller;
                import %s.model.AuthRequest;
                import %s.security.JwtUtils;
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping("/api/auth")
                public class AuthController {
                    private final JwtUtils jwtUtils;
                    public AuthController(JwtUtils jwtUtils) { this.jwtUtils = jwtUtils; }
                    @PostMapping("/login")
                    public String login(@RequestBody AuthRequest request) {
                        return jwtUtils.generateToken(request.getUsername());
                    }
                }
                """.formatted(intent.getPackageName(), intent.getPackageName(), intent.getPackageName());
    }

    private String generateAuthRequest(IntentModel intent) {
        return """
                package %s.model;
                import lombok.Data;
                @Data
                public class AuthRequest {
                    private String username;
                    private String password;
                }
                """.formatted(intent.getPackageName());
    }

    private String generateUserEntity(IntentModel intent) {
        return """
                package %s.model;
                import lombok.Data;
                @Data
                public class User {
                    private Long id;
                    private String username;
                    private String password;
                }
                """.formatted(intent.getPackageName());
    }

    private String generateUserMapper(IntentModel intent) {
        return """
                package %s.mapper;
                import %s.model.User;
                import org.apache.ibatis.annotations.*;
                import java.util.List;
                @Mapper
                public interface UserMapper {
                    @Select("SELECT * FROM users")
                    List<User> findAll();
                    @Insert("INSERT INTO users(username, password) VALUES(#{username}, #{password})")
                    @Options(useGeneratedKeys = true, keyProperty = "id")
                    void insert(User user);
                }
                """.formatted(intent.getPackageName(), intent.getPackageName());
    }

    private String generateSchema(IntentModel intent) {
        return """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL
                );
                """;
    }

    private String generateJpaEntity(IntentModel intent, EntityModel entity) {
        StringBuilder fields = new StringBuilder();
        for (EntityModel.FieldModel field : entity.getFields()) {
            fields.append("    private %s %s;\n".formatted(field.getType(), field.getName()));
        }
        return """
                package %s.model;
                import jakarta.persistence.*;
                import lombok.Data;
                @Entity
                @Table(name = "%s")
                @Data
                public class %s {
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    private Long id;
                    %s
                }
                """.formatted(intent.getPackageName(), entity.getName().toLowerCase() + "s",
                entity.getName(), fields.toString());
    }

    private String generateJpaRepository(IntentModel intent, EntityModel entity) {
        return """
                package %s.repository;
                import %s.model.%s;
                import org.springframework.data.jpa.repository.JpaRepository;
                import org.springframework.stereotype.Repository;
                @Repository
                public interface %sRepository extends JpaRepository<%s, Long> {}
                """.formatted(intent.getPackageName(), intent.getPackageName(), entity.getName(),
                entity.getName(), entity.getName());
    }

    private String generateOAuth2SecurityConfig(IntentModel intent) {
        return """
                package %s.security;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import org.springframework.security.config.annotation.web.builders.HttpSecurity;
                import org.springframework.security.web.SecurityFilterChain;
                import static org.springframework.security.config.Customizer.withDefaults;
                @Configuration
                public class OAuth2SecurityConfig {
                    @Bean
                    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                        http.csrf(csrf -> csrf.disable())
                            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                            .oauth2Login(withDefaults());
                        return http.build();
                    }
                }
                """.formatted(intent.getPackageName());
    }
}
