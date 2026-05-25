package com.nagoya.fridge.ingredient;

import com.nagoya.fridge.notion.NotionClientException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class IngredientExceptionHandler {

    @ExceptionHandler(NotionClientException.class)
    ProblemDetail handleNotionClientException(NotionClientException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                exception.getMessage()
        );
        problemDetail.setTitle("Notion integration failed");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
