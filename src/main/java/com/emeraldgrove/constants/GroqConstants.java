package com.emeraldgrove.constants;

public final class GroqConstants {
    private GroqConstants() {
        // Prevent instantiation
    }
    
    // HTTP
    public static final String HTTP_HEADER_AUTHORIZATION_PREFIX = "Bearer ";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final int CONNECT_TIMEOUT_SECONDS = 5;
    
    // API request parameters
    public static final double TEMPERATURE_SUMMARY = 0.3;
    public static final double TEMPERATURE_ANALYSIS = 0.2;
    public static final int MAX_COMPLETION_TOKENS_SUMMARY = 160;
    public static final int MAX_COMPLETION_TOKENS_ANALYSIS = 2048;
    public static final double TOP_P = 0.95;
    public static final boolean STREAM = false;
    
    // JSON keys
    public static final String JSON_KEY_MODEL = "model";
    public static final String JSON_KEY_TEMPERATURE = "temperature";
    public static final String JSON_KEY_MAX_COMPLETION_TOKENS = "max_completion_tokens";
    public static final String JSON_KEY_TOP_P = "top_p";
    public static final String JSON_KEY_STREAM = "stream";
    public static final String JSON_KEY_REASONING_EFFORT = "reasoning_effort";
    public static final String JSON_KEY_REASONING_FORMAT = "reasoning_format";
    public static final String JSON_KEY_MESSAGES = "messages";
    public static final String JSON_KEY_ROLE = "role";
    public static final String JSON_KEY_CONTENT = "content";
    public static final String JSON_KEY_CHOICES = "choices";
    public static final String JSON_KEY_MESSAGE = "message";
    public static final String JSON_KEY_USAGE = "usage";
    public static final String JSON_KEY_TOTAL_TOKENS = "total_tokens";
    public static final String JSON_VALUE_REASONING_EFFORT_NONE = "none";
    public static final String JSON_VALUE_REASONING_FORMAT_HIDDEN = "hidden";
    public static final String JSON_VALUE_ROLE_USER = "user";
    
    // Prompts
    public static final String PROMPT_SUMMARY = """
        Summarize this article in 1-2 short sentences.
        Keep the summary under %d characters.
        Use the same language as the source text.
        Return plain text only.

        Title: %s
        Source text:
        %s
        """;
    
    public static final String PROMPT_ANALYSIS = """
        You are an API that returns structured JSON only.

        Rules:
        - No explanations
        - No markdown
        - Only valid JSON

        Schema:
        {
          "summary": {"short": "string (1-2 sentences)", "detailed": "string (3-5 sentences)"},
          "keyPoints": ["string", "string", "string"],
          "highlights": [{"text": "string (exact quote)", "explanation": "string (why it matters)"}]
        }

        Return 3-5 keyPoints and 2-3 highlights maximum.
        Use the same language as the source text.

        Article title: %s
        Article content:
        %s
        """;
}
