package com.nagoya.fridge.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.util.StringUtils;

public final class LocalDotenvLoader {

    private LocalDotenvLoader() {
    }

    public static void load() {
        load(Path.of(".env"));
    }

    static void load(Path dotenvPath) {
        if (!Files.isRegularFile(dotenvPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(dotenvPath);
            lines.stream()
                    .map(String::trim)
                    .filter(line -> StringUtils.hasText(line) && !line.startsWith("#"))
                    .forEach(LocalDotenvLoader::loadLine);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load local .env file: " + dotenvPath, exception);
        }
    }

    private static void loadLine(String line) {
        int separator = line.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = line.substring(0, separator).trim();
        String value = stripQuotes(line.substring(separator + 1).trim());

        if (!StringUtils.hasText(key)
                || System.getenv().containsKey(key)
                || System.getProperties().containsKey(key)) {
            return;
        }

        System.setProperty(key, value);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
