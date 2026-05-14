package com.emeraldgrove.constants;

public final class AiConstants {
    private AiConstants() {
        // Prevent instantiation
    }
    
    // Content processing
    public static final int MAX_CONTENT_LENGTH = 8_000;
    
    // Worker scheduling
    public static final int SCHEDULED_DELAY_MS = 3000;
    public static final int SCHEDULED_INITIAL_DELAY_MS = 5000;
    public static final int MAX_RETRIES = 3;
    
    // JSON validation
    public static final String MARKDOWN_CODE_BLOCK_START = "```";
    public static final String MARKDOWN_CODE_BLOCK_PATTERN = "(?s)^```[a-zA-Z]*\\s*";
    public static final String MARKDOWN_CODE_BLOCK_END_PATTERN = "(?s)```\\s*$";
}
