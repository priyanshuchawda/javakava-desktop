#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_VERSION="${1:-1.0.0}"
RUNTIME_DIR="$ROOT_DIR/build/runtime"
PACKAGE_INPUT_DIR="$ROOT_DIR/build/package-input"

"$ROOT_DIR/scripts/build-jar.sh"

rm -rf "$ROOT_DIR/dist/JavaKava" "$ROOT_DIR/dist/JavaKava-linux-app-image.tar.gz" "$PACKAGE_INPUT_DIR"
rm -rf "$RUNTIME_DIR"
mkdir -p "$PACKAGE_INPUT_DIR"
cp "$ROOT_DIR/dist/javakava.jar" "$PACKAGE_INPUT_DIR/javakava.jar"

MODULES="$(jdeps --multi-release 21 --ignore-missing-deps --print-module-deps "$ROOT_DIR/dist/javakava.jar")"
jlink \
  --add-modules "$MODULES" \
  --output "$RUNTIME_DIR" \
  --strip-debug \
  --compress=2 \
  --no-header-files \
  --no-man-pages

jpackage \
  --type app-image \
  --input "$PACKAGE_INPUT_DIR" \
  --dest "$ROOT_DIR/dist" \
  --name "JavaKava" \
  --main-jar "javakava.jar" \
  --main-class "Main" \
  --runtime-image "$RUNTIME_DIR" \
  --app-version "$APP_VERSION"

tar -C "$ROOT_DIR/dist" -czf "$ROOT_DIR/dist/JavaKava-linux-app-image.tar.gz" JavaKava

echo "Built app image: $ROOT_DIR/dist/JavaKava"
echo "Built archive: $ROOT_DIR/dist/JavaKava-linux-app-image.tar.gz"
