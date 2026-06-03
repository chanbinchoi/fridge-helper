package com.nagoya.fridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rakuten.recipe")
public record RakutenRecipeProperties(
        String applicationId,
        String accessKey,
        String baseUrl
) {
}
