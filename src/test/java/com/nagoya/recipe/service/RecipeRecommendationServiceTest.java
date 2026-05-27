package com.nagoya.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nagoya.fridge.ingredient.IngredientNamesResult;
import com.nagoya.fridge.ingredient.IngredientRawDataService;
import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.mapping.RecipeCategoryMatcher;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeRecommendationServiceTest {

    @Test
    void recommendRecipesMatchesIngredientsAndScrapesMappedCategories() {
        IngredientRawDataService ingredientService = new IngredientRawDataService(null, null) {
            @Override
            public IngredientNamesResult fetchIngredientNames() {
                return new IngredientNamesResult(
                        Instant.parse("2026-05-27T00:00:00Z"),
                        2,
                        List.of("卵", "豆腐")
                );
            }
        };
        RecordingRakutenRecipeScraper scraper = new RecordingRakutenRecipeScraper();
        RecipeRecommendationService service = new RecipeRecommendationService(
                ingredientService,
                new RecipeCategoryMatcher(),
                scraper
        );

        List<RecipeSearchResultDto> results = service.recommendRecipes();

        assertThat(scraper.calledCategoryIds).containsExactly("33", "35");
        assertThat(scraper.calledRecipeUrls).containsExactly(
                "https://recipe.rakuten.co.jp/recipe/egg/",
                "https://recipe.rakuten.co.jp/recipe/shared/",
                "https://recipe.rakuten.co.jp/recipe/tofu/"
        );
        assertThat(results).extracting(RecipeSearchResultDto::getTitle)
                .containsExactly("共通レシピ", "卵レシピ", "豆腐レシピ");
        assertThat(results).extracting(RecipeSearchResultDto::getMatchRate)
                .containsExactly(100.0, 50.0, 50.0);
        assertThat(results.getFirst().getMissingMaterials()).isEmpty();
    }

    @Test
    void scoreRecipeUsesContainsMatchingAndReturnsMissingMaterials() {
        RecipeRecommendationService service = new RecipeRecommendationService(null, null, null);
        RecipeSearchResultDto recipe = RecipeSearchResultDto.builder()
                .title("豚肉卵炒め")
                .linkUrl("https://recipe.rakuten.co.jp/recipe/pork-egg/")
                .imageUrl("https://recipe.r10s.jp/pork-egg.jpg")
                .materials(List.of("豚肉（バラ）", "卵", "玉ねぎ"))
                .build();

        RecipeSearchResultDto scored = service.scoreRecipe(recipe, List.of("豚肉", "卵"));

        assertThat(scored.getMatchRate()).isEqualTo(66.66666666666666);
        assertThat(scored.getMissingMaterials()).containsExactly("玉ねぎ");
    }

    @Test
    void recommendRecipesOnlyScrapesDetailsForTopFiveUniqueRecipes() {
        IngredientRawDataService ingredientService = new IngredientRawDataService(null, null) {
            @Override
            public IngredientNamesResult fetchIngredientNames() {
                return new IngredientNamesResult(
                        Instant.parse("2026-05-27T00:00:00Z"),
                        2,
                        List.of("卵", "豆腐")
                );
            }
        };
        RecordingRakutenRecipeScraper scraper = new RecordingRakutenRecipeScraper();
        scraper.useManyRecipes = true;
        RecipeRecommendationService service = new RecipeRecommendationService(
                ingredientService,
                new RecipeCategoryMatcher(),
                scraper
        );

        List<RecipeSearchResultDto> results = service.recommendRecipes();

        assertThat(results).hasSize(5);
        assertThat(scraper.calledRecipeUrls).hasSize(5);
        assertThat(scraper.calledRecipeUrls).containsExactly(
                "https://recipe.rakuten.co.jp/recipe/egg-1/",
                "https://recipe.rakuten.co.jp/recipe/egg-2/",
                "https://recipe.rakuten.co.jp/recipe/egg-3/",
                "https://recipe.rakuten.co.jp/recipe/egg-4/",
                "https://recipe.rakuten.co.jp/recipe/egg-5/"
        );
    }

    private static class RecordingRakutenRecipeScraper extends RakutenRecipeScraper {

        private final List<String> calledCategoryIds = new ArrayList<>();
        private final List<String> calledRecipeUrls = new ArrayList<>();
        private boolean useManyRecipes;

        @Override
        public List<RecipeSearchResultDto> scrapePopularRecipesByCategory(String categoryId) {
            calledCategoryIds.add(categoryId);
            if (useManyRecipes && "33".equals(categoryId)) {
                return java.util.stream.IntStream.rangeClosed(1, 8)
                        .mapToObj(index -> recipe(
                                "卵レシピ" + index,
                                "https://recipe.rakuten.co.jp/recipe/egg-" + index + "/"
                        ))
                        .toList();
            }
            if (useManyRecipes) {
                return List.of();
            }
            if ("33".equals(categoryId)) {
                return List.of(
                        recipe("卵レシピ", "https://recipe.rakuten.co.jp/recipe/egg/"),
                        recipe("共通レシピ", "https://recipe.rakuten.co.jp/recipe/shared/")
                );
            }
            if ("35".equals(categoryId)) {
                return List.of(
                        recipe("共通レシピ", "https://recipe.rakuten.co.jp/recipe/shared/"),
                        recipe("豆腐レシピ", "https://recipe.rakuten.co.jp/recipe/tofu/")
                );
            }
            return List.of();
        }

        @Override
        public List<String> scrapeRecipeMaterials(String recipeUrl) {
            calledRecipeUrls.add(recipeUrl);
            if (recipeUrl.contains("shared")) {
                return List.of("卵", "豆腐");
            }
            if (recipeUrl.contains("tofu")) {
                return List.of("豆腐", "豚肉（バラ）");
            }
            return List.of("卵", "玉ねぎ");
        }

        private RecipeSearchResultDto recipe(String title, String linkUrl) {
            return RecipeSearchResultDto.builder()
                    .title(title)
                    .linkUrl(linkUrl)
                    .imageUrl("https://recipe.r10s.jp/" + title + ".jpg")
                    .build();
        }
    }
}
