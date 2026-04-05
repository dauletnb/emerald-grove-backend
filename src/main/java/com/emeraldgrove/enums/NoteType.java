package com.emeraldgrove.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Types of notes with string serialization support
 * for compatibility with TypeScript Union Type
 */
@Getter
public enum NoteType {

    /**
     * Idea - creative thought or concept
     */
    IDEA("IDEA", "Idea"),

    /**
     * Task - action that needs to be completed
     */
    TASK("TASK", "Task"),

    /**
     * Question - request for information or clarification
     */
    QUESTION("QUESTION", "Question");

    private final String code;
    private final String displayName;

    NoteType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * Returns string representation for JSON serialization
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * Creates enum from string representation
     */
    public static NoteType fromCode(String code) {
        for (NoteType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown NoteType code: " + code);
    }

    /**
     * Validates code
     */
    public static boolean isValidCode(String code) {
        try {
            fromCode(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}