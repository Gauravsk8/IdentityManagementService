package com.example.identitymanagementservice.common.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class EmailTemplateUtil {

    private static final String TEMPLATE_PATH_PREFIX = "email-templates/";
    private static final String VAR_PREFIX_1 = "{{";
    private static final String VAR_SUFFIX_1 = "}}";
    private static final String VAR_PREFIX_2 = "${";
    private static final String VAR_SUFFIX_2 = "}";
    private static final String LINE_SEPARATOR = "\n";

    public static String loadTemplate(String templateName, Map<String, String> variables) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH_PREFIX + templateName);
            String template;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                template = reader.lines().collect(Collectors.joining(LINE_SEPARATOR));
            }

            // Support two types of placeholder styles {{var}} and ${var}
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                template = template.replace(VAR_PREFIX_1 + entry.getKey() + VAR_SUFFIX_1, entry.getValue());
                template = template.replace(VAR_PREFIX_2 + entry.getKey() + VAR_SUFFIX_2, entry.getValue());
            }
            return template;
        } catch (Exception e) {
            log.error("Failed to load email template: {}", templateName, e);
            throw new RuntimeException("Error loading email template: " + templateName, e);
        }
    }
}
