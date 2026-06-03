package com.nagoya.recipe.rakuten;

import com.nagoya.fridge.config.RakutenRecipeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class RakutenRecipeConfiguration {

    @Bean
    RestClient rakutenRecipeRestClient(RakutenRecipeProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
