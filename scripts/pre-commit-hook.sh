
#!/usr/bin/env bash

# ═══════════════════════════════════════════════════════════
# 🛡️ CodeSage: AI-Powered Security Mentor
# Pre-Commit Hook - Stops security issues before they commit
# ═══════════════════════════════════════════════════════════

# Get project root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ Not in a Git repository."
  exit 1
fi

cd "$PROJECT_ROOT" || exit 1

# Check for bypass file (for development)
BYPASS_FILE="$PROJECT_ROOT/.codesage-bypass"
if [ -f "$BYPASS_FILE" ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "⚠️  CodeSage bypass active (.codesage-bypass detected)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "💡 To disable bypass: rm .codesage-bypass"
  echo "✅ Proceeding with commit..."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🛡️  CodeSage: AI-Powered Security Mentor"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if backend is running - MANDATORY
BACKEND_URL="http://localhost:8080"
echo "🔍 Checking backend at $BACKEND_URL/api/ping..."

PING_RESPONSE=$(curl -s --fail --connect-timeout 3 --max-time 5 $BACKEND_URL/api/ping 2>/dev/null)
PING_EXIT=$?

if [ $PING_EXIT -ne 0 ] || [ -z "$PING_RESPONSE" ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "❌ ERROR: CodeSage backend is NOT running!"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "🚀 Please start the backend first:"
  echo ""
  echo "   cd E:/Hackathon/CodeSage"
  echo "   ./mvnw spring-boot:run"
  echo ""
  echo "Or in PowerShell:"
  echo ""
  echo "   cd E:\\Hackathon\\CodeSage"
  echo "   .\\mvnw.cmd spring-boot:run"
  echo ""
  echo "💡 Pro tip: Use 'touch .codesage-bypass' to skip this check during development"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "🛑 COMMIT BLOCKED: Backend must be running for security analysis"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 1
fi

echo "✅ Backend is running: $PING_RESPONSE"
echo ""

# Check health endpoint
echo "🔍 Checking backend health..."
HEALTH_RESPONSE=$(curl -s --fail --max-time 5 $BACKEND_URL/api/health 2>/dev/null)

if [ $? -eq 0 ]; then
  echo "✅ Backend health check passed"
  echo "📡 Response: $HEALTH_RESPONSE"
else
  echo "⚠️  Warning: Health check failed but ping succeeded"
  echo "⚠️  Continuing with analysis..."
fi

echo ""

# Get staged code files
echo "🔍 Scanning for staged code files..."
STAGED_FILES=$(git diff --cached --name-only --diff-filter=d | grep -E '\.(java|js|jsx|ts|tsx|py|go|rs|php|rb)$')

# Check if any files found
if [ -z "$STAGED_FILES" ]; then
  echo "ℹ️  No code files staged for commit"
  echo "✅ Commit allowed (no code to analyze)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0
fi

echo "📁 Found code files to analyze:"
echo "$STAGED_FILES" | sed 's/^/   → /'
echo ""

# Ensure jq is available
if ! command -v jq >/dev/null 2>&1; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "❌ ERROR: 'jq' is required but not installed"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "📦 Install jq:"
  echo ""
  echo "   Windows (with Chocolatey): choco install jq"
  echo "   Windows (with Scoop): scoop install jq"
  echo "   Windows (manual): https://stedolan.github.io/jq/download/"
  echo ""
  echo "   Mac: brew install jq"
  echo "   Linux: sudo apt-get install jq"
  echo ""
  echo "💡 Bypass tip: Create .codesage-bypass file to skip analysis"
  echo ""
  echo "🛑 COMMIT BLOCKED: Install jq to enable security analysis"
  exit 1
fi

HAS_CRITICAL_ISSUES=0
HAS_HIGH_ISSUES=0
TOTAL_FILES=0
TOTAL_ISSUES=0

# Analyze each file
while IFS= read -r file; do
  if [ ! -f "$file" ]; then
    echo "⚠️  File not found: $file (possibly deleted)"
    continue
  fi

  TOTAL_FILES=$((TOTAL_FILES + 1))

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "🔍 Analyzing: $file"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # Detect language
  case "${file##*.}" in
    "java") LANGUAGE="java" ;;
    "js"|"jsx") LANGUAGE="javascript" ;;
    "ts"|"tsx") LANGUAGE="typescript" ;;
    "py") LANGUAGE="python" ;;
    "go") LANGUAGE="go" ;;
    "rs") LANGUAGE="rust" ;;
    "php") LANGUAGE="php" ;;
    "rb") LANGUAGE="ruby" ;;
    *) LANGUAGE="javascript" ;;
  esac

  # Read file content (limit to 10KB)
  CONTENT=$(cat "$file" | head -c 10240)
  CONTENT_SIZE=${#CONTENT}

  if [ $CONTENT_SIZE -ge 10240 ]; then
    CONTENT="$CONTENT\n... [file truncated for analysis]"
    echo "⚠️  File is large (${CONTENT_SIZE} bytes), analyzing first 10KB"
  fi

  FILENAME=$(basename "$file")

  echo "📤 Sending to CodeSage AI..."
  echo "   File: $FILENAME"
  echo "   Language: $LANGUAGE"
  echo "   Size: $CONTENT_SIZE bytes"
  echo ""

  # Send to backend with timeout
  ANALYSIS_RESPONSE=$(curl -s -X POST $BACKEND_URL/api/analyze \
    -H "Content-Type: application/json" \
    --connect-timeout 5 \
    --max-time 60 \
    -d "{
      \"code\": $(printf '%s' "$CONTENT" | jq -sR .),
      \"language\": \"$LANGUAGE\",
      \"fileName\": \"$FILENAME\"
    }" 2>/dev/null)

  CURL_EXIT=$?

  # Check if request failed
  if [ $CURL_EXIT -ne 0 ]; then
    echo "❌ Failed to connect to backend (curl exit code: $CURL_EXIT)"
    echo ""
    echo "💡 To bypass: touch .codesage-bypass"
    echo "🛑 COMMIT BLOCKED: Cannot analyze code without backend"
    exit 1
  fi

  if [ -z "$ANALYSIS_RESPONSE" ]; then
    echo "❌ Empty response from backend"
    echo ""
    echo "💡 To bypass: touch .codesage-bypass"
    echo "🛑 COMMIT BLOCKED: Backend error"
    exit 1
  fi

  # Check for success
  SUCCESS=$(echo "$ANALYSIS_RESPONSE" | jq -r '.success // false')

  if [ "$SUCCESS" != "true" ]; then
    ERROR_MSG=$(echo "$ANALYSIS_RESPONSE" | jq -r '.error // "Unknown error"')
    echo "❌ Analysis failed: $ERROR_MSG"
    echo ""
    echo "💡 To bypass: touch .codesage-bypass"
    echo "🛑 COMMIT BLOCKED: Analysis error"
    exit 1
  fi

  # Extract data
  SUMMARY=$(echo "$ANALYSIS_RESPONSE" | jq -r '.summary // "No summary"')
  ANALYSIS=$(echo "$ANALYSIS_RESPONSE" | jq -r '.analysis // "No analysis"')
  ISSUES=$(echo "$ANALYSIS_RESPONSE" | jq -r '.issues // []')
  RESPONSE_TIME=$(echo "$ANALYSIS_RESPONSE" | jq -r '.responseTimeMs // 0')
  STATUS=$(echo "$ANALYSIS_RESPONSE" | jq -r '.status // "UNKNOWN"')

  echo "⏱️  Analysis completed in ${RESPONSE_TIME}ms"
  echo ""
  echo "📊 Summary:"
  echo "   $SUMMARY"
  echo ""

  # Count issues
  ISSUE_COUNT=$(echo "$ISSUES" | jq 'length')
  TOTAL_ISSUES=$((TOTAL_ISSUES + ISSUE_COUNT))

  if [ "$ISSUE_COUNT" -gt 0 ]; then
    echo "🔍 Found $ISSUE_COUNT issue(s):"
    echo ""

    # Count by severity
    CRITICAL_COUNT=$(echo "$ISSUES" | jq '[.[] | select(.severity == "CRITICAL")] | length')
    HIGH_COUNT=$(echo "$ISSUES" | jq '[.[] | select(.severity == "HIGH")] | length')
    MEDIUM_COUNT=$(echo "$ISSUES" | jq '[.[] | select(.severity == "MEDIUM")] | length')
    LOW_COUNT=$(echo "$ISSUES" | jq '[.[] | select(.severity == "LOW")] | length')

    if [ "$CRITICAL_COUNT" -gt 0 ]; then
      HAS_CRITICAL_ISSUES=1
      echo "   🚨 CRITICAL: $CRITICAL_COUNT"
    fi

    if [ "$HIGH_COUNT" -gt 0 ]; then
      HAS_HIGH_ISSUES=1
      echo "   ⚠️  HIGH: $HIGH_COUNT"
    fi

    if [ "$MEDIUM_COUNT" -gt 0 ]; then
      echo "   ℹ️  MEDIUM: $MEDIUM_COUNT"
    fi

    if [ "$LOW_COUNT" -gt 0 ]; then
      echo "   💡 LOW: $LOW_COUNT"
    fi

    echo ""
    echo "📋 Issue Details:"
    echo ""

    # Show each issue
    echo "$ISSUES" | jq -r '.[] | "
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🔴 \(.type) [\(.severity)]
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📍 Location: \(.location)

📝 Description:
   \(.description)

✅ Recommendation:
   \(.recommendation)
"'

  else
    echo "✅ No security issues detected in this file!"
  fi

  echo ""

done <<< "$STAGED_FILES"

# Final summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Analysis Complete"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "   Files analyzed: $TOTAL_FILES"
echo "   Total issues: $TOTAL_ISSUES"
echo ""

# Decision logic
if [ $HAS_CRITICAL_ISSUES -eq 1 ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "🛑 COMMIT BLOCKED"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "❌ CRITICAL security issues detected!"
  echo ""
  echo "💡 Please fix the critical issues before committing."
  echo "   CodeSage has provided detailed recommendations above."
  echo ""
  echo "🔒 Security is not optional - it protects users and data."
  echo ""
  echo "💡 To bypass (for development): touch .codesage-bypass"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 1

elif [ $HAS_HIGH_ISSUES -eq 1 ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "⚠️  COMMIT ALLOWED WITH WARNINGS"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "⚠️  HIGH severity security issues found"
  echo ""
  echo "💡 Strongly consider fixing these before pushing to production."
  echo "   Review the recommendations carefully."
  echo ""
  echo "✅ Proceeding with commit..."
  echo ""
  echo "💡 To bypass completely: touch .codesage-bypass"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0

elif [ $TOTAL_ISSUES -gt 0 ]; then
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "ℹ️  COMMIT ALLOWED"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "ℹ️  $TOTAL_ISSUES minor issue(s) found"
  echo ""
  echo "💡 Review the feedback above to improve code quality."
  echo ""
  echo "✅ No blocking issues detected"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0

else
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "✅ COMMIT APPROVED"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "🎉 All code passed security analysis!"
  echo "✅ No issues detected"
  echo ""
  echo "💡 Keep up the great work writing secure code!"
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  exit 0
fi
