# JavaKava Quiz App

A Java Swing quiz application with AI-assisted MCQ generation using Gemini.

## What is working

- **Environment setup:** only `GEMINI_API_KEY` is required in `.env`.
- **Model fallback chain:** automatic fallback in this order:
  1. `gemini-3.1-flash-lite-preview`
  2. `gemini-3-flash-preview`
  3. `gemini-2.5-flash`
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
