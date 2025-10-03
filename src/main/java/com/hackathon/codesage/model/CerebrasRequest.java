
package com.hackathon.codesage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ✅ ADDED IMPORT
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CerebrasRequest {
    private String model;
    private List<Message> messages;
    private double temperature;
    private int max_tokens;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }
}