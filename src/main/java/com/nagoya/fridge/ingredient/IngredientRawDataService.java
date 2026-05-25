package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.notion.NotionClient;
import com.nagoya.fridge.notion.NotionRawQueryResult;
import org.springframework.stereotype.Service;

@Service
public class IngredientRawDataService {

    private final NotionClient notionClient;

    public IngredientRawDataService(NotionClient notionClient) {
        this.notionClient = notionClient;
    }

    public NotionRawQueryResult fetchRawIngredients() {
        return notionClient.fetchRawDatabasePages();
    }
}
