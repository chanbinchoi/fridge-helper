package com.nagoya.fridge.ingredient;

import java.time.Instant;
import java.util.List;

public record IngredientNamesResult(
        Instant fetchedAt,
        int count,
        List<String> names
) {
}
