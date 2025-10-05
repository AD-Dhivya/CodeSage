package com.hackathon.codesage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class DockerMCPService {
    
    private static final Logger log = LoggerFactory.getLogger(DockerMCPService.class);
    
    /**
     * Run analysis in language-specific Docker container
     */
    public Map<String, Object> runContainerAnalysis(String code, String language, String fileName) {
        Map<String, Object> results = new HashMap<>();
        
        try {
            String containerName = getContainerName(language);
            String analysisScript = getAnalysisScript(language);
            
            log.info("🐳 Running Docker MCP analysis in container: {}", containerName);
            
            // Execute analysis in Docker container
            String output = executeInContainer(containerName, analysisScript, code);
            
            results.put("containerAnalysis", output);
            results.put("containerName", containerName);
            results.put("language", language);
            results.put("timestamp", java.time.LocalDateTime.now());
            results.put("dockerMCP", true);
            
            log.info("✅ Docker MCP analysis completed for {}", language);
            
        } catch (Exception e) {
            log.error("❌ Docker MCP analysis failed: {}", e.getMessage());
            results.put("error", e.getMessage());
            results.put("dockerMCP", false);
        }
        
        return results;
    }
    
    /**
     * Execute command in Docker container
     */
    private String executeInContainer(String containerName, String script, String code) throws Exception {
        // Create temporary file with code
        String tempFile = "/tmp/code_" + System.currentTimeMillis();
        
        // Write code to temporary file in container
        String writeCommand = String.format(
            "docker exec %s sh -c 'echo \"%s\" > %s'",
            containerName, code.replace("\"", "\\\""), tempFile
        );
        
        Process writeProcess = Runtime.getRuntime().exec(writeCommand);
        writeProcess.waitFor();
        
        // Run analysis script
        String analysisCommand = String.format(
            "docker exec %s sh -c '%s %s'",
            containerName, script, tempFile
        );
        
        Process analysisProcess = Runtime.getRuntime().exec(analysisCommand);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(analysisProcess.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        analysisProcess.waitFor();
        
        // Clean up temporary file
        String cleanupCommand = String.format("docker exec %s rm -f %s", containerName, tempFile);
        Runtime.getRuntime().exec(cleanupCommand);
        
        return output.toString();
    }
    
    /**
     * Get container name for language
     */
    private String getContainerName(String language) {
        switch (language.toLowerCase()) {
            case "java": return "java-analyzer";
            case "python": return "python-analyzer";
            case "javascript":
            case "typescript": return "js-analyzer";
            default: return "java-analyzer";
        }
    }
    
    /**
     * Get analysis script for language
     */
    private String getAnalysisScript(String language) {
        switch (language.toLowerCase()) {
            case "java": return "/tools/java-analyzer.sh";
            case "python": return "/tools/python-analyzer.sh";
            case "javascript":
            case "typescript": return "/tools/js-analyzer.sh";
            default: return "/tools/java-analyzer.sh";
        }
    }
    
    /**
     * Check if Docker containers are running
     */
    public Map<String, Object> checkContainerHealth() {
        Map<String, Object> health = new HashMap<>();
        
        String[] containers = {"java-analyzer", "python-analyzer", "js-analyzer", "security-scanner", "dependency-checker"};
        
        for (String container : containers) {
            try {
                Process process = Runtime.getRuntime().exec("docker exec " + container + " echo 'OK'");
                int exitCode = process.waitFor();
                health.put(container, exitCode == 0 ? "running" : "stopped");
            } catch (Exception e) {
                health.put(container, "error");
            }
        }
        
        return health;
    }
}
