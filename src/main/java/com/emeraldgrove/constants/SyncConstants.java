package com.emeraldgrove.constants;

public final class SyncConstants {
    private SyncConstants() {
        // Prevent instantiation
    }
    
    // Sync status constants
    public static final String STATUS_APPLIED = "APPLIED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    
    // Sync error codes
    public static final String ERROR_CODE_ARTICLE_NOT_FOUND = "ARTICLE_NOT_FOUND";
    public static final String ERROR_CODE_COLLECTION_NOT_FOUND = "COLLECTION_NOT_FOUND";
    public static final String ERROR_CODE_LINK_NOT_FOUND = "LINK_NOT_FOUND";
}
