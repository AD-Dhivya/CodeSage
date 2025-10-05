# CodeSage 🧙‍♂️

> **AI Code Mentor That Teaches Before You Commit**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.java.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=spring)](https://spring.io)
[![Cerebras](https://img.shields.io/badge/AI-Cerebras-green?logo=openai)](https://cerebras.ai)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)](https://docker.com)

## 🎯 The Problem

70% of security vulnerabilities are introduced during development. Traditional code review tools just flag errors - they don't teach developers WHY code is problematic or HOW to fix it properly.
✨ The Solution
CodeSage is an AI-powered code mentor that analyzes code across 5 dimensions and provides educational feedback that helps developers learn, not just fix.
Why CodeSage Wins

🤖 AI-Powered Analysis - Cerebras + Llama 3.1 (sub-2-second response)
🎓 Educational Approach - Teaches WHY and HOW, not just WHAT
🔍 5-Category Analysis - Security, Performance, Code Quality, Architecture, Clean Code
🔌 MCP Server - HTTP endpoint on port 8081 for extensibility
🛡️ Pre-commit Hooks - Prevent bad code from reaching your repo
⚡ Redis Caching - 40-400x faster for repeated analyses
🐳 Production-Ready - Multi-container Docker deployment

🚀 Quick Start
Prerequisites

Docker & Docker Compose
Cerebras API Key (Get one here)

Installation
bash# Clone the repository
git clone https://github.com/AD-Dhivya/CodeSage
cd CodeSage

# Setup environment
cp .env.example .env
# Add your CEREBRAS_API_KEY to .env

# Start with Docker
docker-compose up --build

# Or run locally
./mvnw spring-boot:run
Test It Works
bash# Health check
curl http://localhost:8080/api/health

# Test analysis
curl -X POST http://localhost:8080/api/analyze \
-H "Content-Type: application/json" \
-d '{
"code": "public class Test { String password = \"admin123\"; }",
"language": "java",
"fileName": "Test.java"
}'

🏗️ Architecture
Multi-Container Deployment
┌─────────────────────┐
│   CodeSage App      │  Port 8080 (REST API)
│   (Spring Boot)     │  Port 8081 (MCP Server)
└──────────┬──────────┘
│
┌─────┴─────┐
│           │
┌────▼────┐  ┌──▼─────┐
│  Redis  │  │ Nginx  │
│ (Cache) │  │  (LB)  │
└─────────┘  └────────┘
Services

codesage-app: Main Spring Boot application (ports 8080, 8081)
redis: High-performance caching layer
nginx: Load balancer and reverse proxy

🔧 Features
Core Capabilities
✅ Multi-Language Support

Java, Python, JavaScript, TypeScript, Go, Rust, PHP, Ruby, Kotlin, Swift, Scala, C, C++, C#

✅ Comprehensive Analysis

Security: Hardcoded credentials, SQL injection, XSS, command injection
Performance: N+1 queries, memory leaks, inefficient loops
Code Quality: Long methods, code duplication, poor naming
Architecture: Tight coupling, missing abstractions
Clean Code: Magic numbers, poor variable names

✅ AI-Powered Mentoring

Educational explanations for each issue
Before/after code examples
Learning resources and best practices
Severity levels (CRITICAL, HIGH, MEDIUM, LOW)

✅ Developer Integration

REST API for CI/CD pipelines
MCP server for IDE integration
Git pre-commit hooks
Real-time analysis feedback

📡 API Endpoints
Analysis
POST /api/analyze
Analyze code and get educational feedback.
json{
"code": "your code here",
"language": "java",
"fileName": "Example.java"
}
Response:
json{
"success": true,
"summary": "1 critical issue found",
"detailedAnalysis": "AI analysis with explanations...",
"issues": [
{
"type": "Hardcoded Credentials",
"severity": "CRITICAL",
"location": "Line 5",
"description": "Password hardcoded in source code",
"recommendation": "Use environment variables",
"explanation": "Why this matters...",
"bestPractice": "How to fix it...",
"learningResource": "https://..."
}
],
"responseTimeMs": 1850,
"poweredBy": "Cerebras + Llama 3.1"
}
Health & Monitoring
GET /api/health - Basic health check
GET /api/health/detailed - Detailed system status
GET /api/ping - Simple ping endpoint
🔌 MCP Integration (IDE Support)
CodeSage includes an MCP (Model Context Protocol) server for native IDE integration.
Supported IDEs

Visual Studio Code
Cursor
Windsurf
Any MCP-compatible editor

MCP Endpoints (Port 8081)

GET /mcp/v1/resources - List available resources
POST /mcp/v1/analyze - Analyze code from IDE

Setup for VS Code
Create .vscode/settings.json:
json{
"mcp.servers": {
"codesage": {
"url": "http://localhost:8081",
"description": "CodeSage AI Code Mentor"
}
}
}
See MCP_INTEGRATION.md for detailed setup.
🪝 Git Pre-Commit Hook
Install the pre-commit hook to analyze code before commits:
bash# Copy the hook
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Test it
git add .
git commit -m "Test commit"
# CodeSage will analyze staged files automatically
The hook will:

Analyze all staged files
Show security issues and recommendations
Block commits with CRITICAL issues
Provide educational feedback

🐳 Docker Deployment
Development
bashdocker-compose up --build
Production
bashdocker-compose up --build -d
View Logs
bashdocker-compose logs -f codesage-app
Stop Services
bashdocker-compose down
⚡ Performance
Redis Caching (Verified Working)
First Analysis:

Response time: ~1.2-2s (calls Cerebras API)
Token usage: ~50 tokens
Full AI processing

Cached Analysis:

Response time: 3-50ms (from Redis)
40-400x faster than first request
Zero Cerebras API calls
Identical results

Cache Configuration

TTL: 1 hour
Key: Hash of code content
Automatic invalidation
JSON serialization with type safety

Performance Metrics

First request: 1200-2000ms
Cached request: 3-50ms
Speedup: 40-400x
Cache hit ratio: >90% in typical usage

🎓 Educational Approach
Unlike traditional linters, CodeSage teaches developers:
Example Output
🚨 CRITICAL: Hardcoded Credentials

Location: Line 5: String password = "admin123"

Why This Matters:
Hardcoded credentials are a major security risk. Anyone with access
to your repository can see these secrets, leading to unauthorized
access and potential data breaches.

Best Practice:
String password = System.getenv("DB_PASSWORD");

Learn More:
https://owasp.org/www-community/vulnerabilities/Use_of_hard-coded_credentials
🛠️ Development
Local Development
bash# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Build
./mvnw clean package
Configuration
Edit src/main/resources/application.properties:
properties# Cerebras API
cerebras.api.url=https://api.cerebras.ai/v1/chat/completions
cerebras.api.model=llama3.1-8b
cerebras.api.max-tokens=512
cerebras.api.temperature=0.2

# Redis Cache
spring.cache.type=redis
spring.data.redis.host=redis
spring.data.redis.port=6379

# Server
server.port=8080
🏆 Hackathon Highlights
Innovation

Educational AI approach focused on teaching, not just detection
MCP integration for native IDE support
Proven Redis caching with 40-400x performance improvement
Production-ready multi-container architecture

Technical Excellence

Java 21 + Spring Boot 3.5.6
Cerebras AI integration with custom prompts
Pattern-based static analysis
Docker + Redis + Nginx deployment
Real-time response tracking

Developer Experience

Sub-2-second first analysis
3-50ms cached responses
Educational feedback with examples
Seamless Git workflow integration
REST API + MCP server

📚 Documentation

Setup Guide - Detailed installation instructions
API Documentation - Complete endpoint reference
Architecture Overview - System design
MCP Integration - IDE setup guide

🤝 Contributing

Fork the repository
Create a feature branch (git checkout -b feature/amazing)
Commit your changes (git commit -m 'Add amazing feature')
Push to the branch (git push origin feature/amazing)
Open a Pull Request

📄 License
This project is licensed under the MIT License - see the LICENSE file for details.
👤 Author
AD-Dhivya

GitHub: @AD-Dhivya
Project: CodeSage

🙏 Acknowledgments

Cerebras for AI inference API
Spring Boot team for the excellent framework
FutureStack25 Hackathon organizers


Made with ❤️ for developers who want to write better code
Built for FutureStack25 Hackathon | January 2025