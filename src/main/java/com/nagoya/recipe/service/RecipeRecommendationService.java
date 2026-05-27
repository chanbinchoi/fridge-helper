package com.nagoya.recipe.service;

import com.nagoya.fridge.ingredient.IngredientNamesResult;
import com.nagoya.fridge.ingredient.IngredientRawDataService;
import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.mapping.RecipeCategoryMatcher;
import com.nagoya.recipe.scraper.RakutenRecipeScraper;
import com.nagoya.recipe.scraper.RakutenRecipeScraperException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecipeRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRecommendationService.class);
    private static final int DETAIL_SCRAPE_LIMIT = 5;

    private final IngredientRawDataService ingredientRawDataService;
    private final RecipeCategoryMatcher recipeCategoryMatcher;
    private final RakutenRecipeScraper rakutenRecipeScraper;

    public RecipeRecommendationService(
            IngredientRawDataService ingredientRawDataService,
            RecipeCategoryMatcher recipeCategoryMatcher,
            RakutenRecipeScraper rakutenRecipeScraper
    ) {
        this.ingredientRawDataService = ingredientRawDataService;
        this.recipeCategoryMatcher = recipeCategoryMatcher;
        this.rakutenRecipeScraper = rakutenRecipeScraper;
    }

    public List<RecipeSearchResultDto> recommendRecipes() {
        IngredientNamesResult ingredientNamesResult = ingredientRawDataService.fetchIngredientNames();
        List<String> names = ingredientNamesResult.names();
        log.info("냉장고 보유 재료: {}", names);

        List<Integer> categoryIds = recipeCategoryMatcher.matchCategoryIds(names);
        log.info("매핑된 카테고리 ID 목록: {}", categoryIds);

        Map<String, RecipeSearchResultDto> recipesByLinkUrl = new LinkedHashMap<>();

        for (Integer categoryId : categoryIds) {
            List<RecipeSearchResultDto> recipes = rakutenRecipeScraper.scrapePopularRecipesByCategory(
                    String.valueOf(categoryId)
            );
            log.info("라쿠텐 카테고리 {} 크롤링 완료: {}건", categoryId, recipes.size());
            recipes.forEach(recipe -> recipesByLinkUrl.putIfAbsent(recipe.getLinkUrl(), recipe));
        }

        return recipesByLinkUrl.values().stream()
                .limit(DETAIL_SCRAPE_LIMIT)
                .map(this::withScrapedMaterials)
                .map(recipe -> scoreRecipe(recipe, names))
                .sorted(Comparator.comparingDouble(RecipeSearchResultDto::getMatchRate).reversed())
                .toList();
    }

    private RecipeSearchResultDto withScrapedMaterials(RecipeSearchResultDto recipe) {
        try {
            List<String> materials = rakutenRecipeScraper.scrapeRecipeMaterials(recipe.getLinkUrl());
            return recipe.toBuilder()
                    .materials(materials)
                    .build();
        } catch (RakutenRecipeScraperException exception) {
            log.info("레시피 상세 재료 크롤링 실패: {}", recipe.getLinkUrl());
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
                .map(this::normalizeForMatching)
                .filter(stockedIngredient -> !stockedIngredient.isBlank())
                .anyMatch(stockedIngredient ->
                        normalizedMaterial.contains(stockedIngredient)
                                || stockedIngredient.contains(normalizedMaterial)
                );
    }

    private String normalizeForMatching(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("\\s+", "")
                .replace("　", "")
                .trim()
                .toLowerCase();
    }
}
