package com.example.doktoribackend.notification.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{(\\w+)}");

    public String render(String template, Map<String, String> parameters) {
        if (template == null || parameters == null || parameters.isEmpty()) {
            return template;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = parameters.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
