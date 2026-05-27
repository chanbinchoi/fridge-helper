package com.nagoya.recipe.scraper;

import com.nagoya.recipe.dto.RecipeSearchResultDto;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RakutenRecipeScraper {

    private static final String BASE_URL = "https://recipe.rakuten.co.jp";
    private static final int TIMEOUT_MILLIS = 10_000;
    private static final int DETAIL_TIMEOUT_MILLIS = 600;
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; FridgeHelperBot/1.0)";

    public List<RecipeSearchResultDto> scrapePopularRecipesByCategory(String categoryId) {
        Document document = fetchCategoryDocument(categoryId);
        return parsePopularRecipes(document);
    }

    List<RecipeSearchResultDto> parsePopularRecipes(String html) {
        return parsePopularRecipes(Jsoup.parse(html, BASE_URL));
    }

    public List<String> scrapeRecipeMaterials(String recipeUrl) {
        Document document = fetchRecipeDocument(recipeUrl);
        return parseRecipeMaterials(document);
    }

    List<String> parseRecipeMaterials(String html) {
        return parseRecipeMaterials(Jsoup.parse(html, BASE_URL));
    }

    private Document fetchCategoryDocument(String categoryId) {
        try {
            return Jsoup.connect(categoryUrl(categoryId))
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .get();
        } catch (IOException exception) {
            throw new RakutenRecipeScraperException("Failed to scrape Rakuten Recipe category: " + categoryId, exception);
        }
    }

    private Document fetchRecipeDocument(String recipeUrl) {
        try {
            return Jsoup.connect(recipeUrl)
                    .userAgent(USER_AGENT)
                    .timeout(DETAIL_TIMEOUT_MILLIS)
                    .get();
        } catch (IOException exception) {
            throw new RakutenRecipeScraperException("Failed to scrape Rakuten Recipe detail: " + recipeUrl, exception);
        }
    }

    private String categoryUrl(String categoryId) {
        if (!StringUtils.hasText(categoryId)) {
            throw new RakutenRecipeScraperException("Rakuten Recipe category id must not be blank.");
        }
        return BASE_URL + "/category/" + categoryId.trim() + "/";
    }

    private List<RecipeSearchResultDto> parsePopularRecipes(Document document) {
        return document.select("li.recipe_ranking__item").stream()
                .map(this::toRecipeResult)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> parseRecipeMaterials(Document document) {
        return document.select(".recipe_material__item_name").stream()
                .map(Element::text)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private RecipeSearchResultDto toRecipeResult(Element item) {
        Element link = item.selectFirst("a.recipe_ranking__link[href]");
        Element title = item.selectFirst(".recipe_ranking__recipe_title");
        Element image = item.selectFirst("img[src]");

        if (link == null || title == null || image == null) {
            return null;
        }

        String recipeTitle = title.text().trim();
        String recipeLink = link.absUrl("href");
        String imageUrl = image.absUrl("src");

        if (!StringUtils.hasText(recipeTitle)
                || !StringUtils.hasText(recipeLink)
                || !StringUtils.hasText(imageUrl)) {
            return null;
        }

        return RecipeSearchResultDto.builder()
                .title(recipeTitle)
                .linkUrl(recipeLink)
                .imageUrl(imageUrl)
                .build();
    }
}
