#!/bin/zsh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
"$ROOT_DIR/launcher/build.sh"
exec java -jar "$ROOT_DIR/launcher/dist/zlauncher.jar" "$ROOT_DIR"
