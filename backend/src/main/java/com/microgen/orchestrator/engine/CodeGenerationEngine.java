package com.microgen.orchestrator.engine;

import com.microgen.orchestrator.model.IntentModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microgen.orchestrator.model.EntityModel;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeGenerationEngine {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public Map<String, String> generateProject(IntentModel intent, String llmOutput) {
    Map<String, String> files = new HashMap<>();
    String packagePath = "src/main/java/" + intent.getPackageName().replace('.', '/') + "/";

    // 1. pom.xml
    files.put("pom.xml", generatePomDynamic(intent, llmOutput));

    // 2. Main Application
    files.put(packagePath + "Application.java", generateMain(intent));

    // 3. application.yml
    files.put("src/main/resources/application.yml", generateApplicationYml(intent));

    // 4. Injected LLM Logic (Multi-file parsing)
    parseAndWriteLlmOutput(intent, llmOutput, files);

    // 5. Auth Logic (If JWT)
    if ("JWT".equalsIgnoreCase(intent.getAuth())) {
      files.put(packagePath + "security/SecurityConfig.java", generateSecurityConfig(intent));
      files.put(packagePath + "security/JwtUtils.java", generateJwtUtils(intent));
      files.put(packagePath + "controller/AuthController.java", generateAuthController(intent));
      files.put(packagePath + "model/AuthRequest.java", generateAuthRequest(intent));
    }

    // 6. Persistence Logic (If JPA or MyBatis)
    if ("JPA".equalsIgnoreCase(intent.getPersistence()) && intent.getEntities() != null) {
      for (EntityModel entity : intent.getEntities()) {
        files.put(packagePath + "model/" + entity.getName() + ".java", generateJpaEntity(intent, entity));
        files.put(packagePath + "repository/" + entity.getName() + "Repository.java",
            generateJpaRepository(intent, entity));
      }
    } else if ("MYBATIS".equalsIgnoreCase(intent.getPersistence())) {
      files.put(packagePath + "model/User.java", generateUserEntity(intent));
      files.put(packagePath + "mapper/UserMapper.java", generateUserMapper(intent));
      files.put("src/main/resources/schema.sql", generateSchema(intent));
    }

    // 6.5 OAuth2 Config
    if ("OAUTH2".equalsIgnoreCase(intent.getAuth())) {
      files.put(packagePath + "security/OAuth2SecurityConfig.java", generateOAuth2SecurityConfig(intent));
    }

    // 7. Dockerfile
    files.put("Dockerfile", generateDockerfile(intent));

    // 8. Docker Compose
    files.put("docker-compose.yml", generateDockerCompose(intent));

    // 9. CI Workflow
    files.put(".github/workflows/ci.yml", generateCiWorkflow(intent));

    return files;
  }

  // Add this method in CodeGenerationEngine.java (around line 60, before
  // buildDependenciesFromLlm)

  private String cleanLlmOutput(String llmResponse) {
    if (llmResponse == null || llmResponse.trim().isEmpty()) {
      return "[]";
    }

    String response = llmResponse;

    // Remove ALL file markers and XML completely (your main issue)
    response = response.replaceAll("(?is)///\\s*(START|END)\\s*FILE\\s*:?\\s*[\\w./-]*\\s*\n?", "");
    response = response.replaceAll("(?s)<project.*?/project>", ""); // Remove full XML
    response = response.replaceAll("(?s)<[^>]*>", ""); // Remove all XML tags

    // Clean other junk
    response = response.replaceAll("(?s)``````", "");
    response = response.replaceAll("(?m)^\\s*//.*$", "");
    response = response.replaceAll("/\\*.*?\\*/", "");

    // Find JSON array or object
    String json = extractJsonBlock(response);
    return json.isEmpty() ? "[]" : json;
  }

  private String extractJsonBlock(String text) {
    // Look for JSON array first
    int arrayStart = text.indexOf('[');
    if (arrayStart != -1) {
      int arrayEnd = findMatchingBracket(text, arrayStart, '[', ']');
      if (arrayEnd != -1) {
        return text.substring(arrayStart, arrayEnd + 1);
      }
    }

    // Then JSON object
    int objStart = text.indexOf('{');
    if (objStart != -1) {
      int objEnd = findMatchingBracket(text, objStart, '{', '}');
      if (objEnd != -1) {
        return text.substring(objStart, objEnd + 1);
      }
    }

    return "";
  }

  private int findMatchingBracket(String text, int start, char open, char close) {
    int count = 1;
    for (int i = start + 1; i < text.length(); i++) {
      if (text.charAt(i) == open)
        count++;
      else if (text.charAt(i) == close) {
        count--;
        if (count == 0)
          return i;
      }
    }
    return -1;
  }

  // private String buildDependenciesFromLlm(String llmOutput) {
  // StringBuilder deps = new StringBuilder();

  // try {
  // String cleanedOutput = cleanLlmOutput(llmOutput);
  // System.out.println("Cleaned LLM output: " + cleanedOutput); // Debug

  // JsonNode root = objectMapper.readTree(cleanedOutput);

  // // Handle both array and single object
  // if (root.isArray()) {
  // for (JsonNode dep : root) {
  // appendDependency(deps, dep);
  // }
  // } else if (root.isObject() && root.has("dependencies")) {
  // JsonNode depsArray = root.get("dependencies");
  // if (depsArray.isArray()) {
  // for (JsonNode dep : depsArray) {
  // appendDependency(deps, dep);
  // }
  // }
  // }

  // } catch (Exception e) {
  // System.err.println("Failed to parse LLM dependency JSON: " + e.getMessage());
  // System.err.println("Raw LLM output: " + llmOutput.substring(0, Math.min(200,
  // llmOutput.length())));
  // // Return Kafka defaults instead of empty
  // return getKafkaDefaultDependencies();
  // }

  // return deps.length() > 0 ? deps.toString() : getKafkaDefaultDependencies();
  // }

  private String buildDependenciesFromLlm(String llmOutput) {
    StringBuilder deps = new StringBuilder();

    try {
      String cleaned = cleanLlmOutput(llmOutput);

      // FIXED: Safe substring with length check
      int maxLength = Math.min(200, cleaned.length());
      System.out.println("Cleaned deps JSON: " + cleaned.substring(0, maxLength));

      JsonNode root = objectMapper.readTree(cleaned);
      if (root.isArray()) {
        for (JsonNode dep : root) {
          appendDependency(deps, dep);
        }
      }
    } catch (Exception e) {
      System.err.println("Dependency parse failed, using Kafka defaults: " + e.getMessage());
    }

    // ENSURE KAFKA ALWAYS WORKS
    if (deps.length() == 0) {
      deps.append(getKafkaDependencies());
    }

    return deps.toString();
  }

  private void appendDependency(StringBuilder deps, JsonNode dep) {
    deps.append("    <dependency>\n");
    deps.append("        <groupId>").append(dep.path("groupId").asText()).append("</groupId>\n");
    deps.append("        <artifactId>").append(dep.path("artifactId").asText()).append("</artifactId>\n");
    if (dep.has("version")) {
      deps.append("        <version>").append(dep.path("version").asText()).append("</version>\n");
    }
    deps.append("    </dependency>\n");
  }

  private String getKafkaDependencies() {
    return """
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        """;
  }

  public String generatePomDynamic(IntentModel intent, String llmDependenciesJson) {
    String depsXml = buildDependenciesFromLlm(llmDependenciesJson);

    return String.format(
        """
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
                            <dependencies>
            %s
                            </dependencies>
                        </project>
                        """,
        intent.getPackageName(), intent.getServiceName(), intent.getServiceName(), depsXml);
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
    String dbUrl = "";
    String dbDriver = "";
    String dbUser = "sa";
    String dbPass = "password";

    if (intent.getDatabase().equalsIgnoreCase("MYSQL")) {
      dbUrl = "jdbc:mysql://db:3306/" + intent.getServiceName().replace("-", "_");
      dbDriver = "com.mysql.cj.jdbc.Driver";
      dbUser = "root";
      dbPass = "root";
    } else if (intent.getDatabase().equalsIgnoreCase("POSTGRESQL")) {
      dbUrl = "jdbc:postgresql://db:5432/" + intent.getServiceName().replace("-", "_");
      dbDriver = "org.postgresql.Driver";
      dbUser = "postgres";
      dbPass = "postgres";
    } else {
      dbUrl = "jdbc:h2:mem:" + intent.getServiceName().replace("-", "_");
      dbDriver = "org.h2.Driver";
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
          sql:
            init:
              mode: always
        """.formatted(intent.getPort(), intent.getServiceName(), dbUrl, dbDriver, dbUser, dbPass);
  }

  private void parseAndWriteLlmOutput(IntentModel intent, String llmOutput, Map<String, String> files) {
    String packagePath = "src/main/java/" + intent.getPackageName().replace('.', '/') + "/";
    String[] parts = llmOutput.split("/// START FILE: ");
    String subPackage = "service/";
    for (String part : parts) {
      if (part.trim().isEmpty())
        continue;

      int endIndex = part.indexOf("\n");
      if (endIndex == -1)
        continue;

      String filename = part.substring(0, endIndex).trim();
      String content = part.substring(endIndex + 1);

      if (content.contains("/// END FILE")) {
        content = content.substring(0, content.indexOf("/// END FILE")).trim();
      }

      // ALLOW LLM TO OVERRIDE CORE FILES - NO MORE DUPLICATES SKIPPED
      List<String> coreFiles = Arrays.asList("pom.xml", "application.yml", "application.properties",
          "Dockerfile", "docker-compose.yml", "docker-compose.yaml");

      if (coreFiles.contains(filename)) {
        System.out.println("✅ LLM overriding core file: " + filename);
        // LET IT THROUGH - NO CONTINUE!
      } else if (files.containsKey(packagePath + subPackage + filename)) {
        System.out.println("⚠️ Skipping true duplicate: " + filename);
        continue;
      }

      // Determine sub-package based on filename or content
      // default
      if (filename.endsWith("Controller.java"))
        subPackage = "controller/";
      else if (filename.endsWith("Repository.java"))
        subPackage = "repository/";
      else if (filename.endsWith("DTO.java") || filename.endsWith("Request.java") || filename.endsWith("Response.java")
          || filename.endsWith("Model.java") || content.contains("@Entity"))
        subPackage = "model/";
      else if (filename.endsWith("Config.java") || filename.endsWith("Configuration.java"))
        subPackage = "config/";
      else if (filename.endsWith("Util.java") || filename.endsWith("Utils.java"))
        subPackage = "util/";

      // Fix package declaration if needed (heuristic)
      if (!content.contains("package " + intent.getPackageName())) {
        content = content.replaceFirst("package .*",
            "package " + intent.getPackageName() + "." + subPackage.replace("/", "") + ";");
      }

      files.put(packagePath + subPackage + filename, content);
    }

    // Fallback if parsing failed (e.g. LLM ignored instructions)
    if (files.isEmpty() || parts.length <= 1) {
      files.put(packagePath + "service/GeneratedService.java",
          """
              package %s.service;
              import org.springframework.stereotype.Service;
              // LLM output format was not recognized. Dumping raw content:
              /*
              %s
              */
              @Service
              public class GeneratedService {}
              """.formatted(intent.getPackageName(), llmOutput));
    }
  }

  private String generateDockerfile(IntentModel intent) {
    return """
        FROM eclipse-temurin:21-jdk-alpine
        VOLUME /tmp
        COPY target/*.jar app.jar
        ENTRYPOINT ["java","-jar","/app.jar"]
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
            private String jwtKey = System.getenv().getOrDefault("JWT_SECRET", "dev-key-123");

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

            public AuthController(JwtUtils jwtUtils) {
                this.jwtUtils = jwtUtils;
            }

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

  private String generateDockerCompose(IntentModel intent) {
    String dbImage = "h2";
    StringBuilder env = new StringBuilder();
    String dbName = intent.getServiceName().replace("-", "_");

    if (intent.getDatabase().equalsIgnoreCase("MYSQL")) {
      dbImage = "mysql:8.0";
      env.append("""
                - MYSQL_ROOT_PASSWORD=root
                - MYSQL_DATABASE=%s
          """.formatted(dbName));
    } else if (intent.getDatabase().equalsIgnoreCase("POSTGRESQL")) {
      dbImage = "postgres:15-alpine";
      env.append("""
                - POSTGRES_PASSWORD=postgres
                - POSTGRES_DB=%s
          """.formatted(dbName));
    }

    if (dbImage.equals("h2")) {
      return """
          version: '3.8'
          services:
            app:
              build: .
              ports:
                - "%d:%d"
          """.formatted(intent.getPort(), intent.getPort());
    }

    return """
        version: '3.8'
        services:
          app:
            build: .
            ports:
              - "%d:%d"
            depends_on:
              - db
          db:
            image: %s
            environment:
        %s
        """.formatted(intent.getPort(), intent.getPort(), dbImage, env.toString());
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
        """.formatted(intent.getPackageName(), entity.getName().toLowerCase() + "s", entity.getName(),
        fields.toString());
  }

  private String generateJpaRepository(IntentModel intent, EntityModel entity) {
    return """
        package %s.repository;

        import %s.model.%s;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.stereotype.Repository;

        @Repository
        public interface %sRepository extends JpaRepository<%s, Long> {
        }
        """.formatted(intent.getPackageName(), intent.getPackageName(), entity.getName(), entity.getName(),
        entity.getName());
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
                    .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                    .oauth2Login(withDefaults());
                return http.build();
            }
        }
        """.formatted(intent.getPackageName());
  }
}
