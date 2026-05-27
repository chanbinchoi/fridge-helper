package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.config.NotionProperties;
import com.nagoya.fridge.notion.NotionClient;
import com.nagoya.fridge.notion.NotionRawPage;
import com.nagoya.fridge.notion.NotionRawQueryResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

@Service
public class IngredientRawDataService {

    private static final Logger log = LoggerFactory.getLogger(IngredientRawDataService.class);

    private final NotionClient notionClient;
    private final NotionProperties notionProperties;

    public IngredientRawDataService(NotionClient notionClient, NotionProperties notionProperties) {
        this.notionClient = notionClient;
        this.notionProperties = notionProperties;
    }

    public NotionRawQueryResult fetchRawIngredients() {
        return notionClient.fetchRawDatabasePages();
    }

    public IngredientNamesResult fetchIngredientNames() {
        NotionRawQueryResult rawIngredients = fetchRawIngredients();
        Set<String> names = new LinkedHashSet<>();
        log.info("Notion 원본 식재료 페이지 수: {}", rawIngredients.pages().size());

        rawIngredients.pages().stream()
                .filter(page -> !page.archived() && !page.inTrash())
                .filter(this::isInStock)
                .map(this::extractIngredientName)
                .flatMap(Optional::stream)
                .forEach(names::add);

        log.info("재고 필터 적용 후 추출된 식재료명: {}", names);

        return new IngredientNamesResult(
                Instant.now(),
                names.size(),
                names.stream().toList()
        );
    }

    private Optional<String> extractIngredientName(NotionRawPage page) {
        String configuredPropertyName = normalizeConfiguredText(notionProperties.ingredientNameProperty());

        if (StringUtils.hasText(configuredPropertyName)) {
            Optional<String> configuredName = extractTitleText(page.properties().path(configuredPropertyName));
            if (configuredName.isPresent()) {
                return configuredName;
            }
            log.info(
                    "설정된 식재료명 컬럼 '{}'에서 title 값을 찾지 못해 page {}의 title 속성을 자동 탐색합니다.",
                    configuredPropertyName.trim(),
                    page.id()
            );
        }

        for (Map.Entry<String, JsonNode> property : page.properties().properties()) {
            Optional<String> name = extractTitleText(property.getValue());
            if (name.isPresent()) {
                return name;
            }
        }

        return Optional.empty();
    }

    private boolean isInStock(NotionRawPage page) {
        String stockStatusProperty = normalizeConfiguredText(notionProperties.stockStatusProperty());
        String inStockValue = normalizeConfiguredText(notionProperties.inStockValue());

        if (!StringUtils.hasText(inStockValue)) {
            return false;
        }

        if (StringUtils.hasText(stockStatusProperty)) {
            JsonNode property = page.properties().path(stockStatusProperty);
            if (isInStockProperty(property, inStockValue)) {
                return true;
            }
            log.info(
                    "설정된 재고 컬럼 '{}'에서 '{}' 값을 찾지 못했습니다. page {}의 status/select 속성을 자동 탐색합니다.",
                    stockStatusProperty.trim(),
                    inStockValue.trim(),
                    page.id()
            );
        }

        for (Map.Entry<String, JsonNode> property : page.properties().properties()) {
            if (isInStockProperty(property.getValue(), inStockValue)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInStockProperty(JsonNode property, String inStockValue) {
        String type = property.path("type").asText();
        String value = switch (type) {
            case "status" -> property.path("status").path("name").asText();
            case "select" -> property.path("select").path("name").asText();
            default -> "";
        };
        return inStockValue.trim().equals(value.trim());
    }

    private String normalizeConfiguredText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        String trimmed = value.trim();
        if (!looksLikeLatin1Mojibake(trimmed)) {
            return trimmed;
        }

        String decoded = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
        return decoded.contains("\uFFFD") ? trimmed : decoded;
    }

    private boolean looksLikeLatin1Mojibake(String value) {
        return value.chars().allMatch(character -> character <= 0xFF)
                && value.chars().anyMatch(character -> character >= 0x80);
    }

    private Optional<String> extractTitleText(JsonNode property) {
        if (!"title".equals(property.path("type").asText())) {
            return Optional.empty();
        }

        JsonNode title = property.path("title");
        if (!title.isArray() || title.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder builder = new StringBuilder();
        title.forEach(text -> builder.append(text.path("plain_text").asText("")));
        String name = builder.toString().trim();

        return StringUtils.hasText(name) ? Optional.of(name) : Optional.empty();
    }
}
