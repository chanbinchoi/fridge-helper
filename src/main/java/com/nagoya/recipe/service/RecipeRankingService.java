package com.nagoya.recipe.service;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.rakuten.RakutenRecipeApiException;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Service
public class RecipeRankingService {

    private static final Logger log = LoggerFactory.getLogger(RecipeRankingService.class);
    private static final int RANKING_LIMIT = 5;
    private static final String RANKING_API_URL = "https://openapi.rakuten.co.jp/recipems/api/Recipe/CategoryRanking/20170426";
    private static final String RAKUTEN_API_REFERER = "https://policies-soonest-essex-amounts.trycloudflare.com";
    private static final List<String> PROTEIN_CATEGORY_IDS = List.of(
            "10-277-1119",
            "30-315",
            "10"
    );
    private static final String OUTPUT_ELEMENTS = String.join(",",
            "recipeTitle",
            "recipeUrl",
            "foodImageUrl",
            "mediumImageUrl",
            "smallImageUrl"
    );

    private final String applicationId;
    private final String accessKey;
    private final RestClient restClient;

    @Autowired
    public RecipeRankingService(
            @Value("${rakuten.recipe.application-id:${RAKUTEN_API_KEY:}}") String applicationId,
            @Value("${rakuten.recipe.access-key:}") String accessKey,
            @Qualifier("rakutenRecipeRestClient") RestClient restClient
    ) {
        this.applicationId = normalize(applicationId);
        this.accessKey = normalize(accessKey);
        this.restClient = restClient;
        log.info("Rakuten Recipe applicationId loaded: present={}, value={}",
                StringUtils.hasText(this.applicationId),
                masked(this.applicationId));
    }

    public List<RecipeSearchResultDto> generalRanking() {
        return fetchRanking(null).stream()
                .limit(RANKING_LIMIT)
                .toList();
    }

    public List<RecipeSearchResultDto> proteinRanking() {
        Map<String, RecipeSearchResultDto> recipesByLinkUrl = new LinkedHashMap<>();

        for (String categoryId : PROTEIN_CATEGORY_IDS) {
            fetchRanking(categoryId).stream()
                    .limit(RANKING_LIMIT)
                    .forEach(recipe -> recipesByLinkUrl.putIfAbsent(recipe.getLinkUrl(), recipe));
            if (recipesByLinkUrl.size() >= RANKING_LIMIT) {
                break;
            }
        }

        return recipesByLinkUrl.values().stream()
                .limit(RANKING_LIMIT)
                .toList();
    }

    private List<RecipeSearchResultDto> fetchRanking(String categoryId) {
        validateConfiguration();

        try {
            String uri = rankingUri(categoryId);
            log.info("Calling Rakuten Recipe ranking API: uri={}, referer={}",
                    maskedApplicationIdInUri(uri),
                    RAKUTEN_API_REFERER);
            JsonNode response = restClient.get()
                    .uri(uri)
                    .header("accessKey", accessKey)
                    .header(HttpHeaders.REFERER, RAKUTEN_API_REFERER)
                    .retrieve()
                    .body(JsonNode.class);

            return parseRanking(response);
        } catch (RestClientResponseException exception) {
            throw new RakutenRecipeApiException(rakutenErrorMessage(exception), exception);
        } catch (RuntimeException exception) {
            if (exception instanceof RakutenRecipeApiException rakutenRecipeApiException) {
                throw rakutenRecipeApiException;
            }
            throw new RakutenRecipeApiException("Failed to call Rakuten Recipe ranking API.", exception);
        }
    }

    private String rankingUri(String categoryId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(RANKING_API_URL)
                .queryParam("format", "json")
                .queryParam("formatVersion", "2")
                .queryParam("applicationId", applicationId)
                .queryParam("accessKey", accessKey)
                .queryParam("elements", OUTPUT_ELEMENTS);

        if (StringUtils.hasText(categoryId)) {
            builder.queryParam("categoryId", categoryId.trim());
        }

        return builder.build().encode().toUriString();
    }

    private List<RecipeSearchResultDto> parseRanking(JsonNode response) {
        JsonNode results = response == null ? null : response.path("result");
        if (results == null || !results.isArray()) {
            throw new RakutenRecipeApiException("Rakuten Recipe ranking API response did not include result.");
        }

        List<RecipeSearchResultDto> recipes = new ArrayList<>();
        for (JsonNode result : results) {
            RecipeSearchResultDto recipe = toRecipe(result);
            if (recipe != null) {
                recipes.add(recipe);
            }
            if (recipes.size() >= RANKING_LIMIT) {
                break;
            }
        }
        return recipes;
    }

    private RecipeSearchResultDto toRecipe(JsonNode node) {
        String title = node.path("recipeTitle").asText("");
        String linkUrl = node.path("recipeUrl").asText("");
        String imageUrl = firstText(
                node.path("foodImageUrl"),
                node.path("mediumImageUrl"),
                node.path("smallImageUrl")
        );

        if (!StringUtils.hasText(title) || !StringUtils.hasText(linkUrl) || !StringUtils.hasText(imageUrl)) {
            return null;
        }

        return RecipeSearchResultDto.builder()
                .title(title.trim())
                .linkUrl(linkUrl.trim())
                .imageUrl(imageUrl.trim())
                .build();
    }

    private String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = node.asText("");
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String rakutenErrorMessage(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        return StringUtils.hasText(body)
                ? "Rakuten Recipe API returned " + exception.getStatusCode() + ": " + body
                : "Rakuten Recipe API returned " + exception.getStatusCode();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(applicationId)) {
            throw new RakutenRecipeApiException("Missing rakuten.recipe.application-id. Set RAKUTEN_RECIPE_APPLICATION_ID or RAKUTEN_API_KEY.");
        }
        if (!StringUtils.hasText(accessKey)) {
            throw new RakutenRecipeApiException("Missing rakuten.recipe.access-key. Set RAKUTEN_RECIPE_ACCESS_KEY.");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String masked(String value) {
        if (!StringUtils.hasText(value)) {
            return "(empty)";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String maskedApplicationIdInUri(String uri) {
        return uri.replace("applicationId=" + applicationId, "applicationId=" + masked(applicationId));
    }
}
