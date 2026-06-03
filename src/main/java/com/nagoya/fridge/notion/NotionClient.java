package com.nagoya.fridge.notion;

import com.nagoya.fridge.config.NotionProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotionClient {

    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);
    private static final int PAGE_SIZE = 100;

    private final NotionProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public NotionClient(
            NotionProperties properties,
            RestClient notionRestClient,
            ObjectMapper objectMapper
    ) {
        this(properties, notionRestClient, objectMapper, Clock.systemUTC());
    }

    NotionClient(
            NotionProperties properties,
            RestClient notionRestClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.properties = properties;
        this.restClient = notionRestClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public NotionRawQueryResult fetchRawDatabasePages() {
        validateConfiguration();

        String databaseId = properties.databaseId();
        log.info(
                "Starting Notion database query: databaseId={}, filter={}.status == {}",
                databaseId,
                properties.stockStatusProperty(),
                properties.inStockValue()
        );
        String dataSourceId = findPrimaryDataSourceId(databaseId);
        List<NotionRawPage> pages = queryAllPages(dataSourceId);
        log.info("Completed Notion database query: databaseId={}, dataSourceId={}, fetchedPages={}",
                databaseId,
                dataSourceId,
                pages.size());

        return new NotionRawQueryResult(
                databaseId,
                dataSourceId,
                Instant.now(clock),
                pages.size(),
                List.copyOf(pages)
        );
    }

    private String findPrimaryDataSourceId(String databaseId) {
        JsonNode database = getJson("/v1/databases/{databaseId}", databaseId);
        JsonNode dataSources = database.path("data_sources");

        if (!dataSources.isArray() || dataSources.isEmpty()) {
            throw new NotionClientException("Notion database has no data source: " + databaseId);
        }

        String dataSourceId = dataSources.get(0).path("id").asText();
        if (!StringUtils.hasText(dataSourceId)) {
            throw new NotionClientException("Notion database response did not include a data source id.");
        }
        return dataSourceId;
    }

    private List<NotionRawPage> queryAllPages(String dataSourceId) {
        List<NotionRawPage> pages = new ArrayList<>();
        String nextCursor = null;

        do {
            JsonNode response = postJson(
                    "/v1/data_sources/{dataSourceId}/query",
                    queryBody(nextCursor),
                    dataSourceId
            );

            JsonNode results = response.path("results");
            if (!results.isArray()) {
                throw new NotionClientException("Notion query response did not include results.");
            }

            results.forEach(page -> pages.add(toRawPage(page)));
            nextCursor = response.path("next_cursor").isNull() ? null : response.path("next_cursor").asText(null);
        } while (StringUtils.hasText(nextCursor));

        return pages;
    }

    private Map<String, Object> queryBody(String startCursor) {
        List<Map<String, String>> sorts = List.of(Map.of(
                "timestamp", "last_edited_time",
                "direction", "descending"
        ));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page_size", PAGE_SIZE);
        body.put("filter", stockStatusFilter());
        body.put("sorts", sorts);

        if (StringUtils.hasText(startCursor)) {
            body.put("start_cursor", startCursor);
        }

        return body;
    }

    private Map<String, Object> stockStatusFilter() {
        return Map.of(
                "property", properties.stockStatusProperty().trim(),
                "status", Map.of("equals", properties.inStockValue().trim())
        );
    }

    private NotionRawPage toRawPage(JsonNode page) {
        return new NotionRawPage(
                page.path("id").asText(),
                page.path("url").asText(null),
                page.path("created_time").asText(null),
                page.path("last_edited_time").asText(null),
                page.path("archived").asBoolean(false),
                page.path("in_trash").asBoolean(false),
                page.path("properties").deepCopy()
        );
    }

    private JsonNode getJson(String uri, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(uri, uriVariables)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw notionResponseException(exception);
        } catch (RuntimeException exception) {
            throw new NotionClientException("Failed to call Notion API.", exception);
        }
    }

    private JsonNode postJson(String uri, Object body, Object... uriVariables) {
        try {
            JsonNode response = restClient.post()
                    .uri(uri, uriVariables)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return response == null ? objectMapper.createObjectNode() : response;
        } catch (RestClientResponseException exception) {
            throw notionResponseException(exception);
        } catch (RuntimeException exception) {
            throw new NotionClientException("Failed to call Notion API.", exception);
        }
    }

    private NotionClientException notionResponseException(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        String message = StringUtils.hasText(body)
                ? "Notion API returned " + exception.getStatusCode() + ": " + body
                : "Notion API returned " + exception.getStatusCode();
        return new NotionClientException(message, exception);
    }

    private String bearerToken() {
        return "Bearer " + properties.apiKey();
    }

    private void validateConfiguration() {
        if (isMissingOrPlaceholder(properties.apiKey(), "your_actual_api_key_here")) {
            throw new NotionClientException("Missing notion.api-key. Set NOTION_API_KEY or notion.api-key.");
        }
        if (isMissingOrPlaceholder(properties.databaseId(), "your_actual_database_id_here")) {
            throw new NotionClientException("Missing notion.database-id. Set NOTION_DATABASE_ID or notion.database-id.");
        }
        if (!StringUtils.hasText(properties.stockStatusProperty())) {
            throw new NotionClientException("Missing notion.stock-status-property.");
        }
        if (!StringUtils.hasText(properties.inStockValue())) {
            throw new NotionClientException("Missing notion.in-stock-value.");
        }
    }

    private boolean isMissingOrPlaceholder(String value, String placeholder) {
        return !StringUtils.hasText(value) || placeholder.equals(value.trim());
    }
}
