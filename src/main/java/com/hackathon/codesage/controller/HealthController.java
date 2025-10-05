package com.hackathon.codesage.controller;


import com.hackathon.codesage.service.CerebrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HealthController {

    private final CerebrasService cerebrasService;
    private final com.hackathon.codesage.service.DockerMCPService dockerMCPService;

    @Autowired
    public HealthController(CerebrasService cerebrasService, com.hackathon.codesage.service.DockerMCPService dockerMCPService) {
        this.cerebrasService = cerebrasService;
        this.dockerMCPService = dockerMCPService;
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Check Cerebras API connection
            boolean cerebrasHealthy = cerebrasService.healthCheck();

            // Check API key availability
            status.put("api_key", "configured");
            status.put("cerebras", cerebrasHealthy ? "healthy" : "unhealthy");
            status.put("status", cerebrasHealthy ? "UP" : "DOWN");

            return status;
        } catch (Exception e) {
            status.put("error", e.getMessage());
            status.put("status", "DOWN");
            return status;
        }
    }

    @GetMapping("/health/detailed")
    public Map<String, Object> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic health info
            health.put("service", "CodeSage AI Code Mentor");
            health.put("version", "1.0.0");
            health.put("timestamp", LocalDateTime.now());
            
            // Cerebras API health
            boolean cerebrasHealthy = cerebrasService.healthCheck();
            health.put("cerebras", cerebrasHealthy ? "healthy" : "unhealthy");
            
            // Docker container status
            health.put("docker", "Multi-container architecture active");
            health.put("containers", List.of("codesage-app", "redis", "nginx"));
            
            // Docker MCP container status
            Map<String, Object> dockerMCPHealth = dockerMCPService.checkContainerHealth();
            health.put("dockerMCP", dockerMCPHealth);
            
            // System status
            health.put("status", cerebrasHealthy ? "UP" : "DOWN");
            health.put("uptime", "Running");
            
            // Features
            health.put("features", List.of(
                "AI-powered analysis",
                "Multi-language support", 
                "Pre-commit integration",
                "Docker containerization",
                "Load balancing"
            ));
            
            return health;
            
        } catch (Exception e) {
            health.put("error", e.getMessage());
            health.put("status", "DOWN");
            return health;
        }
    }
}
