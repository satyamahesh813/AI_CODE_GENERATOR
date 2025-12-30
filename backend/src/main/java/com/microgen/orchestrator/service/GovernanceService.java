package com.microgen.orchestrator.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class GovernanceService {

    private static final List<Pattern> SECRET_PATTERNS = List.of(
            Pattern.compile("(?i)secret.*[a-zA-Z0-9]{20,}"));

    public List<String> scan(Map<String, String> generatedFiles) {
        System.out.println("Starting governance scan on " + generatedFiles.size() + " files");
        List<String> violations = new ArrayList<>();

        for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
            String content = entry.getValue();
            String fileName = entry.getKey();

            for (Pattern pattern : SECRET_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    System.out.println("Violation found in " + fileName + " with pattern: " + pattern.pattern());
                    violations.add("Potential secret found in " + fileName);
                }
            }
        }

        return violations;
    }
}
