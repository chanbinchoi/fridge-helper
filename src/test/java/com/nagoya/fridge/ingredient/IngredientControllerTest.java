package com.nagoya.fridge.ingredient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nagoya.fridge.config.NotionProperties;
import com.nagoya.fridge.notion.NotionRawPage;
import com.nagoya.fridge.notion.NotionRawQueryResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

class IngredientControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getRawIngredientsReturnsRawNotionPages() throws Exception {
        NotionRawQueryResult result = new NotionRawQueryResult(
                "database-id",
                "data-source-id",
                Instant.parse("2026-05-26T00:00:00Z"),
                1,
                List.of(new NotionRawPage(
                        "page-1",
                        "https://notion.so/page-1",
                        "2026-05-25T10:00:00.000Z",
                        "2026-05-25T11:00:00.000Z",
                        false,
                        false,
                        objectMapper.readTree("""
                                {
                                  "Name": {
                                    "type": "title",
                                    "title": [{ "plain_text": "Eggs" }]
                                  }
                                }
                                """)
                ))
        );
        IngredientRawDataService service = new IngredientRawDataService(null, notionProperties("")) {
            @Override
            public NotionRawQueryResult fetchRawIngredients() {
                return result;
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new IngredientController(service))
                .build();

        mockMvc.perform(get("/api/fridge/ingredients/raw"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.databaseId").value("database-id"))
                .andExpect(jsonPath("$.dataSourceId").value("data-source-id"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.pages[0].id").value("page-1"))
                .andExpect(jsonPath("$.pages[0].properties.Name.title[0].plain_text").value("Eggs"));
    }

    @Test
    void getIngredientNamesReturnsExtractedNames() throws Exception {
        NotionRawQueryResult result = new NotionRawQueryResult(
                "database-id",
                "data-source-id",
                Instant.parse("2026-05-26T00:00:00Z"),
                2,
                List.of(
                        notionPage("page-1", false, false, "卵"),
                        notionPage("page-2", false, false, "牛乳", "在庫なし")
                )
        );
        IngredientRawDataService service = new IngredientRawDataService(null, notionProperties("食材名")) {
            @Override
            public NotionRawQueryResult fetchRawIngredients() {
                return result;
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new IngredientController(service))
                .build();

        mockMvc.perform(get("/api/fridge/ingredients/names"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.names[0]").value("卵"));
    }

    @Test
    void getIngredientItemsReturnsOriginalNamesAndDaysRemaining() throws Exception {
        NotionRawQueryResult result = new NotionRawQueryResult(
                "database-id",
                "data-source-id",
                Instant.parse("2026-05-26T00:00:00Z"),
                1,
                List.of(notionPage("page-1", false, false, "もめん豆腐", "在庫なし", "2026-06-01"))
        );
        IngredientRawDataService service = new IngredientRawDataService(null, notionProperties("食材名")) {
            @Override
            public NotionRawQueryResult fetchRawIngredients() {
                return result;
            }
        };

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new IngredientController(service))
                .build();

        mockMvc.perform(get("/api/fridge/ingredients/items"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].name").value("もめん豆腐"))
                .andExpect(jsonPath("$.items[0].expirationDate").value("2026-06-01"));
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
        return notionPage(id, archived, inTrash, name, stockStatus, "2026-06-09");
    }

    private NotionRawPage notionPage(
            String id,
            boolean archived,
            boolean inTrash,
            String name,
            String stockStatus,
            String expirationDate
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
                          },
                          "使用期限": {
                            "type": "date",
                            "date": {
                              "start": "%s"
                            }
                          }
                        }
                        """.formatted(name, stockStatus, expirationDate))
        );
    }

    private NotionProperties notionProperties(String ingredientNameProperty) {
        return new NotionProperties(
                "notion-secret",
                "database-id",
                "https://api.notion.test",
                "2026-03-11",
                ingredientNameProperty,
                "使用期限",
                "在庫ステータス",
                "在庫あり"
        );
    }
}
