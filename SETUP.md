# CodeSage Setup Guide

Complete installation and configuration guide for CodeSage.

## Prerequisites

### Required

- **Docker Desktop** (or Docker Engine + Docker Compose)
    - Version 20.10+ recommended
    - [Download Docker Desktop](https://www.docker.com/products/docker-desktop)

- **Cerebras API Key**
    - Sign up at [cerebras.ai](https://cerebras.ai)
    - Navigate to API section
    - Generate a new API key

### Optional (for local development without Docker)

- **Java 21** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Git** (for pre-commit hooks)

---

## Installation

### Method 1: Docker (Recommended)

Fastest way to get started - no Java or Maven installation required.

#### Step 1: Clone Repository

```bash
git clone https://github.com/AD-Dhivya/CodeSage
cd CodeSage
```

#### Step 2: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env file and add your API key
# On Linux/Mac:
nano .env

# On Windows:
notepad .env
```

Add your Cerebras API key:
```bash
CEREBRAS_API_KEY=your_actual_api_key_here
```

#### Step 3: Start Services

```bash
# Build and start all containers
docker-compose up --build

# Or run in background (detached mode)
docker-compose up --build -d
```

This starts three containers:
- `codesage-app` (Spring Boot app on ports 8080, 8081)
- `codesage-redis` (Redis cache on port 6379)
- `codesage-nginx` (Nginx load balancer on port 80)

#### Step 4: Verify Installation

```bash
# Check containers are running
docker ps

# Test health endpoint
curl http://localhost:8080/api/health

# Test ping endpoint
curl http://localhost:8080/api/ping

# Test MCP server
curl http://localhost:8081/mcp/v1/resources
```

Expected output:
```bash
# docker ps
CONTAINER ID   IMAGE           STATUS         PORTS
abc123         codesage-app    Up 2 minutes   0.0.0.0:8080->8080, 8081->8081
def456         redis:7-alpine  Up 2 minutes   0.0.0.0:6379->6379
ghi789         nginx:alpine    Up 2 minutes   0.0.0.0:80->80

# health check
{"status":"UP","cerebras":"healthy",...}

# ping
🏓 pong - CodeSage is alive!

# MCP resources
["codesage-mentor"]
```

---

### Method 2: Local Development (Without Docker)

For developers who want to run CodeSage directly on their machine.

#### Step 1: Install Java 21

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
java -version
```

**macOS:**
```bash
brew install openjdk@21
java -version
```

**Windows:**
- Download from [Adoptium](https://adoptium.net/)
- Install and verify: `java -version`

#### Step 2: Clone and Configure

```bash
git clone https://github.com/AD-Dhivya/CodeSage
cd CodeSage

# Create .env file
cp .env.example .env

# Add your API key to .env
echo "CEREBRAS_API_KEY=your_key_here" >> .env
```

#### Step 3: Build and Run

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run
```

**Windows:**
```bash
mvnw.cmd clean package
mvnw.cmd spring-boot:run
```

#### Step 4: Verify

```bash
# Check if running
curl http://localhost:8080/api/health

# Test analysis
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"int x = 5;","language":"java"}'
```

---

## Configuration

### Environment Variables

The `.env` file contains only the API key:

```bash
# Required
CEREBRAS_API_KEY=your_api_key_here
```

All other configuration is in `application.properties` (see next section).

### Application Properties

Advanced configuration in `src/main/resources/application.properties`:

```properties
# Cerebras API
cerebras.api.url=https://api.cerebras.ai/v1/chat/completions
cerebras.api.model=llama3.1-8b
cerebras.api.max-tokens=512
cerebras.api.temperature=0.2

# Server
server.port=8080

# Redis Cache
spring.cache.type=redis
spring.data.redis.host=redis
spring.data.redis.port=6379

# Prompt limits
prompt.max.chars=6000
prompt.max.code.chars=3000
prompt.max.context.chars=800
prompt.max.examples.chars=1200

# HTTP timeout
http.request.timeout.ms=15000

# Logging
logging.level.com.hackathon.codesage=INFO
```

---

## Git Pre-Commit Hook Setup

Enable automatic code analysis before commits.

### Step 1: Install Hook

```bash
# Copy pre-commit hook
cp scripts/pre-commit .git/hooks/pre-commit

# Make executable
chmod +x .git/hooks/pre-commit
```

### Step 2: Test Hook

```bash
# Create a test file with a security issue
echo 'public class Test { String password = "123"; }' > Test.java

# Try to commit
git add Test.java
git commit -m "Test commit"

# CodeSage will analyze and block if critical issues found
```

### Hook Configuration

Edit `.git/hooks/pre-commit` to customize:

```bash
# API endpoint
API_URL="http://localhost:8080/api/analyze"

# Block on severity levels
BLOCK_ON_CRITICAL=true
BLOCK_ON_HIGH=false
```

---

## MCP Server

CodeSage includes an MCP-compatible HTTP server on port 8081.

### Testing MCP Endpoints

```bash
# List available resources
curl http://localhost:8081/mcp/v1/resources

# Analyze code via MCP endpoint
curl -X POST http://localhost:8081/mcp/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String password = \"admin123\";","filename":"Test.java"}'
```

### Future Development
IDE plugin integration is planned for future releases. The HTTP server provides the foundation for MCP-compatible tools and extensions.

---

## Troubleshooting

### Docker Issues

**Problem:** `Cannot connect to Docker daemon`

**Solution:**
```bash
# Linux: Start Docker service
sudo systemctl start docker

# macOS/Windows: Open Docker Desktop application
```

**Problem:** Port already in use (8080, 8081, 80, 6379)

**Solution:**
```bash
# Find what's using the port
# Linux/Mac:
lsof -i :8080

# Windows:
netstat -ano | findstr :8080

# Stop the conflicting service or change ports in docker-compose.yml
```

### API Key Issues

**Problem:** `CEREBRAS_API_KEY not found`

**Solution:**
```bash
# Verify .env file exists and contains key
cat .env

# Restart Docker containers
docker-compose down
docker-compose up -d

# Check environment variable is loaded
docker exec codesage-app printenv | grep CEREBRAS
```

### Build Failures

**Problem:** Maven build fails

**Solution:**
```bash
# Clean Maven cache
./mvnw clean

# Try again with verbose output
./mvnw clean package -X

# Check Java version
java -version  # Should be 21

# Verify pom.xml exists
ls -l pom.xml
```

### Connection Issues

**Problem:** Can't connect to http://localhost:8080

**Solution:**
```bash
# Check container is running
docker ps

# Check logs for errors
docker logs codesage-app

# Check port mapping
docker port codesage-app

# Try accessing via container IP
docker inspect codesage-app | grep IPAddress
curl http://<container_ip>:8080/api/health
```

### Redis Cache Issues

**Problem:** Cache not working or serialization errors

**Solution:**
```bash
# Check Redis is running
docker ps | grep redis
docker logs codesage-redis

# Test Redis connection
docker exec codesage-redis redis-cli ping
# Should return: PONG

# Check cache keys
docker exec codesage-redis redis-cli KEYS "*"

# Clear cache if needed
docker exec codesage-redis redis-cli FLUSHALL
```

### Performance Issues

**Problem:** Slow analysis responses

**Solution:**
1. Check Cerebras API status
2. Verify network connection
3. Check Docker resource limits
4. Monitor Redis connection

```bash
# Check Docker resource usage
docker stats

# Check logs for timeouts
docker logs codesage-app | grep -i timeout

# Test Cerebras API directly
curl http://localhost:8080/api/health
```

---

## Testing Redis Cache

Verify caching is working properly:

```bash
# First request (should be slow ~1-2s)
time curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}'

# Second identical request (should be fast <50ms)
time curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}'

# Check Redis contains cached data
docker exec codesage-redis redis-cli KEYS "*"
```

Expected: First request takes 1-2s, second request takes <50ms (40-400x faster).

---

## Production Deployment

### Security Considerations

1. **Use HTTPS:**
   ```nginx
   server {
       listen 443 ssl;
       ssl_certificate /path/to/cert.pem;
       ssl_certificate_key /path/to/key.pem;
       
       location / {
           proxy_pass http://codesage-app:8080;
       }
   }
   ```

2. **Secure API Keys:**
    - Never commit `.env` to Git
    - Use secrets management (AWS Secrets Manager, HashiCorp Vault)
    - Rotate keys regularly

3. **Add Authentication:**
    - Implement API key validation
    - Use OAuth2 for enterprise deployments
    - Rate limit per user/key

### Scaling

**Horizontal Scaling:**
```bash
# Scale to 3 instances
docker-compose up --scale codesage-app=3

# Nginx will load balance automatically
```

**Resource Limits:**
```yaml
# docker-compose.yml
services:
  codesage-app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

---

## Uninstallation

### Docker Method

```bash
# Stop and remove containers
docker-compose down

# Remove images
docker rmi codesage-codesage-app redis:7-alpine nginx:alpine

# Remove volumes (optional - deletes cache data)
docker-compose down -v
```

### Local Method

```bash
# Stop application (Ctrl+C if running)

# Remove project directory
cd ..
rm -rf CodeSage
```

---

## Next Steps

After installation:

1. **Test the API**: See [API.md](API.md) for endpoint documentation
2. **Setup IDE Integration**: Configure MCP for your IDE
3. **Install Git Hooks**: Enable pre-commit analysis
4. **Read Architecture**: Understand the system design
5. **Start Analyzing**: Use `/api/analyze` endpoint

---

## Quick Command Reference

```bash
# Start
docker-compose up -d

# Stop
docker-compose down

# Logs
docker logs -f codesage-app

# Rebuild
docker-compose up --build

# Health check
curl http://localhost:8080/api/health

# Test analysis
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"int x;","language":"java"}'

# Check cache
docker exec codesage-redis redis-cli KEYS "*"
```

---

## Support

Need help?

- Check [Troubleshooting](#troubleshooting) section above
- Review logs: `docker logs codesage-app`
- Health check: `curl http://localhost:8080/api/health`
- GitHub Issues: [Report an issue](https://github.com/AD-Dhivya/CodeSage/issues)