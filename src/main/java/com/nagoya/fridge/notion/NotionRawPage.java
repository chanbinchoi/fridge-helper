package com.nagoya.fridge.notion;

import tools.jackson.databind.JsonNode;

public record NotionRawPage(
        String id,
        String url,
        String createdTime,
        String lastEditedTime,
        boolean archived,
        boolean inTrash,
        JsonNode properties
) {
}
