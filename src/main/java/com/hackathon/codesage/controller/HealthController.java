package com.hackathon.codesage.controller;


import com.hackathon.codesage.service.CerebrasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final CerebrasService cerebrasService;

    @Autowired
    public HealthController(CerebrasService cerebrasService) {
        this.cerebrasService = cerebrasService;
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
}
