package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class EnvLoader {
    private EnvLoader() {
    }

    public static void loadDotEnv(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int index = trimmed.indexOf('=');
                if (index <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }

                if (!key.isEmpty() && System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // Ignore .env loading errors and continue with existing environment variables.
        }
    }

    public static void saveGeminiApiKey(String filePath, String apiKey) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be empty.");
        }

        Path path = Paths.get(filePath);
        List<String> lines = Files.exists(path)
                ? Files.readAllLines(path, StandardCharsets.UTF_8)
                : new ArrayList<>();

        List<String> updated = new ArrayList<>();
        boolean replaced = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("GEMINI_API_KEY=")) {
                if (!replaced) {
                    updated.add("GEMINI_API_KEY=" + apiKey.trim());
                    replaced = true;
                }
                continue;
            }
            updated.add(line);
        }

        if (!replaced) {
            updated.add("GEMINI_API_KEY=" + apiKey.trim());
        }

        Files.write(path, updated, StandardCharsets.UTF_8);
        System.setProperty("GEMINI_API_KEY", apiKey.trim());
    }
}
