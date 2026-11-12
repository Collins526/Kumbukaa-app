package com.kumbukaa.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Try multiple locations for .env file
        Path dotenvPath = null;
        String[] possiblePaths = {".env", "./env", "${user.dir}/.env"};
        
        for (String pathStr : possiblePaths) {
            Path candidate = Path.of(pathStr).toAbsolutePath();
            if (Files.exists(candidate)) {
                dotenvPath = candidate;
                break;
            }
        }
        
        // Also check in the current working directory explicitly
        if (dotenvPath == null) {
            Path cwd = Path.of(System.getProperty("user.dir"), ".env");
            if (Files.exists(cwd)) {
                dotenvPath = cwd;
            }
        }
        
        if (dotenvPath == null) {
            // .env file not found, but that's okay - rely on environment variables
            return;
        }

        Map<String, Object> propertyMap = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                propertyMap.put(key, value);
            }
        } catch (IOException e) {
            return;
        }

        if (!propertyMap.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("dotenv", propertyMap));
        }
    }
}
