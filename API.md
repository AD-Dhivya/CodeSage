# CodeSage API Documentation

Complete API reference for CodeSage endpoints.

## Base URLs

- **REST API**: `http://localhost:8080`
- **MCP Server**: `http://localhost:8081`
- **Load Balancer**: `http://localhost:80`

## Authentication

Currently, no authentication is required for local development. For production deployments, consider adding API key authentication.

---

## REST API Endpoints

### POST /api/analyze

Analyze code and receive comprehensive feedback with educational insights.

#### Request

**URL:** `POST http://localhost:8080/api/analyze`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "code": "string (required) - The code to analyze",
  "language": "string (optional) - Programming language",
  "fileName": "string (optional) - File name for language detection"
}
```

**Example Request:**
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { String password = \"admin123\"; }",
    "language": "java",
    "fileName": "Test.java"
  }'
```

#### Response

**Status:** `200 OK`

**Body:**
```json
{
  "success": true,
  "analysis": "🌟 OVERALL ASSESSMENT\n\nDetailed AI-generated analysis with explanations...",
  "summary": "🚨 CRITICAL: 1 critical issues found\n📊 Issues by Category:\n- Security: 1 issues",
  "issues": [
    {
      "type": "Hardcoded Credentials",
      "severity": "CRITICAL",
      "location": "Line 1: String password = \"admin123\"",
      "description": "Password hardcoded in source code",
      "recommendation": "Use environment variables or secure configuration management",
      "category": "Security",
      "explanation": "Hardcoded credentials are a major security risk. Anyone with access to your repository can see these secrets.",
      "example": "String password = \"admin123\"; // ❌ BAD",
      "bestPractice": "String password = System.getenv(\"DB_PASSWORD\"); // ✅ GOOD",
      "learningResource": "https://owasp.org/www-community/vulnerabilities/Use_of_hard-coded_credentials"
    }
  ],
  "responseTimeMs": 1850,
  "status": "SUCCESS",
  "poweredBy": "Cerebras + Llama 3.1"
}
```

#### Cached Response

When Redis cache hits (identical code analyzed recently):

**Response Time:** 3-50ms (vs 1200-2000ms for fresh analysis)

**Body:** Same structure as above, retrieved from Redis cache

**Cache Behavior:**
- Cache key: Hash of code content
- TTL: 1 hour
- Automatic invalidation after expiry

#### Error Responses

**Status:** `400 Bad Request`

```json
{
  "success": false,
  "error": "Code content is required",
  "responseTimeMs": 10
}
```

**Status:** `500 Internal Server Error`

```json
{
  "success": false,
  "error": "Cerebras API error: 401 - Invalid API key",
  "responseTimeMs": 150
}
```

#### Supported Languages

Auto-detected from `fileName` if `language` not provided:

- Java (`.java`)
- Python (`.py`)
- JavaScript (`.js`)
- TypeScript (`.ts`, `.tsx`)
- Go (`.go`)
- Rust (`.rs`)
- PHP (`.php`)
- Ruby (`.rb`)
- Kotlin (`.kt`)
- Swift (`.swift`)
- Scala (`.scala`)
- C (`.c`)
- C++ (`.cpp`, `.cc`)
- C# (`.cs`)

---

### GET /api/health

Basic health check endpoint.

#### Request

```bash
curl http://localhost:8080/api/health
```

#### Response

**Status:** `200 OK`

```json
{
  "api_key": "configured",
  "cerebras": "healthy",
  "status": "UP",
  "timestamp": "2025-01-05T14:30:00"
}
```

**Status:** `500 Internal Server Error` (if unhealthy)

```json
{
  "error": "Cerebras API connection failed",
  "status": "DOWN",
  "timestamp": "2025-01-05T14:30:00"
}
```

---

### GET /api/health/detailed

Comprehensive health check with detailed system information.

#### Request

```bash
curl http://localhost:8080/api/health/detailed
```

#### Response

**Status:** `200 OK`

```json
{
  "service": "CodeSage AI Code Mentor",
  "version": "1.0.0",
  "timestamp": "2025-01-05T14:30:00",
  "javaVersion": "21.0.1",
  "springBootVersion": "3.5.6",
  "cerebrasApi": {
    "status": "healthy",
    "model": "llama3.1-8b",
    "provider": "Cerebras"
  },
  "deployment": {
    "type": "Docker Container",
    "dockerized": true,
    "platform": "Spring Boot Embedded Tomcat"
  },
  "features": [
    "AI-powered code analysis",
    "Educational feedback generation",
    "Multi-language support (15+ languages)",
    "Pre-commit Git hook integration",
    "Sub-2-second response time",
    "Docker containerization",
    "Redis caching (40-400x speedup)"
  ],
  "status": "UP",
  "ready": true
}
```

---

### GET /api/ping

Simple ping endpoint to verify service is running.

#### Request

```bash
curl http://localhost:8080/api/ping
```

#### Response

**Status:** `200 OK`

```
🏓 pong - CodeSage is alive!
```

---

## MCP Server Endpoints

### GET /mcp/v1/resources

List available MCP resources.

#### Request

```bash
curl http://localhost:8081/mcp/v1/resources
```

#### Response

**Status:** `200 OK`

```json
["codesage-mentor"]
```

---

### POST /mcp/v1/analyze

Analyze code via MCP protocol.

#### Request

**URL:** `POST http://localhost:8081/mcp/v1/analyze`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "code": "string (required) - Code to analyze",
  "filename": "string (required) - File name"
}
```

**Example:**
```bash
curl -X POST http://localhost:8081/mcp/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "int x = 5;",
    "filename": "Test.java"
  }'
```

#### Response

**Status:** `200 OK`

```json
{
  "feedback": "🌟 OVERALL ASSESSMENT\n\nDetailed AI analysis...",
  "summary": "✅ No critical issues detected",
  "issues": [],
  "status": "SUCCESS"
}
```

#### Error Response

**Status:** `500 Internal Server Error`

```json
{
  "error": "MCP analysis failed: API key not configured"
}
```

---

## Response Objects

### AnalysisResponse

```typescript
{
  success: boolean;
  analysis: string;           // Detailed AI feedback
  summary: string;            // Quick summary
  issues: CodeIssue[];        // List of detected issues
  responseTimeMs: number;     // Response time in milliseconds
  status: string;             // "SUCCESS" or "ERROR"
  poweredBy: string;          // "Cerebras + Llama 3.1"
}
```

### CodeIssue

```typescript
{
  type: string;              // Issue type (e.g., "Hardcoded Credentials")
  severity: string;          // "CRITICAL", "HIGH", "MEDIUM", "LOW"
  location: string;          // Line number or code location
  description: string;       // What the issue is
  recommendation: string;    // How to fix it
  category: string;          // "Security", "Performance", "CodeQuality", etc.
  explanation: string;       // Why this matters
  example: string;           // Code example showing the problem
  bestPractice: string;      // Recommended approach
  learningResource: string;  // URL to learn more
}
```

---

## Performance Metrics

### Response Times

**Without Cache (First Request):**
- Static analysis: ~200-500ms
- AI processing: ~1000-1500ms
- Total: ~1200-2000ms

**With Cache (Subsequent Identical Requests):**
- Redis retrieval: ~3-50ms
- Speedup: **40-400x faster**

### Cache Statistics

```bash
# Check cache keys
docker exec codesage-redis redis-cli KEYS "*"

# Get cache stats
docker exec codesage-redis redis-cli INFO stats
```

---

## Error Codes

### HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Request successful |
| 400 | Bad Request | Invalid request (missing code, etc.) |
| 500 | Internal Server Error | Server error or AI API failure |

### Error Response Format

```json
{
  "success": false,
  "error": "Error message",
  "responseTimeMs": 100
}
```

---

## Integration Examples

### JavaScript/Node.js

```javascript
const analyzeCode = async (code, language, fileName) => {
  const response = await fetch('http://localhost:8080/api/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, language, fileName })
  });
  
  return await response.json();
};

// Usage
const result = await analyzeCode(
  'public class Test { String password = "123"; }',
  'java',
  'Test.java'
);

console.log(`Summary: ${result.summary}`);
console.log(`Issues: ${result.issues.length}`);
console.log(`Response time: ${result.responseTimeMs}ms`);
```

### Python

```python
import requests

def analyze_code(code, language, file_name):
    response = requests.post(
        'http://localhost:8080/api/analyze',
        json={
            'code': code,
            'language': language,
            'fileName': file_name
        }
    )
    return response.json()

# Usage
result = analyze_code(
    'def test(): password = "123"',
    'python',
    'test.py'
)

print(f"Summary: {result['summary']}")
print(f"Issues: {len(result['issues'])}")
print(f"Response time: {result['responseTimeMs']}ms")
```

### cURL

```bash
# Basic analysis
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"int x = 5;","language":"java","fileName":"Test.java"}'

# With jq for formatted output
curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"int x = 5;","language":"java"}' | jq '.'

# Health check
curl http://localhost:8080/api/health | jq '.'

# Test cache performance
echo "First request (cache miss):"
time curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}' | jq '.responseTimeMs'

echo "Second request (cache hit):"
time curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}' | jq '.responseTimeMs'
```

### Java

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CodeSageClient {
    private static final String API_URL = "http://localhost:8080/api/analyze";
    private final HttpClient client = HttpClient.newHttpClient();
    
    public String analyzeCode(String code, String language) throws Exception {
        String json = String.format(
            "{\"code\":\"%s\",\"language\":\"%s\"}",
            code.replace("\"", "\\\""),
            language
        );
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        
        return response.body();
    }
}
```

---

## Rate Limiting

Currently, no rate limiting is enforced in development. For production:

- Consider rate limiting via Nginx
- Monitor Cerebras API usage
- Implement API key quotas if needed

Example Nginx rate limiting:
```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

location /api/ {
    limit_req zone=api_limit burst=20;
    proxy_pass http://codesage-app:8080;
}
```

---

## Best Practices

### Request Optimization

1. **Keep code snippets reasonable:**
    - Max 500 lines for best results
    - Focus on specific functions or classes
    - Entire files may exceed token limits

2. **Specify language when known:**
    - Improves analysis accuracy
    - Faster processing
    - Better context for AI

3. **Use descriptive file names:**
    - Helps with language detection
    - Provides context to AI
    - Example: `UserController.java` vs `file1.txt`

### Response Handling

1. **Check `success` field first:**
   ```javascript
   if (response.success) {
     // Process analysis
   } else {
     // Handle error
   }
   ```

2. **Parse issues by severity:**
   ```javascript
   const critical = response.issues.filter(i => i.severity === 'CRITICAL');
   const high = response.issues.filter(i => i.severity === 'HIGH');
   ```

3. **Display educational content:**
    - Show explanations to help learning
    - Include best practice examples
    - Link to learning resources

### Error Handling

```javascript
try {
  const response = await fetch('/api/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code, language, fileName })
  });
  
  const data = await response.json();
  
  if (!response.ok || !data.success) {
    console.error('Analysis failed:', data.error);
    return;
  }
  
  // Process successful response
  console.log(`Found ${data.issues.length} issues`);
  console.log(`Response time: ${data.responseTimeMs}ms`);
  
} catch (error) {
  console.error('Network error:', error);
}
```

---

## Troubleshooting

### Common Issues

**Problem:** `CEREBRAS_API_KEY not found`

**Solution:** Add API key to `.env` file:
```bash
echo "CEREBRAS_API_KEY=your_key_here" >> .env
docker-compose restart
```

**Problem:** `Connection refused to localhost:8080`

**Solution:** Ensure Docker container is running:
```bash
docker-compose ps
docker-compose up -d
```

**Problem:** Slow response times

**Solution:** Check Cerebras API status and Redis cache:
```bash
curl http://localhost:8080/api/health
docker exec codesage-redis redis-cli ping
```

**Problem:** Cache not working

**Solution:** Verify Redis is running and connected:
```bash
docker logs codesage-redis
docker exec codesage-redis redis-cli KEYS "*"
```

---

## Support

- GitHub Issues: [CodeSage Issues](https://github.com/AD-Dhivya/CodeSage/issues)
- Documentation: [README.md](README.md)
- Setup Guide: [SETUP.md](SETUP.md)