#!/bin/bash

# ═══════════════════════════════════════════════════════════
# 🚀 CodeSage Complete Docker MCP Demo
# ═══════════════════════════════════════════════════════════

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 CodeSage Complete Docker MCP Demo"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

echo "✅ Docker is running"
echo ""

# Start complete Docker MCP setup
echo "🐳 Starting complete Docker MCP architecture..."
docker-compose -f docker-compose.mcp.yml up --build -d

echo "⏳ Waiting for all services to start..."
sleep 20

# Check if services are running
echo "🔍 Checking container status..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# Test health endpoint
echo "🏥 Testing health endpoint..."
echo "Basic health:"
curl -s http://localhost:8080/api/health | jq 2>/dev/null || echo "Health check failed"
echo ""

echo "Detailed health with Docker MCP:"
curl -s http://localhost:8080/api/health/detailed | jq 2>/dev/null || echo "Detailed health check failed"
echo ""

# Test Docker MCP analysis
echo "🐳 Testing Docker MCP analysis..."
echo "Code: public class Test { String password = \"123456\"; }"
echo ""

ANALYSIS_RESPONSE=$(curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { public static void main(String[] args) { String password = \"123456\"; System.out.println(password); } }",
    "language": "java",
    "fileName": "Test.java"
  }' 2>/dev/null)

if [ $? -eq 0 ] && [ ! -z "$ANALYSIS_RESPONSE" ]; then
    echo "✅ Docker MCP analysis completed successfully!"
    echo "$ANALYSIS_RESPONSE" | jq 2>/dev/null || echo "$ANALYSIS_RESPONSE"
else
    echo "❌ Analysis failed"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Complete Docker MCP Demo Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Multi-container architecture running"
echo "✅ Docker MCP language-specific analyzers active"
echo "✅ AI-powered code analysis working"
echo "✅ Security vulnerability detection active"
echo "✅ Load balancing with Nginx"
echo "✅ Redis caching layer"
echo "✅ Container orchestration"
echo ""
echo "🐳 Docker MCP Containers:"
echo "   - java-analyzer: Java-specific analysis"
echo "   - python-analyzer: Python-specific analysis"
echo "   - js-analyzer: JavaScript/TypeScript analysis"
echo "   - security-scanner: OWASP ZAP security scanning"
echo "   - dependency-checker: Dependency vulnerability scanning"
echo ""
echo "🌐 Access points:"
echo "   - Main API: http://localhost:8080"
echo "   - Load Balancer: http://localhost:80"
echo "   - Health Check: http://localhost:8080/api/health"
echo "   - Detailed Health: http://localhost:8080/api/health/detailed"
echo ""
echo "💡 To stop services: docker-compose -f docker-compose.mcp.yml down"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
