package com.nagoya.recipe.mapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RecipeCategoryMatcher {

    private final Map<String, Integer> ingredientCategoryMap;
    private final IngredientNameNormalizer ingredientNameNormalizer;

    public RecipeCategoryMatcher() {
        this(new IngredientNameNormalizer());
    }

    @Autowired
    public RecipeCategoryMatcher(IngredientNameNormalizer ingredientNameNormalizer) {
        this.ingredientNameNormalizer = ingredientNameNormalizer;
        Map<String, Integer> categoryMap = new LinkedHashMap<>();
        putCategory(categoryMap, "卵", 33);
        putCategory(categoryMap, "たまご", 33);
        putCategory(categoryMap, "玉子", 33);
        putCategory(categoryMap, "豆腐", 35);
        putCategory(categoryMap, "豚こま肉", 10);
        putCategory(categoryMap, "豚肉", 10);
        putCategory(categoryMap, "ごま油", 19);
        putCategory(categoryMap, "胡麻油", 19);
        this.ingredientCategoryMap = Collections.unmodifiableMap(categoryMap);
    }

    public Optional<Integer> matchCategoryId(String ingredientName) {
        if (!StringUtils.hasText(ingredientName)) {
            return Optional.empty();
        }
        String normalizedIngredientName = ingredientNameNormalizer.normalizeForLookup(ingredientName);
        return ingredientCategoryMap.entrySet().stream()
                .filter(entry -> ingredientNameNormalizer.matches(normalizedIngredientName, entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public List<Integer> matchCategoryIds(List<String> ingredientNames) {
        Map<Integer, Boolean> categoryIds = new LinkedHashMap<>();

        ingredientNames.stream()
                .map(this::matchCategoryId)
                .flatMap(Optional::stream)
                .forEach(categoryId -> categoryIds.putIfAbsent(categoryId, true));

        return categoryIds.keySet().stream().toList();
    }

    private void putCategory(Map<String, Integer> categoryMap, String ingredientName, int categoryId) {
        categoryMap.putIfAbsent(ingredientNameNormalizer.normalizeForLookup(ingredientName), categoryId);
    }
}
