package com.emeraldgrove.exception;

public class DuplicateArticleException extends RuntimeException {
    public DuplicateArticleException(Long articleId) {
        super("Article already exists with id=" + articleId);
    }
}