
package com.hackathon.codesage.mcp;

import com.hackathon.codesage.service.CerebrasService;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoHTTPD;
import java.util.Map;

import java.util.concurrent.atomic.AtomicBoolean;

public class McpServer extends NanoHTTPD {

    private final CerebrasService cerebrasService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final AtomicBoolean started = new AtomicBoolean(false);

    public McpServer(CerebrasService cerebrasService) {
        super(8081);
        this.cerebrasService = cerebrasService;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        // Handle CORS preflight
        if (method == Method.OPTIONS) {
            Response response = newFixedLengthResponse(Response.Status.OK, "text/plain", "");
            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Methods", "GET, POST");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type");
            return response;
        }

        if (method == Method.GET && "/mcp/v1/resources".equals(uri)) {
            Response response = newFixedLengthResponse(Response.Status.OK, "application/json", "[\"codesage-mentor\"]");
            response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        }

        if (method == Method.POST && "/mcp/v1/analyze".equals(uri)) {
            try {
                long startTime = System.currentTimeMillis();
                // Read POST body
                Map<String, String> files = new java.util.HashMap<>();
                session.parseBody(files);
                String body = files.get("postData");

                // Parse JSON
                Map<String, String> json = objectMapper.readValue(body, Map.class);
                String code = json.get("code");
                String filename = json.get("filename");
                String language = "Java";

                // Call AI
                var result = cerebrasService.analyzeCode(code, language, filename);
                long responseTimeInMs = System.currentTimeMillis() - startTime;

                // Return structured response
                String jsonResponse = objectMapper.writeValueAsString(Map.of(
                        "feedback", result.getDetailedAnalysis(),
                        "status", result.getStatus(),
                        "issues", result.getIssues(),
                        "summary", result.getSummary(),
                        "success", true,
                        "poweredBy", "Cerebras + Llama 3.1",
                        "responseTimeMs", responseTimeInMs



                ));

                Response response = newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
                response.addHeader("Access-Control-Allow-Origin", "*");
                return response;
            } catch (Exception e) {
                try {
                    String errorResponse = objectMapper.writeValueAsString(Map.of("error", "MCP analysis failed: " + e.getMessage()));
                    Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", errorResponse);
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                } catch (Exception jsonEx) {
                    Response response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "MCP analysis failed: " + e.getMessage());
                    response.addHeader("Access-Control-Allow-Origin", "*");
                    return response;
                }
            }
        }

        Response response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        response.addHeader("Access-Control-Allow-Origin", "*");
        return response;
    }


    public void startServer() {
        if (started.get()) {
            System.out.println("⚠️ MCP Server already running.");
            return;
        }

        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            started.set(true);
            System.out.println("✅ MCP Server is LIVE at http://localhost:8081");
        } catch (Exception e) {
            System.err.println("❌ Failed to start MCP Server: " + e.getMessage());
        }
    }

    public void stopServer() {
        stop();
        System.out.println("🛑 MCP Server stopped.");
    }
}
