package com.nagoya.fridge;

import com.nagoya.fridge.config.LocalDotenvLoader;
import com.nagoya.fridge.config.NotionProperties;
import com.nagoya.fridge.config.RakutenRecipeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.nagoya")
@EnableConfigurationProperties({NotionProperties.class, RakutenRecipeProperties.class})
public class FridgeHelperApplication {

    private static final Logger log = LoggerFactory.getLogger(FridgeHelperApplication.class);

    public static void main(String[] args) {
        LocalDotenvLoader.load();
        SpringApplication.run(FridgeHelperApplication.class, args);
    }

    @Bean
    ApplicationRunner notionConfigurationLogger(NotionProperties notionProperties) {
        return args -> log.info("Configured Notion database id: {}", notionProperties.databaseId());
    }
}
