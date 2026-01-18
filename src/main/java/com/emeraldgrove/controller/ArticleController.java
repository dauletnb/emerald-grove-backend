package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*") // Разрешает запросы от расширения
public class ArticleController {

    @PostMapping
    public ResponseEntity<?> saveArticle(@RequestBody ArticleRequest request) {
        System.out.println("Сохраняем статью: " + request.title());
        System.out.println("URL: " + request.url());

        // Тут будет логика сохранения в БД (Repository)

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Article saved successfully!"
        ));
    }
}