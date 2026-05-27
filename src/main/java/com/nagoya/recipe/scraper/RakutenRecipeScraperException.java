package com.nagoya.recipe.scraper;

public class RakutenRecipeScraperException extends RuntimeException {

    public RakutenRecipeScraperException(String message) {
        super(message);
    }

    public RakutenRecipeScraperException(String message, Throwable cause) {
        super(message, cause);
    }
}
