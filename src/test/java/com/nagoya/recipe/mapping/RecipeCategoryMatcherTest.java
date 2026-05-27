package com.nagoya.recipe.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeCategoryMatcherTest {

    private final RecipeCategoryMatcher matcher = new RecipeCategoryMatcher();

    @Test
    void matchCategoryIdsReturnsMappedRakutenCategoryIdsInOrder() {
        List<Integer> categoryIds = matcher.matchCategoryIds(List.of("卵", "豆腐", "豚こま肉", "豚肉", "未対応"));

        assertThat(categoryIds).containsExactly(33, 35, 10);
    }
}
