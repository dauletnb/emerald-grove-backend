package com.emeraldgrove.constants;

public final class LogMessages {
    private LogMessages() {
        // Prevent instantiation
    }
    
    public static final String LOG_SKIPPING_COLLECTION_LINK_SYNC = 
        "Skipping collection link sync for user {}: article={}, collection={}";
    
    // AI-related log messages
    public static final String LOG_AI_RESULT_SAVED = "AI result saved for article {}, tokensUsed={}";
    public static final String LOG_AI_VALIDATION_FAILED = "AI response validation failed, using empty result: {}";
    public static final String LOG_AI_JOB_FAILED = "AI job {} failed (attempt {}): {}";
    
    // Groq-related log messages
    public static final String LOG_GROQ_SUMMARY_SKIPPED_BLANK =
        "Groq summary skipped: source description is blank";
    public static final String LOG_GROQ_SUMMARY_SKIPPED_UNCHANGED =
        "Groq summary skipped: source description is unchanged";
    public static final String LOG_GROQ_SUMMARY_SKIPPED_DISABLED =
        "Groq summary skipped: emerald-grove.ai.groq.enabled=false";
    public static final String LOG_GROQ_SUMMARY_SKIPPED_NO_KEY =
        "Groq summary skipped: GROQ_API_KEY is empty or unavailable in the backend process";
    public static final String LOG_GROQ_SUMMARY_REQUEST_STARTED =
        "Groq summary request started: model={}, sourceLength={}";
    public static final String LOG_GROQ_SUMMARY_REQUEST_SUCCEEDED =
        "Groq summary request succeeded: summaryLength={}";
    public static final String LOG_GROQ_SUMMARY_RESPONSE_EMPTY =
        "Groq summary response was empty, using local fallback";
    public static final String LOG_GROQ_SUMMARY_GENERATION_FAILED =
        "Groq summary generation failed, using local fallback: {}";
    public static final String LOG_GROQ_ANALYSIS_REQUEST_STARTED =
        "Groq full analysis request started: model={}, contentLength={}";
    public static final String LOG_GROQ_ANALYSIS_REQUEST_SUCCEEDED =
        "Groq full analysis request succeeded: responseLength={}, tokens={}";
}
