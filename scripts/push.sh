#!/usr/bin/env bash
# Пушит текущую ветку в origin.
# Креденшалы берутся из git credential store (~/.git-credentials, helper=store).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRANCH="$(git -C "$ROOT" rev-parse --abbrev-ref HEAD)"

if [ -n "$(git -C "$ROOT" status --porcelain)" ]; then
    echo "WARNING: есть незакоммиченные изменения:" >&2
    git -C "$ROOT" status --short >&2
fi

git -C "$ROOT" push -u origin "$BRANCH"
echo "Pushed: $BRANCH"
