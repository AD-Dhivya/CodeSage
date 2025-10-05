# CodeSage - AI-Powered Code Mentor 🛡️

> **AI-powered security mentor for pre-commit reviews** - Stop security vulnerabilities before they commit!

[![Docker](https://img.shields.io/badge/Docker-MCP-blue?logo=docker)](https://docker.com)
[![Cerebras](https://img.shields.io/badge/AI-Cerebras-green?logo=openai)](https://cerebras.ai)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?logo=spring)](https://spring.io)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.java.net)

## 🎯 The Problem We Solve

**70% of security vulnerabilities are introduced during development.** Developers often lack security expertise, code reviews miss security issues, and vulnerable code reaches production. CodeSage prevents this by analyzing code before it's committed.

## 🚀 The Solution

CodeSage combines **AI-powered analysis** with **Docker MCP** for comprehensive code mentoring:

- 🤖 **AI Analysis**: Cerebras AI (Llama 3.1) for intelligent code insights
- 🐳 **Docker MCP**: Language-specific containers for static analysis
- 🔍 **Comprehensive Analysis**: Security, Performance, Code Quality, Architecture, Clean Code
- 📚 **Developer Education**: Learn best practices while coding
- 🛡️ **Pre-commit Integration**: Automatic analysis before commits
- 🎓 **Mentoring Approach**: Teaches developers how to solve problems, not just find them

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Cerebras API Key

### 1. Clone and Setup
```bash
git clone <your-repo>
cd CodeSage
cp .env.example .env
# Add your CEREBRAS_API_KEY to .env
```

### 2. Run with Docker (Multi-Container Architecture)
```bash
# Start all services
docker-compose up --build

# Or run in background
docker-compose up --build -d
```

### 3. Test the API
```bash
# Test health
curl http://localhost:8080/api/health

# Test analysis
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { String password = \"123456\"; }",
    "language": "java",
    "fileName": "Test.java"
  }'
```

### 4. Run Demo Script
```bash
# Run comprehensive demo
./demo.sh
```

## 🏗️ Multi-Container Architecture

### Services
- **codesage-app**: Main Spring Boot application (Port 8080)
- **redis**: Caching layer for improved performance (Port 6379)
- **nginx**: Load balancer and reverse proxy (Port 80)

### Network
- **codesage-network**: Bridge network connecting all containers
- **Load balancing**: Nginx distributes requests to the main app
- **Caching**: Redis provides session and cache management

## 🔧 Features

### Core Features
- ✅ **AI-powered security analysis** (Cerebras + Llama 3.1)
- ✅ **Multi-language support** (Java, Python, JavaScript, TypeScript, Go, Rust, PHP, Ruby)
- ✅ **Pre-commit hook integration** (Prevents vulnerable code commits)
- ✅ **Docker containerization** (Multi-container architecture)
- ✅ **RESTful API** (Easy integration)
- ✅ **Real-time health monitoring**

### Security Features
- 🛡️ **Pre-commit hook** prevents vulnerable code commits
- 🤖 **AI-powered vulnerability detection** using Cerebras AI
- 🔍 **Pattern-based security analysis** for common vulnerabilities
- ⚙️ **Configurable severity thresholds** (Critical, High, Medium, Low)
- 📊 **Detailed security reports** with actionable recommendations

## 📊 API Endpoints

### Analysis
- `POST /api/analyze` - Analyze code for security vulnerabilities
  ```json
  {
    "code": "public class Test { String password = \"123456\"; }",
    "language": "java",
    "fileName": "Test.java"
  }
  ```

### Health & Monitoring
- `GET /api/health` - Basic health check
- `GET /api/health/detailed` - Detailed system health with container status
- `GET /api/ping` - Simple ping endpoint

### Load Balancer (Nginx)
- `GET /` - Root endpoint (redirects to ping)
- `GET /health` - Health check through load balancer
- `GET /api/*` - All API endpoints through load balancer

## 🛡️ Security Analysis

### Supported Vulnerabilities
- 🔐 **Hardcoded credentials** detection
- 💉 **SQL injection** prevention
- 🚨 **XSS vulnerabilities** detection
- ⚡ **Command injection** risks
- 🔒 **Input validation** issues
- 📝 **Code quality** improvements

### Analysis Pipeline
1. **Static Analysis**: Pattern-based vulnerability detection
2. **AI Analysis**: Cerebras AI (Llama 3.1) for intelligent insights
3. **Context Analysis**: Language-specific security patterns
4. **Results Combination**: Merged analysis with actionable recommendations

## 🔧 Development

### Local Development
```bash
# Start only the main app (without load balancer)
./mvnw spring-boot:run

# Or with Docker
docker-compose up codesage-app
```

### Testing
```bash
# Run tests
./mvnw test

# Run demo
./demo.sh
```

### Pre-commit Hook Setup
```bash
# Install pre-commit hook
cp scripts/pre-commit-hook.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Test the hook
git add .
git commit -m "Test commit"
```

## 📈 Performance

### Caching
- **Redis**: Session and analysis result caching
- **Load Balancing**: Nginx distributes load across instances
- **Container Optimization**: Multi-stage Docker builds

### Scalability
- **Horizontal scaling**: Add more app instances
- **Load balancing**: Nginx handles traffic distribution
- **Caching layer**: Redis reduces API calls

## 🔍 Monitoring

### Health Checks
```bash
# Basic health
curl http://localhost:8080/api/health

# Detailed health (shows container status)
curl http://localhost:8080/api/health/detailed

# Through load balancer
curl http://localhost/health
```

### Container Status
```bash
# View running containers
docker ps

# View logs
docker-compose logs codesage-app
docker-compose logs redis
docker-compose logs nginx
```

## 🚀 Deployment

### Production Deployment
```bash
# Production build
docker-compose -f docker-compose.prod.yml up --build

# Scale services
docker-compose up --scale codesage-app=3
```

### Environment Variables
```bash
# Required
CEREBRAS_API_KEY=your_api_key_here

# Optional
CEREBRAS_API_URL=https://api.cerebras.ai/v1/chat/completions
CEREBRAS_MODEL=llama3.1-8b
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test with `./demo.sh`
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🏆 Hackathon Project

**CodeSage** - AI-Powered Code Mentor
- **Tech Stack**: Java 21, Spring Boot, Docker, Cerebras AI, Redis, Nginx
- **Features**: Multi-container architecture, AI analysis, Pre-commit integration
- **Innovation**: First AI-powered pre-commit security mentor
- **Award**: Best Security Tool (Previous Hackathon)

## 🎨 Creative Features

### **AI + Docker MCP Innovation**
- **Unique Approach**: Combines AI analysis with containerized static analysis
- **Language-Specific**: Each language gets its own analysis container
- **Real-Time Learning**: AI learns from container analysis results

### **Pre-Commit Integration**
- **Git Hook**: Automatically analyzes code before commit
- **Developer Education**: Teaches security while preventing issues
- **Workflow Integration**: Seamless developer experience

### **Multi-Model Analysis**
- **AI Analysis**: Cerebras AI for intelligent insights
- **Static Analysis**: Containerized language-specific tools
- **Pattern Analysis**: Custom vulnerability detection
- **Combined Results**: Best of all approaches

---

**Made with ❤️ for secure coding** 🛡️
