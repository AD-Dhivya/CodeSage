
#!/usr/bin/env bash

# Get project root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ Not in a Git repository."
  exit 1
fi

cd "$PROJECT_ROOT" || exit 1

echo "🛡️ CodeSage: AI-powered pre-commit review starting..."

# Check if backend is running
echo "🔍 Checking backend health at http://127.0.0.1:8080/api/health"
RESPONSE=$(curl -s --fail http://127.0.0.1:8080/api/health)
EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ HTTP request to /api/health failed."
  echo "💡 Make sure your backend is running with: ./mvnw spring-boot:run"
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

HAS_ISSUES=0

# Analyze each file
while IFS= read -r file; do
  if [ ! -f "$file" ]; then
    echo "⚠️  File not found: $file"
    continue
  fi

  echo "🔍 Analyzing: $file"

  # Detect language
  case "${file##*.}" in
    "java") LANGUAGE="Java" ;;
    "js")   LANGUAGE="JavaScript" ;;
    "py")   LANGUAGE="Python" ;;
    "ts")   LANGUAGE="TypeScript" ;;
    *)      LANGUAGE="Generic" ;;
  esac

  # Read file content (limit size)
  CONTENT=$(cat "$file" | head -c 2048)
  if [ ${#CONTENT} -ge 2048 ]; then
    CONTENT="$CONTENT... [truncated]"
  fi

  # Debug: show payload
  echo "📤 Sending to AI backend:"
  echo "   Language: $LANGUAGE"
  echo "   Code: >>>$CONTENT<<<"
  echo ""

  # Send to CodeSage backend
  ANALYSIS_RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/analyze \
    -H "Content-Type: application/json" \
    -d "{
      \"code\": $(printf '%s' "$CONTENT" | jq -sR .),
      \"language\": \"$LANGUAGE\"
    }")

  # Debug: show raw response
  echo "📥 AI Response: $ANALYSIS_RESPONSE"
  echo ""

  # Extract feedback
  FEEDBACK=$(echo "$ANALYSIS_RESPONSE" | jq -r '.suggestions // empty')

  if [ -z "$FEEDBACK" ] || [ "$FEEDBACK" = "null" ]; then
    echo "❌ Failed to get AI feedback."
    echo "💡 Raw response: $ANALYSIS_RESPONSE"
    exit 1
  fi

  # Show AI feedback
  echo "🧠 CodeSage AI Feedback:"
  echo "$FEEDBACK" | sed 's/^/   → /'
  echo ""

  # Check if AI wants to block
  BLOCK=$(echo "$ANALYSIS_RESPONSE" | jq -r '.blockCommit // false')
  if [[ "$BLOCK" == "true" || "$BLOCK" == true ]]; then
    HAS_ISSUES=1
  fi
done <<< "$STAGED_FILES"

# Final decision
if [ $HAS_ISSUES -eq 1 ]; then
  echo "🛑 Commit blocked by CodeSage AI. Fix issues and try again."
  echo "💡 You're learning! That's how great developers grow."
  exit 1
else
  echo "✅ All code passes CodeSage AI review. Commit approved!"
  exit 0
fi