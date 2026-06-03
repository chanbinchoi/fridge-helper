package com.nagoya.recipe.mapping;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class IngredientNameNormalizer {

    private static final double SIMILARITY_THRESHOLD = 0.72;
    private static final List<KanjiReading> KANJI_READINGS = List.of(
            new KanjiReading("玉葱", "たまねぎ"),
            new KanjiReading("玉子", "たまご"),
            new KanjiReading("玉ねぎ", "たまねぎ"),
            new KanjiReading("胡麻", "ごま"),
            new KanjiReading("卵", "たまご"),
            new KanjiReading("葱", "ねぎ")
    );

    public String normalizeForLookup(String value) {
        return normalize(value);
    }

    public boolean matches(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);

        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return false;
        }
        if (normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft)) {
            return true;
        }

        return diceCoefficient(normalizedLeft, normalizedRight) >= SIMILARITY_THRESHOLD;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[\\p{Punct}、。・（）()［］\\[\\]【】「」『』〈〉<>]", "");

        normalized = katakanaToHiragana(normalized);
        for (KanjiReading reading : KANJI_READINGS) {
            normalized = normalized.replace(reading.kanji(), reading.reading());
        }

        return normalized;
    }

    private String katakanaToHiragana(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= '\u30A1' && character <= '\u30F6') {
                builder.append((char) (character - 0x60));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private double diceCoefficient(String left, String right) {
        Set<String> leftBigrams = bigrams(left);
        Set<String> rightBigrams = bigrams(right);

        if (leftBigrams.isEmpty() || rightBigrams.isEmpty()) {
            return 0.0;
        }

        int intersection = 0;
        for (String bigram : leftBigrams) {
            if (rightBigrams.contains(bigram)) {
                intersection++;
            }
        }

        return (2.0 * intersection) / (leftBigrams.size() + rightBigrams.size());
    }

    private Set<String> bigrams(String value) {
        Set<String> bigrams = new HashSet<>();
        if (value.length() < 2) {
            bigrams.add(value);
            return bigrams;
        }

        for (int index = 0; index < value.length() - 1; index++) {
            bigrams.add(value.substring(index, index + 2));
        }
        return bigrams;
    }

    private record KanjiReading(String kanji, String reading) {
    }
}
