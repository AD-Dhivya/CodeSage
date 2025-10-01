
package com.hackathon.codesage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// NO SpringBootTest needed - pure unit test
public class CerebrasServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private CerebrasService cerebrasService;

    private String testCode;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testCode = """
                public class TestCode {
                    public static void main(String[] args) {
                        String password = "12345";
                    }
                }
                """;

        // Set test configuration directly
        ReflectionTestUtils.setField(cerebrasService, "apiKey", "test_key");
        ReflectionTestUtils.setField(cerebrasService, "apiUrl", "https://api.cerebras.ai/v1/chat/completions");
        ReflectionTestUtils.setField(cerebrasService, "model", "llama3.1-8b");

        // Inject mocked HttpClient
        ReflectionTestUtils.setField(cerebrasService, "client", httpClient);
    }

    @Test
    void testAnalyzeCode_Success() throws Exception {
        // Mock valid API response
        String mockResponse = """
                {
                  "choices": [{
                    "message": {
                      "content": "Security warning: Hardcoded password detected. Use environment variables instead."
                    }
                  }]
                }
                """;

        // CRITICAL FIX: Proper generic type handling
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockResponse);
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        String result = cerebrasService.analyzeCode(testCode, "java", "TestFile.java");

        assertTrue(result.contains("Security warning"), "Should detect security issue");
        assertTrue(result.contains("Hardcoded password"), "Should mention hardcoded password");
    }

    @Test
    void testAnalyzeCode_ApiError() throws Exception {
        // Mock API error
        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn("Rate limit exceeded");

        // Same critical fix applied here
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        String result = cerebrasService.analyzeCode(testCode, "java", "TestFile.java");

        assertTrue(result.startsWith("❌ API Error: 429"), "Should show API error status");
        assertTrue(result.contains("Rate limit exceeded"), "Should include error details");
    }

    @Test
    void testAnalyzeCode_ParsingError() throws Exception {
        // Mock invalid JSON
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{invalid json}");

        // Same critical fix applied here
        when(httpClient.send(
                any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()
        )).thenReturn(httpResponse);

        String result = cerebrasService.analyzeCode(testCode, "java", "TestFile.java");

        assertTrue(result.startsWith("❌ Response parsing failed"),
                "Should handle JSON parsing errors");
    }
}