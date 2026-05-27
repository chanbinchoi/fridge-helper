package com.nagoya.fridge.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import com.nagoya.fridge.config.NotionProperties;
import com.nagoya.fridge.notion.NotionClient;
import com.nagoya.fridge.notion.NotionRawPage;
import com.nagoya.fridge.notion.NotionRawQueryResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class IngredientRawDataServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fetchIngredientNamesExtractsConfiguredJapaneseTitleProperty() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(
                        notionPage("page-1", false, false, "卵"),
                        notionPage("page-2", false, false, "牛乳")
                )),
                notionProperties("食材名")
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.count()).isEqualTo(2);
        assertThat(result.names()).containsExactly("卵", "牛乳");
    }

    @Test
    void fetchIngredientNamesSkipsArchivedTrashBlankAndDuplicateNames() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(
                        notionPage("page-1", false, false, "卵"),
                        notionPage("page-2", false, false, "卵"),
                        notionPage("page-3", true, false, "牛乳"),
                        notionPage("page-4", false, true, "豆腐"),
                        notionPage("page-5", false, false, "   ")
                )),
                notionProperties("食材名")
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.count()).isEqualTo(1);
        assertThat(result.names()).containsExactly("卵");
    }

    @Test
    void fetchIngredientNamesOnlyIncludesPagesWithInStockStatus() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(
                        notionPage("page-1", false, false, "卵", "在庫あり"),
                        notionPage("page-2", false, false, "牛乳", "在庫なし"),
                        notionPage("page-3", false, false, "豆腐", "")
                )),
                notionProperties("食材名")
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.count()).isEqualTo(1);
        assertThat(result.names()).containsExactly("卵");
    }

    @Test
    void fetchIngredientNamesFindsFirstTitlePropertyWhenNamePropertyIsNotConfigured() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(notionPage("page-1", false, false, "味噌"))),
                notionProperties("")
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.names()).containsExactly("味噌");
    }

    @Test
    void fetchIngredientNamesFallsBackToTitlePropertyWhenConfiguredNamePropertyDoesNotMatch() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(notionPage("page-1", false, false, "豆腐"))),
                notionProperties("実際には存在しない列")
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.names()).containsExactly("豆腐");
    }

    @Test
    void fetchIngredientNamesFallsBackToStatusPropertyWhenConfiguredStockPropertyDoesNotMatch() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(notionPage("page-1", false, false, "卵"))),
                new NotionProperties(
                        "notion-secret",
                        "database-id",
                        "https://api.notion.test",
                        "2026-03-11",
                        "食材名",
                        "実際には存在しない在庫列",
                        "在庫あり"
                )
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.names()).containsExactly("卵");
    }

    @Test
    void fetchIngredientNamesRepairsMojibakeJapaneseConfigurationValues() throws Exception {
        IngredientRawDataService service = new IngredientRawDataService(
                notionClient(List.of(notionPage("page-1", false, false, "卵"))),
                new NotionProperties(
                        "notion-secret",
                        "database-id",
                        "https://api.notion.test",
                        "2026-03-11",
                        "食材名",
                        "å¨åº«ã¹ãã¼ã¿ã¹",
                        "å¨åº«ãã"
                )
        );

        IngredientNamesResult result = service.fetchIngredientNames();

        assertThat(result.names()).containsExactly("卵");
    }

    private NotionClient notionClient(List<NotionRawPage> pages) {
        return new NotionClient(
                notionProperties("食材名"),
                RestClient.builder().baseUrl("https://api.notion.test").build(),
                objectMapper
        ) {
            @Override
            public NotionRawQueryResult fetchRawDatabasePages() {
                return new NotionRawQueryResult(
                        "database-id",
                        "data-source-id",
                        Instant.parse("2026-05-26T00:00:00Z"),
                        pages.size(),
                        pages
                );
            }
        };
    }

    private NotionRawPage notionPage(String id, boolean archived, boolean inTrash, String name) throws Exception {
        return notionPage(id, archived, inTrash, name, "在庫あり");
    }

    private NotionRawPage notionPage(
            String id,
            boolean archived,
            boolean inTrash,
            String name,
            String stockStatus
    ) throws Exception {
        return new NotionRawPage(
                id,
                "https://notion.so/" + id,
                "2026-05-25T10:00:00.000Z",
                "2026-05-25T11:00:00.000Z",
                archived,
                inTrash,
                objectMapper.readTree("""
                        {
                          "食材名": {
                            "type": "title",
                            "title": [{ "plain_text": "%s" }]
                          },
                          "在庫ステータス": {
                            "type": "status",
                            "status": {
                              "name": "%s"
                            }
                          }
                        }
                        """.formatted(name, stockStatus))
        );
    }

    private NotionProperties notionProperties(String ingredientNameProperty) {
        return new NotionProperties(
                "notion-secret",
                "database-id",
                "https://api.notion.test",
                "2026-03-11",
                ingredientNameProperty,
                "在庫ステータス",
                "在庫あり"
        );
    }
}
