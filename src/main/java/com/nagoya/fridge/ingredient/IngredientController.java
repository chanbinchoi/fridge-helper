package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.notion.NotionRawQueryResult;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<NotionRawQueryResult> getRawIngredients() {
        return noStore(ingredientRawDataService.fetchRawIngredients());
    }

    @GetMapping("/names")
    public ResponseEntity<IngredientNamesResult> getIngredientNames() {
        return noStore(ingredientRawDataService.fetchIngredientNames());
    }

    @GetMapping("/items")
    public ResponseEntity<IngredientItemsResult> getIngredientItems() {
        return noStore(ingredientRawDataService.fetchIngredientItems());
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
