#!/usr/bin/env bash
# Публикует GitHub-релиз: сборка APK → пуш ветки → создание релиза → загрузка APK.
#
# Использование:
#   ./scripts/release.sh <tag> [название]
#   ./scripts/release.sh v1.2
#   ./scripts/release.sh v1.2 "Deepal Code Gen 1.2"
#
# Токен: env GITHUB_TOKEN, иначе берётся из git credential store (~/.git-credentials).
# Примечание: релиз создаётся через python3/urllib — curl в этой среде не
# договаривается с api.github.com по HTTP/2 (POST падает с ошибкой 43).
set -euo pipefail

TAG="${1:?Использование: release.sh <tag> [название]}"
NAME="${2:-}"
REPO="free4fan/deepal-code-generator"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BRANCH="$(git -C "$ROOT" rev-parse --abbrev-ref HEAD)"

case "$TAG" in
    v?*) ;;
    *) echo "Ошибка: тег должен начинаться с 'v' (например, v1.2). Получено: $TAG" >&2; exit 1 ;;
esac

# --- 1. Сборка APK ----------------------------------------------------------
echo ">> Сборка APK..."
(cd "$ROOT/codegen-app" && ./gradlew -q :app:assembleRelease)
cp "$ROOT/codegen-app/app/build/outputs/apk/release/app-release.apk" "$ROOT/deepal_code_gen.apk"
VERSION="$(sed -n "s/^[[:space:]]*versionName '\(.*\)'.*/\1/p" "$ROOT/codegen-app/app/build.gradle")"

# --- 2. Пуш ветки (будет target_commitish релиза) ---------------------------
echo ">> Пушим $BRANCH..."
git -C "$ROOT" push -u origin "$BRANCH"

# --- 3. Changelog: коммиты с предыдущего тега --------------------------------
LAST_TAG="$(git -C "$ROOT" describe --tags --abbrev=0 2>/dev/null || true)"
if [ -n "$LAST_TAG" ]; then
    CHANGES="$(git -C "$ROOT" log --oneline "${LAST_TAG}..HEAD")"
else
    CHANGES="$(git -C "$ROOT" log --oneline | tac)"
fi

# --- 4. Создание релиза и загрузка APK ---------------------------------------
echo ">> Публикуем релиз $TAG..."
python3 - "$REPO" "$TAG" "$NAME" "$VERSION" "$BRANCH" "$CHANGES" "$ROOT/deepal_code_gen.apk" <<'PYEOF'
import json
import os
import sys
import urllib.error
import urllib.request

repo, tag, name, version, branch, changes, apk_path = sys.argv[1:8]
pat = os.environ.get("GITHUB_TOKEN")
if not pat:
    pat = open("/root/.git-credentials").read().strip().split(":", 2)[2].split("@")[0]
base = "https://api.github.com/repos/" + repo
HEADERS = {
    "Authorization": "Bearer " + pat,
    "Accept": "application/vnd.github+json",
    "User-Agent": "deepal-codegen-release",
}


def req(method, url, body=None, ctype="application/json"):
    data = json.dumps(body).encode() if body is not None else None
    headers = dict(HEADERS)
    if data is not None:
        headers["Content-Type"] = ctype
    r = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        return urllib.request.urlopen(r, timeout=600)
    except urllib.error.HTTPError as e:
        sys.exit("HTTP %s: %s" % (e.code, e.read().decode()[:400]))


if not name:
    name = tag

lines = ["## What's new"]
if changes.strip():
    lines += ["- " + c for c in changes.strip().splitlines()]
else:
    lines.append("- (см. коммиты)")
lines += [
    "",
    "## Install",
    "Download `deepal_code_gen.apk` and install it (version %s)." % version,
    "",
    "## Build from source",
    "```",
    "cd codegen-app",
    "./gradlew :app:assembleRelease",
    "```",
]

resp = json.load(req("POST", base + "/releases", {
    "tag_name": tag,
    "name": name,
    "target_commitish": branch,
    "body": "\n".join(lines),
    "draft": False,
    "prerelease": False,
}))
print("Release: %s" % resp["html_url"])

with open(apk_path, "rb") as f:
    apk = f.read()
upload = resp["upload_url"].replace("{?name,label}", "?name=deepal_code_gen.apk")
asset = json.load(req("POST", upload, apk,
                      "application/vnd.android.package-archive"))
print("Asset:   %s (%d bytes)" % (asset["browser_download_url"], asset["size"]))
PYEOF

# --- 5. Подтягиваем новый тег локально ---------------------------------------
git -C "$ROOT" fetch origin "refs/tags/$TAG:refs/tags/$TAG"
echo "Готово: $TAG на $BRANCH"
