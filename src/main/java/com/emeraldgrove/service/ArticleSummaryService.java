package com.emeraldgrove.service;

public interface ArticleSummaryService {
    String summarizeDescription(String title, String sourceDescription, String currentStoredDescription);
}
