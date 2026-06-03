package com.nagoya.recipe.rakuten;

public class RakutenRecipeApiException extends RuntimeException {

    public RakutenRecipeApiException(String message) {
        super(message);
    }

    public RakutenRecipeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
