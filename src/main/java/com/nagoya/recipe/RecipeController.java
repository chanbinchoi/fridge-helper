package com.nagoya.recipe;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import com.nagoya.recipe.service.RecipeRecommendationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private static final String TEST_CATEGORY_ID = "32";

    private final RakutenRecipeScraper rakutenRecipeScraper;
    private final RecipeRecommendationService recipeRecommendationService;

    public RecipeController(
            RakutenRecipeScraper rakutenRecipeScraper,
            RecipeRecommendationService recipeRecommendationService
    ) {
        this.rakutenRecipeScraper = rakutenRecipeScraper;
        this.recipeRecommendationService = recipeRecommendationService;
    }

    @GetMapping("/test")
    public List<RecipeSearchResultDto> testRecipeScraping() {
        return rakutenRecipeScraper.scrapePopularRecipesByCategory(TEST_CATEGORY_ID);
    }

    @GetMapping("/recommend")
    public List<RecipeSearchResultDto> recommendRecipes() {
        return recipeRecommendationService.recommendRecipes();
    }
}
