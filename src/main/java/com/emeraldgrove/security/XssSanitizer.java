package com.emeraldgrove.security;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Performs lightweight sanitization of user-controlled text before persistence.
 * The backend stores cleaned plain text, while the frontend renders it as text.
 */
@Component
public class XssSanitizer {

    private static final Pattern SCRIPT_BLOCK_PATTERN =
        Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern DANGEROUS_BLOCK_PATTERN =
        Pattern.compile("(?is)<(iframe|object|embed|style|link|meta|svg|math)[^>]*>.*?</\\1>");
    private static final Pattern DANGEROUS_SELF_CLOSING_PATTERN =
        Pattern.compile("(?is)<(iframe|object|embed|style|link|meta|svg|math)[^>]*?/?>");
    private static final Pattern EVENT_HANDLER_PATTERN =
        Pattern.compile("(?i)on\\w+\\s*=\\s*(['\"]).*?\\1|on\\w+\\s*=\\s*[^\\s>]+");
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN =
        Pattern.compile("(?i)javascript\\s*:");
    private static final Pattern DATA_HTML_PATTERN =
        Pattern.compile("(?i)data\\s*:\\s*text/html");
    private static final Pattern HTML_TAG_PATTERN =
        Pattern.compile("(?is)<[^>]+>");

    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = input.trim();
        sanitized = SCRIPT_BLOCK_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_BLOCK_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = DANGEROUS_SELF_CLOSING_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = DATA_HTML_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        return sanitized.trim();
    }

    public boolean containsXss(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        return SCRIPT_BLOCK_PATTERN.matcher(input).find()
            || DANGEROUS_BLOCK_PATTERN.matcher(input).find()
            || DANGEROUS_SELF_CLOSING_PATTERN.matcher(input).find()
            || EVENT_HANDLER_PATTERN.matcher(input).find()
            || JAVASCRIPT_PROTOCOL_PATTERN.matcher(input).find()
            || DATA_HTML_PATTERN.matcher(input).find()
            || HTML_TAG_PATTERN.matcher(input).find();
    }
}
