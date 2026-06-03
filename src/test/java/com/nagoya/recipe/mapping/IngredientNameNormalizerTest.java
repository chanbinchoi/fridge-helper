package com.nagoya.recipe.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IngredientNameNormalizerTest {

    private final IngredientNameNormalizer normalizer = new IngredientNameNormalizer();

    @Test
    void matchesHiraganaKatakanaHalfWidthAndKanjiIngredientVariants() {
        assertThat(normalizer.matches("卵", "たまご")).isTrue();
        assertThat(normalizer.matches("ごま油", "ゴマ油")).isTrue();
        assertThat(normalizer.matches("ごま油", "胡麻油")).isTrue();
        assertThat(normalizer.matches("ｺﾞﾏ油", "胡麻油")).isTrue();
    }

    @Test
    void matchesContainedVariantTextAfterRemovingPunctuationAndSpaces() {
        assertThat(normalizer.matches("ごま油", "ごま油(ゴマ油, 胡麻油)")).isTrue();
        assertThat(normalizer.matches("玉葱", "玉ねぎ 1/2個")).isTrue();
    }
}
