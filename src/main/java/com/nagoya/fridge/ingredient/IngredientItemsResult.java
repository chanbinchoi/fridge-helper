package com.nagoya.fridge.ingredient;

import java.time.Instant;
import java.util.List;

public record IngredientItemsResult(
        Instant fetchedAt,
        int count,
        List<IngredientItemDto> items
) {
}
