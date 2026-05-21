package com.emeraldgrove.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AiResultDto {
    public Summary summary;
    public List<String> keyPoints;
    public List<Highlight> highlights;

    public static class Summary {
        @JsonProperty("short")
        public String shortText;
        public String detailed;
    }

    public static class Highlight {
        public String text;
        public String explanation;
    }
}
