
package com.hackathon.codesage.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeIssue {
    private String type;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String location;
    private String description;
    private String recommendation;
    private String category; // Security, Performance, CodeQuality, Architecture, CleanCode
    private String explanation; // Why this is an issue
    private String example; // Code example showing the problem
    private String bestPractice; // How to write it correctly
    private String learningResource; // Link to learn more
}