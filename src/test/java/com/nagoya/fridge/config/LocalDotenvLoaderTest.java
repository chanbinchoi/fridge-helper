package com.nagoya.fridge.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LocalDotenvLoaderTest {

    @Test
    void loadAddsDotenvValuesToSystemProperties() throws Exception {
        String key = "FRIDGE_HELPER_TEST_DOTENV_KEY";
        System.clearProperty(key);
        Path dotenv = Files.createTempFile("fridge-helper", ".env");
        Files.writeString(dotenv, key + "=loaded-value\n");

        try {
            LocalDotenvLoader.load(dotenv);

            assertThat(System.getProperty(key)).isEqualTo("loaded-value");
        } finally {
            System.clearProperty(key);
            Files.deleteIfExists(dotenv);
        }
    }

    @Test
    void loadDoesNotOverrideExistingSystemProperties() throws Exception {
        String key = "FRIDGE_HELPER_TEST_EXISTING_KEY";
        System.setProperty(key, "existing-value");
        Path dotenv = Files.createTempFile("fridge-helper", ".env");
        Files.writeString(dotenv, key + "=dotenv-value\n");

        try {
            LocalDotenvLoader.load(dotenv);

            assertThat(System.getProperty(key)).isEqualTo("existing-value");
        } finally {
            System.clearProperty(key);
            Files.deleteIfExists(dotenv);
        }
    }
}
