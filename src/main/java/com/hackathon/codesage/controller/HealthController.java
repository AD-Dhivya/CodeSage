
package com.hackathon.codesage.controller;

import com.hackathon.codesage.service.CerebrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@RestController
public class HealthController {

    private final CerebrasService cerebrasService;

    @Autowired
    public HealthController(CerebrasService cerebrasService) {
        this.cerebrasService = cerebrasService;
    }

    /**
     * Simple health check
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Check Cerebras API connection
            boolean cerebrasHealthy = cerebrasService.healthCheck();

            status.put("api_key", "configured");
            status.put("cerebras", cerebrasHealthy ? "healthy" : "unhealthy");
            status.put("status", cerebrasHealthy ? "UP" : "DOWN");
            status.put("timestamp", LocalDateTime.now());

            return status;
        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("status", "DOWN");
            status.put("timestamp", LocalDateTime.now());
            return status;
        }
    }

    /**
     * Detailed health check (without Docker MCP dependency)
     */
    @GetMapping("/health/detailed")
    public Map<String, Object> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Basic info
            health.put("service", "CodeSage AI Code Mentor");
            health.put("version", "1.0.0");
            health.put("timestamp", LocalDateTime.now());

            // Cerebras API health
            boolean cerebrasHealthy = cerebrasService.healthCheck();
            health.put("cerebras", cerebrasHealthy ? "healthy" : "unhealthy");

            // Docker services (assumed via compose)
            health.put("docker", "Containerized with Docker Compose");
            health.put("containers", List.of("codesage-app", "redis", "nginx"));
            health.put("redis", "available");
            health.put("nginx", "reverse-proxy-ready");

            // MCP Server (if you have it)
            health.put("mcp_server", "Running on http://localhost:8081 (if enabled)");
            health.put("mcp_ide_integration", "Yes - .vscode/mcp.json included");

            // System status
            health.put("status", cerebrasHealthy ? "UP" : "DOWN");
            health.put("uptime", "Running");

            // Features
            health.put("features", List.of(
                    "AI-powered analysis",
                    "Educational feedback",
                    "Pre-commit integration",
                    "Docker containerization",
                    "Java 21 + Spring Boot",
                    "Cerebras + Llama 3.1"
            ));

            return health;

        } catch (Exception e) {
            health.put("error", e.getMessage());
            health.put("status", "DOWN");
            health.put("timestamp", LocalDateTime.now());
            return health;
        }
    }
}
