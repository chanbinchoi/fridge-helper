package com.nagoya.recipe.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class RecipeSearchResultDto {

    private final String title;
    private final String linkUrl;
    private final String imageUrl;

    @Builder.Default
    private final List<String> materials = List.of();

    @Builder.Default
    private final double matchRate = 0.0;

    @Builder.Default
    private final List<String> missingMaterials = List.of();
}
