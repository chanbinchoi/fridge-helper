package com.nagoya.fridge.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nagoya.fridge.config.NotionProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class NotionClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-26T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void fetchRawDatabasePagesQueriesAllPagesFromPrimaryDataSource() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.notion.test")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Notion-Version", "2026-03-11");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), requestTo("https://api.notion.test/v1/databases/database-id"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer notion-secret"))
                .andRespond(withSuccess("""
                        {
                          "object": "database",
                          "data_sources": [{ "id": "data-source-id" }]
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://api.notion.test/v1/data_sources/data-source-id/query"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "object": "list",
                          "results": [
                            {
                              "id": "page-1",
                              "url": "https://notion.so/page-1",
                              "created_time": "2026-05-25T10:00:00.000Z",
                              "last_edited_time": "2026-05-25T11:00:00.000Z",
                              "archived": false,
                              "in_trash": false,
                              "properties": {
                                "Name": {
                                  "type": "title",
                                  "title": [{ "plain_text": "Eggs" }]
                                }
                              }
                            }
                          ],
                          "has_more": true,
                          "next_cursor": "cursor-2"
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://api.notion.test/v1/data_sources/data-source-id/query"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "object": "list",
                          "results": [
                            {
                              "id": "page-2",
                              "url": "https://notion.so/page-2",
                              "created_time": "2026-05-26T10:00:00.000Z",
                              "last_edited_time": "2026-05-26T11:00:00.000Z",
                              "archived": false,
                              "in_trash": false,
                              "properties": {
                                "Name": {
                                  "type": "title",
                                  "title": [{ "plain_text": "Milk" }]
                                }
                              }
                            }
                          ],
                          "has_more": false,
                          "next_cursor": null
                        }
                        """, MediaType.APPLICATION_JSON));

        NotionClient client = new NotionClient(
                new NotionProperties("notion-secret", "database-id", "https://api.notion.test", "2026-03-11"),
                builder.build(),
                objectMapper,
                clock
        );

        NotionRawQueryResult result = client.fetchRawDatabasePages();

        assertThat(result.databaseId()).isEqualTo("database-id");
        assertThat(result.dataSourceId()).isEqualTo("data-source-id");
        assertThat(result.fetchedAt()).isEqualTo(Instant.parse("2026-05-26T00:00:00Z"));
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.pages()).extracting(NotionRawPage::id).containsExactly("page-1", "page-2");
        assertThat(result.pages().get(0).properties().path("Name").path("title").get(0).path("plain_text").asText())
                .isEqualTo("Eggs");

        server.verify();
    }

    @Test
    void fetchRawDatabasePagesFailsWhenConfigurationIsMissing() {
        NotionClient client = new NotionClient(
                new NotionProperties("", "database-id", "https://api.notion.test", "2026-03-11"),
                RestClient.builder().baseUrl("https://api.notion.test").build(),
                objectMapper,
                clock
        );

        assertThatThrownBy(client::fetchRawDatabasePages)
                .isInstanceOf(NotionClientException.class)
                .hasMessageContaining("Missing notion.api-key");
    }
}
