
package com.hackathon.codesage.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "🟢 CodeSage is alive and ready!";
    }

    @GetMapping("/")
    public String root() {
        return "👋 Welcome to CodeSage! Go to /api/health to check status.";
    }
}