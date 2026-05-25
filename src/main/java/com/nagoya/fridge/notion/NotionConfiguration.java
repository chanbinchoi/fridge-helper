package com.nagoya.fridge.notion;

import com.nagoya.fridge.config.NotionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class NotionConfiguration {

    @Bean
    RestClient notionRestClient(NotionProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Notion-Version", properties.version())
                .build();
    }
}
