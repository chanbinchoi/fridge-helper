package com.nagoya.recipe;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import com.nagoya.recipe.service.RecipeRankingService;
import com.nagoya.recipe.service.RecipeRecommendationService;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private static final String TEST_CATEGORY_ID = "32";

    private final RakutenRecipeScraper rakutenRecipeScraper;
    private final RecipeRankingService recipeRankingService;
    private final RecipeRecommendationService recipeRecommendationService;

    public RecipeController(
            RakutenRecipeScraper rakutenRecipeScraper,
            RecipeRankingService recipeRankingService,
            RecipeRecommendationService recipeRecommendationService
    ) {
        this.rakutenRecipeScraper = rakutenRecipeScraper;
        this.recipeRankingService = recipeRankingService;
        this.recipeRecommendationService = recipeRecommendationService;
    }

    @GetMapping("/test")
    public ResponseEntity<List<RecipeSearchResultDto>> testRecipeScraping() {
        return noStore(rakutenRecipeScraper.scrapePopularRecipesByCategory(TEST_CATEGORY_ID));
    }

    @GetMapping("/recommend")
    public ResponseEntity<List<RecipeSearchResultDto>> recommendRecipes() {
        return noStore(recipeRecommendationService.recommendRecipes());
    }

    @GetMapping("/ranking/general")
    public ResponseEntity<List<RecipeSearchResultDto>> generalRanking() {
        return noStore(recipeRankingService.generalRanking());
    }

    @GetMapping("/ranking/protein")
    public ResponseEntity<List<RecipeSearchResultDto>> proteinRanking() {
        return noStore(recipeRankingService.proteinRanking());
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
