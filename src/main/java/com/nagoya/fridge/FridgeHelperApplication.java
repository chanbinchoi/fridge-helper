package com.nagoya.fridge;

import com.nagoya.fridge.config.LocalDotenvLoader;
import com.nagoya.fridge.config.NotionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.nagoya")
@EnableConfigurationProperties(NotionProperties.class)
public class FridgeHelperApplication {

    public static void main(String[] args) {
        LocalDotenvLoader.load();
        SpringApplication.run(FridgeHelperApplication.class, args);
    }

}
