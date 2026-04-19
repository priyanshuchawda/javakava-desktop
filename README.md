# JavaKava Quiz App

A Java Swing quiz application with AI-assisted MCQ generation using Gemini.

## What is working

- **Environment setup:** only `GEMINI_API_KEY` is required in `.env`.
- **Model fallback chain:** automatic fallback in this order:
  1. `gemini-3.1-flash-lite` (default)
  2. `gemini-3.1-flash-lite-preview`
  3. `gemini-3-flash-preview`
  4. `gemini-2.5-flash`
- **Prompting strategy:** strict JSON-first prompting is enforced for reliable parsing:
  - exact question count
  - fixed schema (`question`, `options`, `correctAnswer`, `explanation`, `topic`, `difficulty`)
  - exactly 4 options per question
  - no markdown / no extra text
- **Smoke test:** `GeminiServiceSmokeTest` verifies real API generation and payload shape.

## Project structure

```text
.
├── Main.java
├── GeminiServiceSmokeTest.java
├── model/
├── service/
├── ui/
└── utils/
```

## Prerequisites

- Java 17+ (JDK) for building/package creation
- Internet access (for Gemini API calls)
- A valid Gemini API key

## Environment setup

1. Copy template:
   ```bash
   cp .env.example .env
   ```
2. Set your key in `.env`:
   ```env
   GEMINI_API_KEY=your_key_here
   ```

You can also set/update the key inside the app from Dashboard using **Set Gemini API Key**.

## Run the app

```bash
rm -rf /tmp/javakava-build && mkdir -p /tmp/javakava-build
find . -name "*.java" -print0 | xargs -0 javac -d /tmp/javakava-build
java -cp /tmp/javakava-build Main
```

## Build runnable JAR

```bash
./scripts/build-jar.sh
java -jar dist/javakava.jar
```

## Build no-JDK desktop package (Linux)

This creates a native **app image** that bundles a Java runtime, so end users do **not** need Java installed.

```bash
./scripts/package-linux-app-image.sh 1.0.0
```

Generated artifacts:

- `dist/JavaKava/` (runnable app image)
- `dist/JavaKava-linux-app-image.tar.gz` (shareable archive)

Run packaged app:

```bash
./dist/JavaKava/bin/JavaKava
```

## Build no-JDK desktop package (Windows)

This creates a native **app image** that bundles a Java runtime, so end users do **not** need Java installed.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows-app-image.ps1 1.0.0
```

Generated artifacts:

- `dist\JavaKava\` (runnable app image)
- `dist\JavaKava-windows-app-image.zip` (shareable archive)

Run packaged app:

```powershell
.\dist\JavaKava\JavaKava.exe
```

## Install for end users (from GitHub Releases)

1. Download `JavaKava-linux-app-image.tar.gz` from the latest release.
2. Extract it:
   ```bash
   tar -xzf JavaKava-linux-app-image.tar.gz
   ```
3. Go into extracted folder and run:
   ```bash
   ./JavaKava/bin/JavaKava
   ```
4. On first AI usage, set API key:
   - Click **Set Gemini API Key** in Dashboard, or
   - create `.env` with `GEMINI_API_KEY=...` in the launch directory.

Optional local install shortcut:

```bash
mkdir -p ~/.local/opt/JavaKava ~/.local/bin
rsync -a --delete JavaKava/ ~/.local/opt/JavaKava/
ln -sf ~/.local/opt/JavaKava/bin/JavaKava ~/.local/bin/javakava
javakava
```

## Publish a GitHub Release (maintainer)

Build artifacts:

```bash
./scripts/package-linux-app-image.sh 1.0.1
```

Create tag + release and upload artifacts:

```bash
gh release create v1.0.1 \
  dist/JavaKava-linux-app-image.tar.gz \
  dist/javakava.jar \
  --repo priyanshuchawda/javakava-desktop \
  --title "JavaKava v1.0.1" \
  --notes "Linux no-JDK package + runnable JAR."
```

## Run Gemini smoke test

```bash
rm -rf /tmp/javakava-build && mkdir -p /tmp/javakava-build
find . -name "*.java" -print0 | xargs -0 javac -d /tmp/javakava-build
java -cp /tmp/javakava-build GeminiServiceSmokeTest
```

Expected output:

```text
Gemini smoke test passed. Questions generated: 2
```

## Notes

- `.env` is ignored by git; `.env.example` is committed.
- Disk cache for AI responses is stored under `data/cache/ai`.
- This app currently uses direct Gemini text generation (no Gemini function-calling/tool-calling API integration).
- For Windows/macOS packaging, run `jpackage` on those OSes (native packaging is OS-specific).
