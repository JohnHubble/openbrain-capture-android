#!/usr/bin/env bash
# SessionStart hook: warns Claude when the current worktree's base is stale.
#
# Detects two conditions:
#   1. Local commits on any branch that aren't on origin/main.
#   2. Local branches not merged into origin/main.
#
# If either is non-empty, emits a SessionStart additionalContext block so the
# warning lands in the model's context as a system-reminder. Silent otherwise.
#
# Why: a previous Claude session shipped 12 commits to claude/intelligent-faraday-78e1a2
# without merging to main, and the next session branched from main and missed all of it.
# This hook prevents the same trap on every future session-start in this repo.

set -euo pipefail

DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

if ! git -C "$DIR" rev-parse --git-dir >/dev/null 2>&1; then
  exit 0
fi

# Commits reachable from any local branch or remote, excluding origin/main.
commits=$(git -C "$DIR" log --oneline --branches --remotes "^origin/main" -20 2>/dev/null || true)

# Local + remote branches not yet merged into origin/main.
branches=$(git -C "$DIR" branch -a --no-merged origin/main 2>/dev/null || true)

# Drop blank lines for the gate; keep them in the displayed payload.
nontrivial=$( { printf '%s\n' "$commits"; printf '%s\n' "$branches"; } | grep -v '^[[:space:]]*$' || true)

if [ -z "$nontrivial" ]; then
  exit 0
fi

payload=$(printf '%s\n---\n%s' "$commits" "$branches")

jq -nc \
  --arg ctx "⚠️  UNMERGED WORK DETECTED — verify base before editing.

A previous session may have shipped commits to a claude/* branch without merging to main. Before doing any work, ask the user which branch is the source of truth.

$payload" \
  '{"hookSpecificOutput": {"hookEventName": "SessionStart", "additionalContext": $ctx}}'
