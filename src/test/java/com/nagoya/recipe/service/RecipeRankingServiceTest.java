package com.nagoya.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import com.nagoya.recipe.rakuten.RakutenRecipeApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RecipeRankingServiceTest {

    @Test
    void generalRankingCallsRakutenApiWithoutCategoryIdAndReturnsTopFive() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openapi.rakuten.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), request -> {
                    String uri = request.getURI().toString();
                    assertThat(uri).startsWith("https://openapi.rakuten.co.jp/recipems/api/Recipe/CategoryRanking/20170426?");
                    assertThat(uri).contains("format=json");
                    assertThat(uri).contains("formatVersion=2");
                    assertThat(uri).contains("applicationId=app-id");
                    assertThat(uri).contains("accessKey=access-key");
                    assertThat(uri).doesNotContain("categoryId=");
                })
                .andExpect(header("accessKey", "access-key"))
                .andExpect(header("Referer", "https://policies-soonest-essex-amounts.trycloudflare.com"))
                .andRespond(withSuccess("""
                        {
                          "result": [
                            {
                              "recipeTitle": "人気レシピ1",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/1/",
                              "foodImageUrl": "https://recipe.r10s.jp/1.jpg"
                            },
                            {
                              "recipeTitle": "人気レシピ2",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/2/",
                              "foodImageUrl": "https://recipe.r10s.jp/2.jpg"
                            },
                            {
                              "recipeTitle": "人気レシピ3",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/3/",
                              "foodImageUrl": "https://recipe.r10s.jp/3.jpg"
                            },
                            {
                              "recipeTitle": "人気レシピ4",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/4/",
                              "foodImageUrl": "https://recipe.r10s.jp/4.jpg"
                            },
                            {
                              "recipeTitle": "人気レシピ5",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/5/",
                              "foodImageUrl": "https://recipe.r10s.jp/5.jpg"
                            },
                            {
                              "recipeTitle": "人気レシピ6",
                              "recipeUrl": "https://recipe.rakuten.co.jp/recipe/6/",
                              "foodImageUrl": "https://recipe.r10s.jp/6.jpg"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RecipeRankingService service = rankingService(builder.build());

        List<RecipeSearchResultDto> recipes = service.generalRanking();

        assertThat(recipes).hasSize(5);
        assertThat(recipes).extracting(RecipeSearchResultDto::getTitle)
                .containsExactly("人気レシピ1", "人気レシピ2", "人気レシピ3", "人気レシピ4", "人気レシピ5");
        server.verify();
    }

    @Test
    void proteinRankingCallsExplicitProteinCategoryIdsAndDeduplicatesTopFive() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://openapi.rakuten.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(once(), request -> assertThat(request.getURI().toString()).contains("categoryId=10-277-1119"))
                .andExpect(header("accessKey", "access-key"))
                .andExpect(header("Referer", "https://policies-soonest-essex-amounts.trycloudflare.com"))
                .andRespond(withSuccess(rankingResponse("chicken", "shared"), MediaType.APPLICATION_JSON));
        server.expect(once(), request -> assertThat(request.getURI().toString()).contains("categoryId=30-315"))
                .andExpect(header("accessKey", "access-key"))
                .andExpect(header("Referer", "https://policies-soonest-essex-amounts.trycloudflare.com"))
                .andRespond(withSuccess(rankingResponse("shared", "tofu", "tofu2", "tofu3"), MediaType.APPLICATION_JSON));

        RecipeRankingService service = rankingService(builder.build());

        List<RecipeSearchResultDto> recipes = service.proteinRanking();

        assertThat(recipes).hasSize(5);
        assertThat(recipes).extracting(RecipeSearchResultDto::getLinkUrl)
                .containsExactly(
                        "https://recipe.rakuten.co.jp/recipe/chicken/",
                        "https://recipe.rakuten.co.jp/recipe/shared/",
                        "https://recipe.rakuten.co.jp/recipe/tofu/",
                        "https://recipe.rakuten.co.jp/recipe/tofu2/",
                        "https://recipe.rakuten.co.jp/recipe/tofu3/"
                );
        server.verify();
    }

    @Test
    void rankingFailsWhenConfigurationIsMissing() {
        RecipeRankingService service = new RecipeRankingService(
                "",
                "access-key",
                RestClient.builder().baseUrl("https://openapi.rakuten.test").build()
        );

        assertThatThrownBy(service::generalRanking)
                .isInstanceOf(RakutenRecipeApiException.class)
                .hasMessageContaining("Missing rakuten.recipe.application-id");
    }

    private RecipeRankingService rankingService(RestClient restClient) {
        return new RecipeRankingService(
                "app-id",
                "access-key",
                restClient
        );
    }

    private String rankingResponse(String... recipeIds) {
        return """
                {
                  "result": [
                    %s
                  ]
                }
                """.formatted(String.join(",", List.of(recipeIds).stream()
                .map(this::rankingItem)
                .toList()));
    }

    private String rankingItem(String recipeId) {
        return """
                {
                  "recipeTitle": "%s recipe",
                  "recipeUrl": "https://recipe.rakuten.co.jp/recipe/%s/",
                  "foodImageUrl": "https://recipe.r10s.jp/%s.jpg"
                }
                """.formatted(recipeId, recipeId, recipeId);
    }
}
