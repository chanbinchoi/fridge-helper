package com.nagoya.recipe;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import com.nagoya.recipe.service.RecipeRankingService;
import com.nagoya.recipe.service.RecipeRecommendationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecipeControllerTest {

    @Test
    void testRecipeScrapingReturnsRecipeSearchResults() throws Exception {
        RakutenRecipeScraper scraper = new RakutenRecipeScraper() {
            @Override
            public List<RecipeSearchResultDto> scrapePopularRecipesByCategory(String categoryId) {
                return List.of(RecipeSearchResultDto.builder()
                        .title("テストレシピ")
                        .linkUrl("https://recipe.rakuten.co.jp/recipe/1234567890/")
                        .imageUrl("https://recipe.r10s.jp/test.jpg")
                        .build());
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RecipeController(scraper, null, null))
                .build();

        mockMvc.perform(get("/api/recipes/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("テストレシピ"))
                .andExpect(jsonPath("$[0].linkUrl").value("https://recipe.rakuten.co.jp/recipe/1234567890/"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://recipe.r10s.jp/test.jpg"));
    }

    @Test
    void recommendRecipesReturnsRecommendationResults() throws Exception {
        RecipeRecommendationService recommendationService = new RecipeRecommendationService(null, null, null) {
            @Override
            public List<RecipeSearchResultDto> recommendRecipes() {
                return List.of(RecipeSearchResultDto.builder()
                        .title("おすすめレシピ")
                        .linkUrl("https://recipe.rakuten.co.jp/recipe/recommend/")
                        .imageUrl("https://recipe.r10s.jp/recommend.jpg")
                        .build());
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RecipeController(null, null, recommendationService))
                .build();

        mockMvc.perform(get("/api/recipes/recommend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("おすすめレシピ"))
                .andExpect(jsonPath("$[0].linkUrl").value("https://recipe.rakuten.co.jp/recipe/recommend/"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://recipe.r10s.jp/recommend.jpg"));
    }

    @Test
    void generalRankingReturnsRankingResults() throws Exception {
        RecipeRankingService rankingService = new RecipeRankingService("app-id", "access-key", null) {
            @Override
            public List<RecipeSearchResultDto> generalRanking() {
                return List.of(RecipeSearchResultDto.builder()
                        .title("総合人気レシピ")
                        .linkUrl("https://recipe.rakuten.co.jp/recipe/general/")
                        .imageUrl("https://recipe.r10s.jp/general.jpg")
                        .build());
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RecipeController(null, rankingService, null))
                .build();

        mockMvc.perform(get("/api/recipes/ranking/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("総合人気レシピ"))
                .andExpect(jsonPath("$[0].linkUrl").value("https://recipe.rakuten.co.jp/recipe/general/"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://recipe.r10s.jp/general.jpg"));
    }

    @Test
    void proteinRankingReturnsRankingResults() throws Exception {
        RecipeRankingService rankingService = new RecipeRankingService("app-id", "access-key", null) {
            @Override
            public List<RecipeSearchResultDto> proteinRanking() {
                return List.of(RecipeSearchResultDto.builder()
                        .title("高タンパクレシピ")
                        .linkUrl("https://recipe.rakuten.co.jp/recipe/protein/")
                        .imageUrl("https://recipe.r10s.jp/protein.jpg")
                        .build());
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new RecipeController(null, rankingService, null))
                .build();

        mockMvc.perform(get("/api/recipes/ranking/protein"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("高タンパクレシピ"))
                .andExpect(jsonPath("$[0].linkUrl").value("https://recipe.rakuten.co.jp/recipe/protein/"))
                .andExpect(jsonPath("$[0].imageUrl").value("https://recipe.r10s.jp/protein.jpg"));
    }
}
