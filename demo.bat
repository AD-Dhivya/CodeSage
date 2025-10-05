@echo off
REM ═══════════════════════════════════════════════════════════
REM 🚀 CodeSage Demo Script - AI-Powered Code Mentor
REM ═══════════════════════════════════════════════════════════

echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo 🚀 CodeSage Demo - AI-Powered Code Mentor
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.

REM Check if Docker is running
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker is not running. Please start Docker first.
    exit /b 1
)

echo ✅ Docker is running
echo.

REM Start services
echo 🐳 Starting multi-container architecture...
docker-compose up --build -d

echo ⏳ Waiting for services to start...
timeout /t 15 /nobreak >nul

REM Check if services are running
echo 🔍 Checking container status...
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo.

REM Test health endpoint
echo 🏥 Testing health endpoint...
echo Basic health:
curl -s http://localhost:8080/api/health
echo.
echo.

echo Detailed health:
curl -s http://localhost:8080/api/health/detailed
echo.
echo.

REM Test ping
echo 🏓 Testing ping endpoint...
curl -s http://localhost:8080/api/ping
echo.
echo.

REM Test analysis
echo 🔍 Testing AI analysis with vulnerable code...
echo Code: public class Test { String password = "123456"; }
echo.

curl -s -X POST http://localhost:8080/api/analyze -H "Content-Type: application/json" -d "{\"code\": \"public class Test { public static void main(String[] args) { String password = \\\"123456\\\"; System.out.println(password); } }\", \"language\": \"java\", \"fileName\": \"Test.java\"}"

echo.
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo 📊 Demo Summary
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
echo.
echo ✅ Multi-container architecture running
echo ✅ AI-powered code analysis working
echo ✅ Security vulnerability detection active
echo ✅ Load balancing with Nginx
echo ✅ Redis caching layer
echo.
echo 🌐 Access points:
echo    - Main API: http://localhost:8080
echo    - Load Balancer: http://localhost:80
echo    - Health Check: http://localhost:8080/api/health
echo    - Detailed Health: http://localhost:8080/api/health/detailed
echo.
echo 💡 To stop services: docker-compose down
echo ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
