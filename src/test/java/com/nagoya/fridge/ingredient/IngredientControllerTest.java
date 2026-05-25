package com.nagoya.fridge.ingredient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        IngredientRawDataService service = new IngredientRawDataService(null) {
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
                .andExpect(jsonPath("$.databaseId").value("database-id"))
                .andExpect(jsonPath("$.dataSourceId").value("data-source-id"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.pages[0].id").value("page-1"))
                .andExpect(jsonPath("$.pages[0].properties.Name.title[0].plain_text").value("Eggs"));
    }
}
