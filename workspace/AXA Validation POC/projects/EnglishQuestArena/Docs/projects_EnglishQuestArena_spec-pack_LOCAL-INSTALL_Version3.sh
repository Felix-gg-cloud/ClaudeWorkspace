#!/usr/bin/env bash
set -euo pipefail

# Create directories
mkdir -p projects/EnglishQuestArena/spec-pack/seed/examples

echo "Now copy the files from chat into the paths under projects/EnglishQuestArena/spec-pack/ ..."
echo "After creating files, run:"
echo "  cd <your-repo-root>"
echo "  git add projects/EnglishQuestArena/spec-pack"
echo "  git commit -m \"Add Claude Code spec-pack\""
echo "  git push"