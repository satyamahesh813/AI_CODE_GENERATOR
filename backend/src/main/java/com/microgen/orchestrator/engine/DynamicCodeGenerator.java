// Utility class for dynamic code generation
package com.microgen.orchestrator.engine;

import com.microgen.orchestrator.model.EntityModel;
import com.microgen.orchestrator.model.IntentModel;

import java.util.StringJoiner;

public class DynamicCodeGenerator {

    // 1. SecurityConfig for JWT dynamically
    public static String generateSecurityConfig(IntentModel intent) {
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

    // 2. JwtUtils dynamically
    public static String generateJwtUtils(IntentModel intent) {
        return """
                package %s.security;

                import io.jsonwebtoken.Jwts;
                import io.jsonwebtoken.SignatureAlgorithm;
                import org.springframework.stereotype.Component;
                import java.util.Date;

                @Component
                public class JwtUtils {
                    private final String jwtKey = System.getenv().getOrDefault("JWT_SECRET", "dev-key-123");

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

    // 3. AuthController dynamically
    public static String generateAuthController(IntentModel intent) {
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

    // 4. AuthRequest dynamically
    public static String generateAuthRequest(IntentModel intent) {
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

    // 5. OAuth2SecurityConfig dynamically
    public static String generateOAuth2SecurityConfig(IntentModel intent) {
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

    // 6. JPA Entity dynamically
    public static String generateJpaEntity(IntentModel intent, EntityModel entity) {
        StringJoiner fields = new StringJoiner("\n    ");
        entity.getFields().forEach(f -> fields.add("private " + f.getType() + " " + f.getName() + ";"));

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

    // 7. JPA Repository dynamically
    public static String generateJpaRepository(IntentModel intent, EntityModel entity) {
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

    // 8. MyBatis User Entity dynamically
    public static String generateUserEntity(IntentModel intent) {
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

    // 9. MyBatis User Mapper dynamically
    public static String generateUserMapper(IntentModel intent) {
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

    // 10. Schema.sql dynamically
    public static String generateSchema(IntentModel intent) {
        return """
                CREATE TABLE IF NOT EXISTS users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    password VARCHAR(255) NOT NULL
                );
                """;
    }

    // 11. Dockerfile dynamically
    public static String generateDockerfile(IntentModel intent) {
        return """
                FROM eclipse-temurin:21-jdk-alpine
                VOLUME /tmp
                COPY target/%s.jar app.jar
                ENTRYPOINT ["java","-jar","/app.jar"]
                """.formatted(intent.getServiceName());
    }

    // 12. Docker Compose dynamically
    public static String generateDockerCompose(IntentModel intent) {
        String dbService = "";
        if ("MYSQL".equalsIgnoreCase(intent.getDatabase())) {
            dbService = """
                    db:
                      image: mysql:8.0
                      environment:
                        - MYSQL_ROOT_PASSWORD=root
                        - MYSQL_DATABASE=%s
                    """.formatted(intent.getServiceName().replace("-", "_"));
        } else if ("POSTGRESQL".equalsIgnoreCase(intent.getDatabase())) {
            dbService = """
                    db:
                      image: postgres:15-alpine
                      environment:
                        - POSTGRES_PASSWORD=postgres
                        - POSTGRES_DB=%s
                    """.formatted(intent.getServiceName().replace("-", "_"));
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
                %s
                """.formatted(intent.getPort(), intent.getPort(), dbService);
    }

    // 13. CI Workflow dynamically
    public static String generateCiWorkflow(IntentModel intent) {
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
}
