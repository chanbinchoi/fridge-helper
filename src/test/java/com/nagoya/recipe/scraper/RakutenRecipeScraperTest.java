package com.nagoya.recipe.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class RakutenRecipeScraperTest {

    private final RakutenRecipeScraper scraper = new RakutenRecipeScraper();

    @Test
    void parsePopularRecipesExtractsTitleLinkAndImageUrl() {
        List<RecipeSearchResultDto> results = scraper.parsePopularRecipes("""
                <html>
                  <body>
                    <ul>
                      <li class="recipe_ranking__item">
                        <a href="/recipe/1234567890/" class="recipe_ranking__link">
                          <figure>
                            <img src="https://recipe.r10s.jp/sample.jpg" alt="sample">
                          </figure>
                          <span class="recipe_ranking__recipe_title omit_2line">テスト卵焼き</span>
                        </a>
                      </li>
                    </ul>
                  </body>
                </html>
                """);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("テスト卵焼き");
        assertThat(results.getFirst().getLinkUrl()).isEqualTo("https://recipe.rakuten.co.jp/recipe/1234567890/");
        assertThat(results.getFirst().getImageUrl()).isEqualTo("https://recipe.r10s.jp/sample.jpg");
    }

    @Test
    void parseRecipeMaterialsExtractsMaterialNamesFromRecipeDetailPage() {
        List<String> materials = scraper.parseRecipeMaterials("""
                <html>
                  <body>
                    <section class="recipe_material">
                      <ul class="recipe_material__list">
                        <li class="recipe_material__item">
                          <span class="recipe_material__item_name">豚肉（バラ）</span>
                          <span class="recipe_material__item_serving">100g</span>
                        </li>
                        <li class="recipe_material__item">
                          <span class="recipe_material__item_name">卵</span>
                          <span class="recipe_material__item_serving">2個</span>
                        </li>
                      </ul>
                    </section>
                  </body>
                </html>
                """);

        assertThat(materials).containsExactly("豚肉（バラ）", "卵");
    }

    @Test
    void scrapePopularRecipesByCategoryReadsActualRakutenPageWhenNetworkIsAvailable() {
        List<RecipeSearchResultDto> results;
        try {
            results = scraper.scrapePopularRecipesByCategory("32");
        } catch (RakutenRecipeScraperException exception) {
            assumeTrue(false, "Rakuten Recipe live page is not reachable from this environment.");
            return;
        }

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getTitle()).isNotBlank();
        assertThat(results.getFirst().getLinkUrl()).startsWith("https://recipe.rakuten.co.jp/recipe/");
        assertThat(results.getFirst().getImageUrl()).startsWith("https://");
        assertThat(results.getFirst().getMaterials()).isEmpty();
    }
}
