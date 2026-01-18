package com.emeraldgrove.dto;

public record ArticleRequest(
        String title,
        String url,
        String description
) {}