package com.nagoya.fridge.notion;

import java.time.Instant;
import java.util.List;

public record NotionRawQueryResult(
        String databaseId,
        String dataSourceId,
        Instant fetchedAt,
        int count,
        List<NotionRawPage> pages
) {
}
