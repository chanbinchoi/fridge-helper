package com.nagoya.recipe.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RecipeCategoryMatcher {

    private final Map<String, Integer> ingredientCategoryMap;

    public RecipeCategoryMatcher() {
        this.ingredientCategoryMap = Map.of(
                "卵", 33,
                "豆腐", 35,
                "豚こま肉", 10,
                "豚肉", 10
        );
    }

    public Optional<Integer> matchCategoryId(String ingredientName) {
        if (!StringUtils.hasText(ingredientName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ingredientCategoryMap.get(ingredientName.trim()));
    }

    public List<Integer> matchCategoryIds(List<String> ingredientNames) {
        Map<Integer, Boolean> categoryIds = new LinkedHashMap<>();

        ingredientNames.stream()
                .map(this::matchCategoryId)
                .flatMap(Optional::stream)
                .forEach(categoryId -> categoryIds.putIfAbsent(categoryId, true));

        return categoryIds.keySet().stream().toList();
    }
}
