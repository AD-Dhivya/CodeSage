
#!/usr/bin/env bash

# Get project root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ Not in a Git repository."
  exit 1
fi

cd "$PROJECT_ROOT" || exit 1

echo "🛡️ CodeSage: AI-powered educational code mentor starting..."

# Check if backend is running - NOW USING DOCKER ADDRESS!
echo "🔍 Checking backend health at http://host.docker.internal:8080/api/health"
RESPONSE=$(curl -s --fail http://host.docker.internal:8080/api/health)
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ HTTP request to /api/health failed."
  echo "💡 Make sure your backend is running with: docker-compose up"
  exit 1
fi

echo "📡 Health response: $RESPONSE"

if echo "$RESPONSE" | grep -q "alive"; then
  echo "✅ Backend is UP. Scanning staged files..."
else
  echo "❌ Backend not ready. Response does not contain 'alive'."
  echo "💡 Full response: $RESPONSE"
  exit 1
fi

# Debug: show all staged files
echo "🔍 All staged files:"
git diff --cached --name-only --diff-filter=d | cat -A
echo ""

# Get staged code files
echo "🔍 Looking for code files with extensions: .java, .js, .py, .ts"
STAGED_FILES=$(git diff --cached --name-only --diff-filter=d | grep -E '\.(java|js|py|ts)$')

# Debug: show what was captured
echo "📄 Raw STAGED_FILES value:"
printf '%s' "$STAGED_FILES" | cat -A
echo ""

# Check if any files found
if [ -z "$STAGED_FILES" ]; then
  echo "✅ No code files to analyze. Commit allowed."
  exit 0
fi

echo "✅ Found files to analyze:"
echo "$STAGED_FILES"
echo ""

# Ensure jq is available
if ! command -v jq >/dev/null 2>&1; then
  echo "❌ This hook requires 'jq' but it's not installed."
  echo "💡 Install jq: https://stedolan.github.io/jq/download/"
  exit 1
fi

HAS_CRITICAL_SECURITY_ISSUES=0

# Analyze each file
while IFS= read -r file; do
  if [ ! -f "$file" ]; then
    echo "⚠️  File not found: $file"
    continue
  fi

  echo "🔍 Analyzing: $file"

  # Detect language
  case "${file##*.}" in
    "java") LANGUAGE="java" ;;
    "js")   LANGUAGE="javascript" ;;
    "py")   LANGUAGE="python" ;;
    "ts")   LANGUAGE="typescript" ;;
    *)      LANGUAGE="java" ;;
  esac

  # Read file content (limit size)
CONTENT=$(cat "$file" | head -c 4096)
if [ ${#CONTENT} -ge 4096 ]; then
  CONTENT="$CONTENT... [truncated]"
fi

  # Debug: show payload
  echo "📤 Sending to AI backend:"
  echo "   Language: $LANGUAGE"
  echo "   Code: >>>$CONTENT<<<"
  echo ""

  # Send to CodeSage backend - NOW USING DOCKER ADDRESS!
  ANALYSIS_RESPONSE=$(curl -s -X POST http://host.docker.internal:8080/api/analyze \
    -H "Content-Type: application/json" \
    -d "{
      \"code\": $(printf '%s' "$CONTENT" | jq -sR .),
      \"language\": \"$LANGUAGE\"
    }")

  # Debug: show raw response
  echo "📥 AI Response: $ANALYSIS_RESPONSE"
  echo ""

  # Extract feedback
  FEEDBACK=$(echo "$ANALYSIS_RESPONSE" | jq -r '.analysis // empty')

  if [ -z "$FEEDBACK" ] || [ "$FEEDBACK" = "null" ]; then
    echo "❌ Failed to get AI feedback."
    echo "💡 Raw response: $ANALYSIS_RESPONSE"

    # Check if there's an error message
    ERROR_MSG=$(echo "$ANALYSIS_RESPONSE" | jq -r '.error // empty')
    if [ ! -z "$ERROR_MSG" ]; then
      echo "❌ Error from server: $ERROR_MSG"
    fi

    exit 1
  fi

  # Show AI feedback
  echo "🧠 CodeSage Educational Feedback:"
  echo "$FEEDBACK" | sed 's/^/   → /'
  echo ""

  # Check if there's a CRITICAL security issue (only block for these)
  if echo "$FEEDBACK" | grep -q "Severity: CRITICAL" &&
     echo "$FEEDBACK" | grep -q "Category: Security"; then
    HAS_CRITICAL_SECURITY_ISSUES=1
    echo "⚠️ CRITICAL SECURITY ISSUE FOUND! Commit will be blocked."
  fi
done <<< "$STAGED_FILES"

# Final decision
if [ $HAS_CRITICAL_SECURITY_ISSUES -eq 1 ]; then
  echo "🛑 Commit blocked by CodeSage AI. Please fix critical security issues."
  echo "💡 Remember: Security is everyone's responsibility!"
  exit 1
else
  echo "✅ All code passes CodeSage review. Commit approved with educational feedback!"
  echo "💡 Keep learning and growing as a developer!"
  exit 0
fi
