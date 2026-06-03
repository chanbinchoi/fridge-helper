package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.config.NotionProperties;
import com.nagoya.fridge.notion.NotionClient;
import com.nagoya.fridge.notion.NotionRawPage;
import com.nagoya.fridge.notion.NotionRawQueryResult;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

@Service
public class IngredientRawDataService {

    private static final Logger log = LoggerFactory.getLogger(IngredientRawDataService.class);
    private static final ZoneId TOKYO_ZONE = ZoneId.of("Asia/Tokyo");

    private final NotionClient notionClient;
    private final NotionProperties notionProperties;
    private final Clock clock;

    @Autowired
    public IngredientRawDataService(NotionClient notionClient, NotionProperties notionProperties) {
        this(notionClient, notionProperties, Clock.system(TOKYO_ZONE));
    }

    IngredientRawDataService(NotionClient notionClient, NotionProperties notionProperties, Clock clock) {
        this.notionClient = notionClient;
        this.notionProperties = notionProperties;
        this.clock = clock;
    }

    public NotionRawQueryResult fetchRawIngredients() {
        return notionClient.fetchRawDatabasePages();
    }

    public IngredientNamesResult fetchIngredientNames() {
        NotionRawQueryResult rawIngredients = fetchRawIngredients();
        Set<String> names = new LinkedHashSet<>();
        log.info("Notion raw ingredient page count: {}", rawIngredients.pages().size());

        rawIngredients.pages().stream()
                .filter(page -> !page.archived() && !page.inTrash())
                .map(this::extractIngredientName)
                .flatMap(Optional::stream)
                .forEach(names::add);

        log.info("Ingredient names extracted after applying the Notion stock filter: {}", names);

        return new IngredientNamesResult(
                Instant.now(),
                names.size(),
                names.stream().toList()
        );
    }

    public IngredientItemsResult fetchIngredientItems() {
        NotionRawQueryResult rawIngredients = fetchRawIngredients();
        LocalDate today = LocalDate.now(clock.withZone(TOKYO_ZONE));
        List<IngredientItemDto> items = rawIngredients.pages().stream()
                .filter(page -> !page.archived() && !page.inTrash())
                .map(page -> toIngredientItem(page, today))
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparing(
                                IngredientItemDto::daysRemaining,
                                Comparator.nullsLast(Integer::compareTo)
                        )
                        .thenComparing(IngredientItemDto::name))
                .toList();

        log.info("Created {} ingredient items for the frontend: {}", items.size(), items);

        return new IngredientItemsResult(
                Instant.now(),
                items.size(),
                items
        );
    }

    private Optional<IngredientItemDto> toIngredientItem(NotionRawPage page, LocalDate today) {
        Optional<String> name = extractIngredientName(page);
        if (name.isEmpty()) {
            return Optional.empty();
        }

        Optional<LocalDate> expirationDate = extractExpirationDate(page);
        Integer daysRemaining = expirationDate
                .map(date -> (int) ChronoUnit.DAYS.between(today, date))
                .orElse(null);

        return Optional.of(new IngredientItemDto(
                page.id(),
                name.get(),
                expirationDate.orElse(null),
                daysRemaining
        ));
    }

    private Optional<String> extractIngredientName(NotionRawPage page) {
        String configuredPropertyName = normalizeConfiguredText(notionProperties.ingredientNameProperty());

        if (StringUtils.hasText(configuredPropertyName)) {
            Optional<String> configuredName = extractTitleText(page.properties().path(configuredPropertyName));
            if (configuredName.isPresent()) {
                return configuredName;
            }
            log.info(
                    "Configured ingredient name property '{}' did not contain a title value. Searching title properties on page {}.",
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

    private Optional<LocalDate> extractExpirationDate(NotionRawPage page) {
        String expirationDateProperty = normalizeConfiguredText(notionProperties.expirationDateProperty());
        if (!StringUtils.hasText(expirationDateProperty)) {
            return Optional.empty();
        }

        JsonNode property = page.properties().path(expirationDateProperty);
        if (!"date".equals(property.path("type").asText())) {
            return Optional.empty();
        }

        String start = property.path("date").path("start").asText(null);
        if (!StringUtils.hasText(start)) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(start.substring(0, Math.min(start.length(), 10))));
        } catch (RuntimeException exception) {
            log.info("Failed to parse 使用期限: page {}, value {}", page.id(), start);
            return Optional.empty();
        }
    }
}
