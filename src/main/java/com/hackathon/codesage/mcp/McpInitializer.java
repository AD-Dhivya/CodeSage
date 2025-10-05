
package com.hackathon.codesage.mcp;

import com.hackathon.codesage.service.CerebrasService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class McpInitializer {

    private final CerebrasService cerebrasService;
    private McpServer mcpServer;

    public McpInitializer(CerebrasService cerebrasService) {
        this.cerebrasService = cerebrasService;
    }

    @PostConstruct
    public void init() {
        System.out.println("🚀 Starting MCP Server for AI-native IDE support...");
        mcpServer = new McpServer(cerebrasService);
        new Thread(mcpServer::start).start();
    }

    @PreDestroy
    public void destroy() {
        System.out.println("🛑 Shutting down MCP Server...");
        if (mcpServer != null) {
            mcpServer.stop();
        }
    }
}
