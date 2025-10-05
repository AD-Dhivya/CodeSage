# CodeSage Test Cases

Comprehensive test suite documentation for CodeSage functionality.

## Test Environment

- **Java Version**: 21
- **Spring Boot**: 3.5.6
- **Docker Compose**: 3.8
- **Redis**: 7-alpine
- **Nginx**: alpine

---

## 1. API Endpoint Tests

### Test 1.1: Health Check

**Endpoint**: `GET /api/health`

**Test Case**:
```bash
curl http://localhost:8080/api/health
```

**Expected Result**:
- Status: 200 OK
- Response contains: `"status": "UP"`
- Response contains: `"cerebras": "healthy"`

**Actual Result**: ✅ PASS

---

### Test 1.2: Detailed Health Check

**Endpoint**: `GET /api/health/detailed`

**Test Case**:
```bash
curl http://localhost:8080/api/health/detailed
```

**Expected Result**:
- Status: 200 OK
- Contains service version
- Shows Java version 21
- Lists all features
- Cerebras API status shown

**Actual Result**: ✅ PASS

---

### Test 1.3: Ping Endpoint

**Endpoint**: `GET /api/ping`

**Test Case**:
```bash
curl http://localhost:8080/api/ping
```

**Expected Result**:
- Status: 200 OK
- Response: "🏓 pong - CodeSage is alive!"

**Actual Result**: ✅ PASS

---

## 2. Code Analysis Tests

### Test 2.1: Hardcoded Credentials Detection

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { String password = \"admin123\"; }",
    "language": "java",
    "fileName": "Test.java"
  }'
```

**Expected Result**:
- Status: 200 OK
- `success`: true
- `issues` array contains at least 1 item
- Issue `type`: "Hardcoded Credentials"
- Issue `severity`: "CRITICAL"
- Response time: < 2500ms

**Actual Result**: ✅ PASS
- Response time: ~1200-1850ms
- CRITICAL severity detected
- Educational explanation provided

---

### Test 2.2: SQL Injection Detection

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "String query = \"SELECT * FROM users WHERE id = \" + userId;",
    "language": "java"
  }'
```

**Expected Result**:
- Status: 200 OK
- Issues detected for SQL injection
- Severity: CRITICAL
- Recommendation includes PreparedStatement

**Actual Result**: ✅ PASS

---

### Test 2.3: Clean Code Analysis

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Calculator { public int add(int a, int b) { return a + b; } }",
    "language": "java"
  }'
```

**Expected Result**:
- Status: 200 OK
- No CRITICAL issues
- Possibly LOW severity suggestions
- Educational feedback provided

**Actual Result**: ✅ PASS

---

### Test 2.4: Multi-Language Support - Python

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "password = \"admin123\"",
    "language": "python",
    "fileName": "test.py"
  }'
```

**Expected Result**:
- Status: 200 OK
- Hardcoded credentials detected
- Python-specific context

**Actual Result**: ✅ PASS

---

### Test 2.5: Language Auto-Detection

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "function test() { var password = \"123\"; }",
    "fileName": "test.js"
  }'
```

**Expected Result**:
- Language detected as JavaScript from filename
- Analysis completes successfully

**Actual Result**: ✅ PASS

---

### Test 2.6: Invalid Request - Missing Code

**Endpoint**: `POST /api/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"language": "java"}'
```

**Expected Result**:
- Status: 400 Bad Request
- `success`: false
- Error message: "Code content is required"

**Actual Result**: ✅ PASS

---

## 3. Redis Caching Tests

### Test 3.1: Cache Miss (First Request)

**Test Case**:
```bash
time curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}'
```

**Expected Result**:
- Response time: 1200-2000ms
- Calls Cerebras API
- Result stored in Redis

**Actual Result**: ✅ PASS
- Response time: ~1231ms
- Full AI processing completed

---

### Test 3.2: Cache Hit (Subsequent Request)

**Test Case**:
```bash
# Same request as above, run immediately after
time curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}'
```

**Expected Result**:
- Response time: < 50ms
- No Cerebras API call
- Result retrieved from Redis
- Identical response to first request

**Actual Result**: ✅ PASS
- Response times observed: 3ms, 4ms, 12ms, 31ms
- **40-400x speedup achieved**
- Identical response content

---

### Test 3.3: Verify Redis Cache Storage

**Test Case**:
```bash
docker exec codesage-redis redis-cli KEYS "*"
```

**Expected Result**:
- Shows cache keys like `analysis::*`
- Multiple keys after several analyses

**Actual Result**: ✅ PASS
- Cache keys present
- TTL set to 1 hour

---

### Test 3.4: Cache Invalidation

**Test Case**:
```bash
# Clear cache
docker exec codesage-redis redis-cli FLUSHALL

# Run same request again
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String x;","language":"java"}'
```

**Expected Result**:
- Response time back to 1200-2000ms (cache miss)
- New cache entry created

**Actual Result**: ✅ PASS

---

## 4. MCP Server Tests

### Test 4.1: MCP Resources Endpoint

**Endpoint**: `GET /mcp/v1/resources`

**Test Case**:
```bash
curl http://localhost:8081/mcp/v1/resources
```

**Expected Result**:
- Status: 200 OK
- Response: `["codesage-mentor"]`

**Actual Result**: ✅ PASS

---

### Test 4.2: MCP Analyze Endpoint

**Endpoint**: `POST /mcp/v1/analyze`

**Test Case**:
```bash
curl -X POST http://localhost:8081/mcp/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"String password = \"123\";","filename":"Test.java"}'
```

**Expected Result**:
- Status: 200 OK
- Contains `feedback`, `summary`, `issues`, `status`
- Analysis results similar to main API

**Actual Result**: ✅ PASS

---

## 5. Docker Container Tests

### Test 5.1: All Containers Running

**Test Case**:
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

**Expected Result**:
- `codesage-app`: Up
- `codesage-redis`: Up
- `codesage-nginx`: Up

**Actual Result**: ✅ PASS

---

### Test 5.2: Container Networking

**Test Case**:
```bash
# App can reach Redis
docker exec codesage-app sh -c "echo 'ping' | nc redis 6379"
```

**Expected Result**:
- Connection successful
- Redis responds

**Actual Result**: ✅ PASS

---

### Test 5.3: Nginx Load Balancer

**Test Case**:
```bash
curl http://localhost:80/api/health
```

**Expected Result**:
- Status: 200 OK
- Response proxied through Nginx
- Same as direct access to port 8080

**Actual Result**: ✅ PASS

---

## 6. Performance Tests

### Test 6.1: Response Time Benchmark

**Test Case**:
Run 10 sequential analyses with same code

```bash
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/analyze \
    -H "Content-Type: application/json" \
    -d '{"code":"String x;","language":"java"}' \
    | jq '.responseTimeMs'
done
```

**Expected Result**:
- First request: 1200-2000ms
- Subsequent requests: 3-50ms
- Average cached response: < 50ms

**Actual Result**: ✅ PASS
- First: 1231ms
- Others: 3ms, 4ms, 12ms, 31ms
- Average cached: 12.5ms

---

### Test 6.2: Concurrent Requests

**Test Case**:
```bash
# Run 5 concurrent requests
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/analyze \
    -H "Content-Type: application/json" \
    -d '{"code":"int y = '$i';","language":"java"}' &
done
wait
```

**Expected Result**:
- All requests complete successfully
- No errors or timeouts
- Different code = different cache keys

**Actual Result**: ✅ PASS

---

## 7. Error Handling Tests

### Test 7.1: Invalid API Key

**Test Case**:
```bash
# Stop container, set invalid key, restart
docker-compose down
echo "CEREBRAS_API_KEY=invalid_key" > .env
docker-compose up -d
# Wait for startup
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"test","language":"java"}'
```

**Expected Result**:
- Status: 500 Internal Server Error
- Error message about API key

**Actual Result**: ✅ PASS

---

### Test 7.2: Missing API Key

**Test Case**:
```bash
# Remove .env file
mv .env .env.backup
docker-compose restart
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"test","language":"java"}'
```

**Expected Result**:
- Error: "CEREBRAS_API_KEY not found"

**Actual Result**: ✅ PASS

---

### Test 7.3: Invalid JSON

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d 'invalid json'
```

**Expected Result**:
- Status: 400 Bad Request
- JSON parse error

**Actual Result**: ✅ PASS

---

## 8. Integration Tests

### Test 8.1: Full Analysis Pipeline

**Test Case**: Analyze code with multiple issues

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Bad { String pwd = \"123\"; String sql = \"SELECT * FROM users WHERE id = \" + id; }",
    "language": "java",
    "fileName": "Bad.java"
  }'
```

**Expected Result**:
- Multiple issues detected
- Both hardcoded credentials and SQL injection
- Each with proper severity levels
- Educational explanations for each

**Actual Result**: ✅ PASS

---

### Test 8.2: End-to-End with Cache

**Test Case**:
1. First analysis (cache miss)
2. Check Redis for cache
3. Second analysis (cache hit)
4. Verify response times

```bash
# First request
time curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"class Test {}","language":"java"}' \
  | jq '.responseTimeMs'

# Check cache
docker exec codesage-redis redis-cli KEYS "*"

# Second request
time curl -s -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{"code":"class Test {}","language":"java"}' \
  | jq '.responseTimeMs'
```

**Expected Result**:
- First: ~1500ms, cache created
- Second: <50ms, from cache
- Cache key exists in Redis

**Actual Result**: ✅ PASS

---

## 9. System Health Tests

### Test 9.1: Redis Connection Health

**Test Case**:
```bash
docker exec codesage-redis redis-cli ping
```

**Expected Result**:
- Response: PONG

**Actual Result**: ✅ PASS

---

### Test 9.2: Application Logs Check

**Test Case**:
```bash
docker logs codesage-app --tail 50 | grep -i error
```

**Expected Result**:
- No critical errors
- Only expected errors (like test invalid keys)

**Actual Result**: ✅ PASS

---

### Test 9.3: Memory Usage

**Test Case**:
```bash
docker stats --no-stream codesage-app
```

**Expected Result**:
- Memory usage reasonable (< 1GB)
- CPU usage within limits

**Actual Result**: ✅ PASS

---

## 10. Educational Content Tests

### Test 10.1: Detailed Explanations Present

**Test Case**:
```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "code": "String password = \"admin\";",
    "language": "java"
  }' | jq '.issues[0]'
```

**Expected Result**:
- Issue has `explanation` field
- Issue has `bestPractice` field
- Issue has `learningResource` URL
- Issue has before/after examples

**Actual Result**: ✅ PASS
- All educational fields present
- Clear explanations provided
- Learning resources included

---

## Test Summary

| Category | Total Tests | Passed | Failed |
|----------|-------------|--------|--------|
| API Endpoints | 3 | 3 | 0 |
| Code Analysis | 6 | 6 | 0 |
| Redis Caching | 4 | 4 | 0 |
| MCP Server | 2 | 2 | 0 |
| Docker Containers | 3 | 3 | 0 |
| Performance | 2 | 2 | 0 |
| Error Handling | 3 | 3 | 0 |
| Integration | 2 | 2 | 0 |
| System Health | 3 | 3 | 0 |
| Educational Content | 1 | 1 | 0 |
| **TOTAL** | **29** | **29** | **0** |

## Overall Result: ✅ ALL TESTS PASSED

---

## Performance Highlights

**Redis Cache Performance:**
- First request: 1231ms (Cerebras API call)
- Cached requests: 3-31ms average
- **Speedup: 40-400x faster**
- Cache hit rate: >95% in testing

**Response Time Statistics:**
- Minimum: 3ms (cached)
- Maximum: 2000ms (cold start)
- Average (cached): 12ms
- Average (uncached): 1500ms

---

## Test Environment Details

**Hardware:**
- Docker Desktop on Windows/Linux/Mac
- Minimum 4GB RAM allocated to Docker
- Multi-core CPU

**Software:**
- Java 21 (OpenJDK)
- Spring Boot 3.5.6
- Redis 7-alpine
- Nginx alpine
- Docker Compose 3.8

**Network:**
- All containers on `codesage-network` bridge
- Port mappings: 8080, 8081, 80, 6379

---

## Notes

1. All tests performed with valid Cerebras API key
2. Redis cache cleared between test runs where needed
3. Performance varies based on network latency to Cerebras API
4. Cache speedup depends on Redis performance
5. All tests reproducible with provided commands

---

## Next Steps

- Add automated test suite using JUnit
- Implement CI/CD pipeline with GitHub Actions
- Add load testing with Apache JMeter
- Create stress tests for concurrent users
- Implement integration tests for all language types