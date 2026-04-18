#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build"
CLASSES_DIR="$BUILD_DIR/classes"
DIST_DIR="$ROOT_DIR/dist"
MANIFEST_FILE="$BUILD_DIR/manifest.mf"

rm -rf "$CLASSES_DIR" "$DIST_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR" "$BUILD_DIR"

find "$ROOT_DIR" \
  -type d \( -name .git -o -name build -o -name dist \) -prune -o \
  -name "*.java" -print0 | xargs -0 javac -d "$CLASSES_DIR"

cat > "$MANIFEST_FILE" <<EOF
Main-Class: Main
EOF

jar --create --file "$DIST_DIR/javakava.jar" --manifest "$MANIFEST_FILE" -C "$CLASSES_DIR" .

echo "Built: $DIST_DIR/javakava.jar"
