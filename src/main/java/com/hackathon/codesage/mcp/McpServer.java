
package com.hackathon.codesage.mcp;

import com.hackathon.codesage.service.CerebrasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import spark.Spark;

public class McpServer {

    private final CerebrasService cerebrasService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpServer(CerebrasService cerebrasService) {
        this.cerebrasService = cerebrasService;
    }

    public void start() {
        // Run on 8081, accessible from outside container
        Spark.port(8081);
        Spark.ipAddress("0.0.0.0");

        // Enable CORS (important for IDEs like Cursor)
        Spark.options("/*", (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST");
            response.header("Access-Control-Allow-Headers", "Content-Type");
            return "";
        });

        // Health check endpoint
        Spark.get("/mcp/v1/resources", (request, response) -> {
            response.type("application/json");
            return "[\"codesage-mentor\"]";
        });

        // Main analysis endpoint
        Spark.post("/mcp/v1/analyze", (request, response) -> {
            response.type("application/json");
            try {
                // Parse JSON body
                Map<String, String> body = objectMapper.readValue(request.body(), Map.class);
                String code = body.get("code");
                String filename = body.get("filename");
                String language = "Java"; // You can extract from filename if needed

                // Use your REAL CerebrasService
                var result = cerebrasService.analyzeCode(code, language, filename);

                // Return AI feedback
                return objectMapper.writeValueAsString(Map.of(
                        "feedback", result.getDetailedAnalysis(),
                        "summary", result.getSummary(),
                        "issues", result.getIssues(),
                        "status", result.getStatus()
                ));
            } catch (Exception e) {
                return objectMapper.writeValueAsString(Map.of(
                        "error", "Analysis failed: " + e.getMessage()
                ));
            }
        });

        Spark.awaitInitialization();
        System.out.println("✅ MCP Server is LIVE at http://localhost:8081");
    }

    public void stop() {
        Spark.stop();
        System.out.println("🛑 MCP Server stopped.");
    }
}