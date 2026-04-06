#!/bin/zsh
set -euo pipefail
setopt +o nomatch

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LAUNCHER_DIR="$ROOT_DIR/launcher"
SRC_DIR="$LAUNCHER_DIR/src"
ASSETS_DIR="$LAUNCHER_DIR/assets"
BUILD_DIR="$LAUNCHER_DIR/build"
STAGE_DIR="$BUILD_DIR/stage"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$LAUNCHER_DIR/dist"
LIB_DIR="$LAUNCHER_DIR/lib"
GSON_JAR="$LIB_DIR/gson-2.10.1.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"

rm -rf "$CLASSES_DIR" "$STAGE_DIR"
mkdir -p "$CLASSES_DIR" "$STAGE_DIR" "$DIST_DIR" "$LIB_DIR"

if [[ ! -f "$GSON_JAR" ]]; then
  echo "Downloading Gson to $GSON_JAR"
  curl -L --fail --silent --show-error "$GSON_URL" -o "$GSON_JAR"
fi

javac \
  --release 17 \
  -encoding UTF-8 \
  -cp "$GSON_JAR" \
  -d "$CLASSES_DIR" \
  $(find "$SRC_DIR" -name '*.java' | sort)

cp -R "$CLASSES_DIR"/. "$STAGE_DIR"/
if [[ -d "$ASSETS_DIR" ]]; then
  mkdir -p "$STAGE_DIR/assets"
  cp -R "$ASSETS_DIR"/. "$STAGE_DIR/assets"/
fi
if [[ -f "$ROOT_DIR/icon.png" ]]; then
  mkdir -p "$STAGE_DIR/assets"
  cp "$ROOT_DIR/icon.png" "$STAGE_DIR/assets/icon.png"
fi
if [[ -f "$ROOT_DIR/game.png" ]]; then
  mkdir -p "$STAGE_DIR/assets"
  cp "$ROOT_DIR/game.png" "$STAGE_DIR/assets/game.png"
fi
if [[ -f "$ROOT_DIR/butom.png" ]]; then
  mkdir -p "$STAGE_DIR/assets"
  cp "$ROOT_DIR/butom.png" "$STAGE_DIR/assets/butom.png"
fi
if [[ -f "$ROOT_DIR/back.png" ]]; then
  mkdir -p "$STAGE_DIR/assets"
  cp "$ROOT_DIR/back.png" "$STAGE_DIR/assets/back.png"
fi
(
  cd "$STAGE_DIR"
  jar xf "$GSON_JAR"
  rm -rf META-INF/maven
  rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA
  printf 'Main-Class: com.novaevent.launcher.LauncherApp\n' > manifest.mf
  jar cfm "$DIST_DIR/${LAUNCHER_JAR_NAME:-zlauncher.jar}" manifest.mf .
)

echo "Built $DIST_DIR/${LAUNCHER_JAR_NAME:-zlauncher.jar}"
