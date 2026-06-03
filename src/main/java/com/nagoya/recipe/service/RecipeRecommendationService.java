package com.nagoya.recipe.service;

import com.nagoya.fridge.ingredient.IngredientNamesResult;
import com.nagoya.fridge.ingredient.IngredientRawDataService;
import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.mapping.IngredientNameNormalizer;
import com.nagoya.recipe.mapping.RecipeCategoryMatcher;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import com.nagoya.recipe.scraper.RakutenRecipeScraperException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecipeRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRecommendationService.class);
    private static final int DETAIL_SCRAPE_LIMIT = 5;

    private final IngredientRawDataService ingredientRawDataService;
    private final IngredientNameNormalizer ingredientNameNormalizer;
    private final RecipeCategoryMatcher recipeCategoryMatcher;
    private final RakutenRecipeScraper rakutenRecipeScraper;

    public RecipeRecommendationService(
            IngredientRawDataService ingredientRawDataService,
            RecipeCategoryMatcher recipeCategoryMatcher,
            RakutenRecipeScraper rakutenRecipeScraper
    ) {
        this(ingredientRawDataService, new IngredientNameNormalizer(), recipeCategoryMatcher, rakutenRecipeScraper);
    }

    @Autowired
    public RecipeRecommendationService(
            IngredientRawDataService ingredientRawDataService,
            IngredientNameNormalizer ingredientNameNormalizer,
            RecipeCategoryMatcher recipeCategoryMatcher,
            RakutenRecipeScraper rakutenRecipeScraper
    ) {
        this.ingredientRawDataService = ingredientRawDataService;
        this.ingredientNameNormalizer = ingredientNameNormalizer;
        this.recipeCategoryMatcher = recipeCategoryMatcher;
        this.rakutenRecipeScraper = rakutenRecipeScraper;
    }

    public List<RecipeSearchResultDto> recommendRecipes() {
        IngredientNamesResult ingredientNamesResult = ingredientRawDataService.fetchIngredientNames();
        List<String> names = ingredientNamesResult.names();
        log.info("冷蔵庫の在庫食材: {}", names);

        List<Integer> categoryIds = recipeCategoryMatcher.matchCategoryIds(names);
        log.info("マッピングされたカテゴリID一覧: {}", categoryIds);

        Map<String, RecipeSearchResultDto> recipesByLinkUrl = new LinkedHashMap<>();

        for (Integer categoryId : categoryIds) {
            List<RecipeSearchResultDto> recipes = rakutenRecipeScraper.scrapePopularRecipesByCategory(
                    String.valueOf(categoryId)
            );
            log.info("楽天カテゴリ {} のクロール完了: {}件", categoryId, recipes.size());
            recipes.forEach(recipe -> recipesByLinkUrl.putIfAbsent(recipe.getLinkUrl(), recipe));
        }

        return recipesByLinkUrl.values().stream()
                .limit(DETAIL_SCRAPE_LIMIT)
                .map(this::withScrapedMaterials)
                .map(recipe -> scoreRecipe(recipe, names))
                .sorted(Comparator.comparingDouble(RecipeSearchResultDto::getMatchRate).reversed())
                .toList();
    }

    RecipeSearchResultDto withScrapedMaterials(RecipeSearchResultDto recipe) {
        try {
            List<String> materials = rakutenRecipeScraper.scrapeRecipeMaterials(recipe.getLinkUrl());
            return recipe.toBuilder()
                    .materials(materials)
                    .build();
        } catch (RakutenRecipeScraperException exception) {
            log.info("レシピ詳細の材料クロールに失敗しました: {}", recipe.getLinkUrl());
            return recipe.toBuilder()
                    .materials(List.of())
                    .build();
        }
    }

    RecipeSearchResultDto scoreRecipe(RecipeSearchResultDto recipe, List<String> stockedIngredients) {
        List<String> materials = recipe.getMaterials();
        if (materials.isEmpty()) {
            return recipe.toBuilder()
                    .matchRate(0.0)
                    .missingMaterials(List.of())
                    .build();
        }

        List<String> missingMaterials = materials.stream()
                .filter(material -> !matchesAnyStockedIngredient(material, stockedIngredients))
                .toList();
        int matchedCount = materials.size() - missingMaterials.size();
        double matchRate = ((double) matchedCount / materials.size()) * 100.0;

        return recipe.toBuilder()
                .matchRate(matchRate)
                .missingMaterials(missingMaterials)
                .build();
    }

    private boolean matchesAnyStockedIngredient(String recipeMaterial, List<String> stockedIngredients) {
        String normalizedMaterial = normalizeForMatching(recipeMaterial);
        if (normalizedMaterial.isBlank()) {
            return false;
        }

        return stockedIngredients.stream()
                .anyMatch(stockedIngredient -> ingredientNameNormalizer.matches(normalizedMaterial, stockedIngredient));
    }

    private String normalizeForMatching(String value) {
        return ingredientNameNormalizer.normalizeForLookup(value);
    }
}
