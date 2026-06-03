package com.nagoya.fridge.ingredient;

import java.time.LocalDate;

public record IngredientItemDto(
        String id,
        String name,
        LocalDate expirationDate,
        Integer daysRemaining
) {
}
