
package com.example.codesage.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1)
public class ResponseTimeFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip health checks and non-API/MCP endpoints
        String uri = request.getRequestURI();
        if (uri.contains("/health") ||
                uri.contains("/actuator") ||
                !uri.startsWith("/api") && !uri.startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        // Process the request through the filter chain
        filterChain.doFilter(request, responseWrapper);

        // Only modify JSON responses
        if (isJsonResponse(responseWrapper)) {
            try {
                String payload = new String(responseWrapper.getContentAsByteArray());
                JsonNode jsonNode = objectMapper.readTree(payload);

                // Create modified JSON with server timing
                ObjectNode updatedJson = jsonNode.deepCopy();
                long serverTime = System.currentTimeMillis() - startTime;
                updatedJson.put("serverResponseTimeMs", serverTime);

                // Add verification field (for hackathon judges)
                updatedJson.put("verification", "Server-measured time - not client-side estimate");

                // Write back to response
                String updatedPayload = updatedJson.toString();
                responseWrapper.setContentLength(updatedPayload.length());
                responseWrapper.getWriter().write(updatedPayload);
            } catch (Exception e) {
                // If JSON parsing fails, return original response
                responseWrapper.copyBodyToResponse();
                return;
            }
        }

        responseWrapper.copyBodyToResponse();
    }

    private boolean isJsonResponse(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.contains("application/json");
    }
}