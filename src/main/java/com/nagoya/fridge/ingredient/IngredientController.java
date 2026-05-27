package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.notion.NotionRawQueryResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fridge/ingredients")
public class IngredientController {

    private final IngredientRawDataService ingredientRawDataService;

    public IngredientController(IngredientRawDataService ingredientRawDataService) {
        this.ingredientRawDataService = ingredientRawDataService;
    }

    @GetMapping("/raw")
    public NotionRawQueryResult getRawIngredients() {
        return ingredientRawDataService.fetchRawIngredients();
    }

    @GetMapping("/names")
    public IngredientNamesResult getIngredientNames() {
        return ingredientRawDataService.fetchIngredientNames();
    }
}
