#!/usr/bin/env bash

# Get project root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ -z "$PROJECT_ROOT" ]; then
  echo "❌ Not in a Git repository."
  exit 1
fi

echo "🛡️ CodeSage: Pre-commit hook starting..."

# Check if backend is running

if ! curl -s --fail http://127.0.0.1:8080/api/health | grep -q "CodeSage is alive"; then
  echo "❌ CodeSage backend is not running!"
  echo "💡 Start it with: ./mvnw spring-boot:run"
  exit 1
fi

echo "✅ Backend is UP. Scanning staged files..."

# Get staged code files
STAGED_FILES=$(git diff --cached --name-only --diff-filter=d | grep -E '\\.(java|js|py|ts)$')

if [ -z "$STAGED_FILES" ]; then
  echo "✅ No code files to analyze. Commit allowed."
  exit 0
fi

echo "📄 Files to analyze:"
echo "$STAGED_FILES"
echo ""

echo "💡 Next: Sending files to CodeSage AI for review..."
echo "✅ For now, commit is allowed."

exit 0
