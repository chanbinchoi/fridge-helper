package com.nagoya.fridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notion")
public record NotionProperties(
        String apiKey,
        String databaseId,
        String baseUrl,
        String version,
        String ingredientNameProperty,
        String expirationDateProperty,
        String stockStatusProperty,
        String inStockValue
) {
}
