package com.emeraldgrove.constants;

public final class ErrorMessages {
    private ErrorMessages() {
        // Prevent instantiation
    }
    
    // Error messages (Russian)
    public static final String ERROR_ARTICLE_NOT_FOUND = "Статья не найдена: ";
    public static final String ERROR_COLLECTION_NOT_FOUND = "Коллекция не найдена: ";
    public static final String ERROR_NOTE_NOT_FOUND = "Заметка не найдена: ";
    
    // Error messages (English)
    public static final String ERROR_RACE_CONDITION = "Race condition: article not found after conflict";
    
    // AI-related error messages
    public static final String ERROR_UNKNOWN_JOB_TYPE = "Unknown job type: ";
    public static final String ERROR_GROQ_ANALYSIS_FAILED = "Groq analysis failed: ";
    public static final String ERROR_AI_RESULT_SERIALIZATION_FAILED = "Failed to serialize AI result";
    
    // Auth-related error messages
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "Email is already registered.";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password.";
    public static final String ERROR_INVALID_REFRESH_TOKEN = "Invalid refresh token.";
    public static final String ERROR_REFRESH_TOKEN_REVOKED = "Refresh token has been revoked.";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired.";
    public static final String ERROR_USER_NOT_FOUND = "User not found.";
    
    // Groq-related error messages
    public static final String ERROR_GROQ_API_STATUS = "Groq API returned status %d with body: %s";
}
