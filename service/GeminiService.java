package service;

import model.Question;
import utils.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeminiService {
    private static final String FIXED_MODEL = "gemini-2.5-flash";
    private static final int MAX_OUTPUT_TOKENS = 1400;
    private static final int MAX_RETRIES = 0;
    private static final long CACHE_TTL_MILLIS = 15 * 60 * 1000;
    private static final long DISK_CACHE_TTL_MILLIS = 24 * 60 * 60 * 1000;
    private static final long FAILURE_COOLDOWN_MILLIS = 5 * 1000;
    private static final Path DISK_CACHE_DIR = Paths.get("data", "cache", "ai");

    private final HttpClient httpClient;
    private final JSONParser parser;
    private final Map<String, CacheEntry> responseCache;
    private long cooldownUntilMillis;
    private String lastErrorMessage;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.parser = new JSONParser();
        this.responseCache = new HashMap<>();
        this.cooldownUntilMillis = 0L;
        this.lastErrorMessage = "";
    }

    public List<Question> generateMCQsFromTopic(String topic, int count) {
        String normalizedTopic = topic == null ? "General Knowledge" : topic.trim();
        String prompt = "Generate " + count + " MCQs on " + normalizedTopic + ". Return STRICT JSON in this format:\n" +
                "[\n" +
                "{\n" +
                "'question': '',\n" +
                "'options': ['A', 'B', 'C', 'D'],\n" +
                "'correctAnswer': '',\n" +
                "'explanation': '',\n" +
                "'topic': '',\n" +
                "'difficulty': ''\n" +
                "}\n" +
                "]";
        return generateMCQs(prompt, "topic:" + normalizedTopic.toLowerCase(Locale.ROOT) + ":" + count, count);
    }

    public List<Question> generateMCQsFromTopicWithDifficulty(String topic, String difficulty, int count) {
        String normalizedTopic = topic == null || topic.isBlank() ? "General Knowledge" : topic.trim();
        String normalizedDifficulty = difficulty == null || difficulty.isBlank() ? "Medium" : difficulty.trim();

        String prompt = "Generate exactly " + count + " MCQs on the topic '" + normalizedTopic +
                "' at " + normalizedDifficulty + " difficulty. " +
                "Return ONLY a strict JSON array using double quotes and these fields only: " +
                "question, options, correctAnswer, explanation, topic, difficulty. " +
            "Each options array must contain exactly 4 options and correctAnswer must match one of them. " +
            "Keep explanation to one short sentence (max 18 words).";

        String cacheKey = "topic:" + normalizedTopic.toLowerCase(Locale.ROOT) +
                ":difficulty:" + normalizedDifficulty.toLowerCase(Locale.ROOT) +
                ":" + count;

        List<Question> questions = generateMCQs(prompt, cacheKey, count);
        List<Question> normalized = new ArrayList<>();
        for (Question question : questions) {
            if (question == null || question.getOptions() == null || question.getOptions().length != 4) {
                continue;
            }
            String qTopic = question.getTopic() == null || question.getTopic().isBlank() ? normalizedTopic : question.getTopic();
            String qDifficulty = question.getDifficulty() == null || question.getDifficulty().isBlank() ? normalizedDifficulty : question.getDifficulty();
            normalized.add(new Question(
                    question.getQuestionText(),
                    question.getOptions(),
                    question.getCorrectAnswer(),
                    question.getExplanation(),
                    qTopic,
                    qDifficulty,
                    question.isMandatory()
            ));
        }
        return normalized;
    }

    public List<Question> generateMixedDifficultyMCQs(String topic, int totalCount) {
        int safeTotal = Math.max(1, totalCount);
        int base = safeTotal / 3;
        int remainder = safeTotal % 3;

        int easyCount = base + (remainder > 0 ? 1 : 0);
        int mediumCount = base + (remainder > 1 ? 1 : 0);
        int hardCount = base;

        String normalizedTopic = topic == null || topic.isBlank() ? "General Knowledge" : topic.trim();
        String prompt = "Generate exactly " + safeTotal + " MCQs on topic '" + normalizedTopic +
                "' with this exact difficulty distribution: Easy=" + easyCount + ", Medium=" + mediumCount +
                ", Hard=" + hardCount + ". " +
                "Return ONLY strict JSON array with double quotes and fields: question, options, correctAnswer, explanation, topic, difficulty. " +
            "Each options array must contain exactly 4 options and correctAnswer must match one option exactly. " +
            "Keep explanation to one short sentence (max 18 words).";

        String cacheKey = "topic:" + normalizedTopic.toLowerCase(Locale.ROOT) + ":mixed:" + safeTotal +
                ":e" + easyCount + ":m" + mediumCount + ":h" + hardCount;

        return generateMCQs(prompt, cacheKey, safeTotal);
    }

    public List<Question> generateMCQsFromNotes(String notesText, int count) {
        String compactNotes = normalizeNotes(notesText);
        String prompt = "Generate " + count + " MCQs strictly based on the notes below. " +
                "Return STRICT JSON with question, options(4), correctAnswer, explanation, topic, difficulty fields only.\n\n" +
                "NOTES:\n" + compactNotes;
        String cacheKey = "notes:" + Integer.toHexString(compactNotes.hashCode()) + ":" + count;
        return generateMCQs(prompt, cacheKey, count);
    }

    public List<Question> generateMCQsFromNotesFile(Path notesFile, int count) throws IOException {
        if (notesFile == null || !Files.exists(notesFile)) {
            throw new IOException("Notes file not found.");
        }

        String fileName = notesFile.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".pdf")) {
            byte[] pdfBytes = Files.readAllBytes(notesFile);
            if (pdfBytes.length == 0) {
                throw new IOException("PDF file is empty.");
            }
            String pdfText = extractTextFromPdf(pdfBytes);
            if (pdfText.isBlank() || pdfText.length() < 80) {
                throw new IOException("Could not extract enough text from PDF. Try a text-based PDF or TXT file.");
            }

            String prompt = "Generate " + count + " MCQs strictly from these notes. " +
                    "Return ONLY strict JSON array with double quotes and fields: question, options, correctAnswer, explanation, topic, difficulty.\n\n" +
                    "NOTES:\n" + normalizeNotes(pdfText);
            String cacheKey = "pdf:" + notesFile.getFileName() + ":" + pdfBytes.length + ":" + count;
            return generateMCQs(prompt, cacheKey, count);
        }

        if (fileName.endsWith(".txt") || fileName.endsWith(".md") || fileName.endsWith(".csv") || fileName.endsWith(".json")) {
            String notesText = Files.readString(notesFile, StandardCharsets.UTF_8);
            return generateMCQsFromNotes(notesText, count);
        }

        throw new IOException("Unsupported notes format. Use TXT, MD, CSV, JSON, or PDF.");
    }

    private List<Question> generateMCQs(String prompt, String cacheKey, int expectedCount) {
        String requestJson = "{\"contents\":[{\"parts\":[{\"text\":\"" + escape(prompt) + "\"}]}]," +
                "\"generationConfig\":{\"temperature\":0.2,\"topP\":0.8,\"candidateCount\":1,\"maxOutputTokens\":" +
                MAX_OUTPUT_TOKENS + ",\"responseMimeType\":\"application/json\"}}";
        return executeRequestWithRetry(requestJson, cacheKey, Math.max(1, expectedCount));
    }

    private List<Question> executeRequestWithRetry(String requestJson, String cacheKey, int expectedCount) {
        lastErrorMessage = "";
        long now = System.currentTimeMillis();
        CacheEntry cached = responseCache.get(cacheKey);
        if (cached != null && now < cached.expiresAtMillis && cached.questions.size() >= expectedCount) {
            return new ArrayList<>(cached.questions);
        }
        if (cached != null && cached.questions.size() < expectedCount) {
            responseCache.remove(cacheKey);
        }

        List<Question> diskCached = readDiskCacheEntry(cacheKey, expectedCount);
        if (!diskCached.isEmpty()) {
            responseCache.put(cacheKey, new CacheEntry(diskCached, System.currentTimeMillis() + CACHE_TTL_MILLIS));
            return diskCached;
        }

        if (now < cooldownUntilMillis) {
            lastErrorMessage = "Gemini cooldown active. Please retry shortly.";
            return new ArrayList<>();
        }

        String apiKey = getConfig("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            lastErrorMessage = "GEMINI_API_KEY is missing. Check .env configuration.";
            return new ArrayList<>();
        }

        String[] modelCandidates = new String[]{FIXED_MODEL};

        for (String selectedModel : modelCandidates) {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + selectedModel + ":generateContent";
            String activeRequestJson = supportsJsonMode(selectedModel)
                    ? requestJson
                    : requestJson.replace(",\"responseMimeType\":\"application/json\"", "");

            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("X-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(activeRequestJson))
                        .build();

                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    int code = response.statusCode();

                    if (code >= 200 && code < 300) {
                        String generatedText = extractGeneratedText(response.body());
                        if (generatedText.isBlank()) {
                            lastErrorMessage = "Gemini response did not contain quiz content.";
                            return new ArrayList<>();
                        }

                        List<Question> questions;
                        try {
                            questions = parser.parseQuestions(generatedText);
                        } catch (IllegalArgumentException parseEx) {
                            questions = parser.parseQuestions(coerceJsonQuotes(generatedText));
                        }
                        if (questions.size() < expectedCount) {
                            if (attempt < MAX_RETRIES) {
                                sleepQuietly(nextBackoffMillis(attempt, 500));
                                continue;
                            }
                            lastErrorMessage = "Gemini returned only " + questions.size() + " questions. Expected " + expectedCount + ".";
                            break;
                        }
                        responseCache.put(cacheKey, new CacheEntry(questions, System.currentTimeMillis() + CACHE_TTL_MILLIS));
                        writeDiskCacheEntry(cacheKey, questions);
                        return questions;
                    }

                    if (code == 429) {
                        if (attempt < MAX_RETRIES) {
                            sleepQuietly(nextBackoffMillis(attempt, 429));
                            continue;
                        }
                        long retryMillis = parseRetryDelayMillis(response.body());
                        long effectiveCooldown = retryMillis > 0 ? retryMillis : FAILURE_COOLDOWN_MILLIS;
                        cooldownUntilMillis = System.currentTimeMillis() + effectiveCooldown;
                        long retrySeconds = Math.max(1L, (effectiveCooldown + 999L) / 1000L);
                        lastErrorMessage = "Gemini quota/rate-limit reached (429) on model " + selectedModel +
                                ". Retry in about " + retrySeconds + "s.";
                        break;
                    }

                    if (code == 503) {
                        if (attempt < MAX_RETRIES) {
                            sleepQuietly(nextBackoffMillis(attempt, 503));
                            continue;
                        }
                        cooldownUntilMillis = System.currentTimeMillis() + (FAILURE_COOLDOWN_MILLIS / 2);
                        lastErrorMessage = "Gemini service is temporarily unavailable (503) on model " + selectedModel + ". Please retry shortly.";
                        break;
                    }

                    if (code == 404) {
                        lastErrorMessage = "Model unavailable: " + selectedModel;
                        break;
                    }

                    if (code == 400 && response.body() != null && response.body().contains("INVALID_ARGUMENT")) {
                        String relaxedRequest = activeRequestJson.replace(",\"responseMimeType\":\"application/json\"", "");
                        if (!relaxedRequest.equals(activeRequestJson) && attempt < MAX_RETRIES) {
                            activeRequestJson = relaxedRequest;
                            sleepQuietly(nextBackoffMillis(attempt, 500));
                            continue;
                        }
                    }

                    lastErrorMessage = "Gemini API error: HTTP " + code;
                    return new ArrayList<>();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    lastErrorMessage = "Gemini request interrupted.";
                    return new ArrayList<>();
                } catch (IOException | IllegalArgumentException ex) {
                    if (attempt < MAX_RETRIES) {
                        sleepQuietly(nextBackoffMillis(attempt, 500));
                        continue;
                    }
                    lastErrorMessage = "Gemini request failed: " + ex.getMessage();
                    break;
                }
            }
        }

        return new ArrayList<>();
    }

    private String normalizeNotes(String notesText) {
        if (notesText == null) {
            return "";
        }

        String trimmed = notesText.trim();
        if (trimmed.length() <= 2000) {
            return trimmed;
        }

        return trimmed.substring(0, 2000);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private long nextBackoffMillis(int attempt, int statusCode) {
        long base;
        if (statusCode == 503) {
            base = 1200L;
        } else if (statusCode == 429) {
            base = 800L;
        } else {
            base = 500L;
        }

        long exp = base * (1L << Math.min(attempt, 4));
        long jitter = (long) (Math.random() * 350L);
        return Math.min(exp + jitter, 12000L);
    }

    private String extractGeneratedText(String responseJson) {
        String candidatesArray = extractArrayValueForKey(responseJson, "candidates");
        if (candidatesArray.isEmpty()) {
            return "";
        }

        List<String> candidateObjects = splitTopLevelObjects(candidatesArray);
        if (candidateObjects.isEmpty()) {
            return "";
        }

        String contentObject = extractObjectValueForKey(candidateObjects.get(0), "content");
        if (contentObject.isEmpty()) {
            return "";
        }

        String partsArray = extractArrayValueForKey(contentObject, "parts");
        if (partsArray.isEmpty()) {
            return "";
        }

        List<String> partObjects = splitTopLevelObjects(partsArray);
        StringBuilder textBuilder = new StringBuilder();
        for (String part : partObjects) {
            String textValue = extractStringValueForKey(part, "text");
            if (textValue.isBlank()) {
                continue;
            }
            if (textBuilder.length() > 0) {
                textBuilder.append('\n');
            }
            textBuilder.append(textValue);
        }

        if (textBuilder.length() == 0) {
            return "";
        }

        String text = textBuilder.toString()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        return text;
    }

    private String coerceJsonQuotes(String value) {
        String normalized = value.trim();
        normalized = normalized.replace("'question'", "\"question\"")
                .replace("'options'", "\"options\"")
                .replace("'correctAnswer'", "\"correctAnswer\"")
                .replace("'explanation'", "\"explanation\"")
                .replace("'topic'", "\"topic\"")
                .replace("'difficulty'", "\"difficulty\"");
        normalized = normalized.replace('\'', '"');
        return normalized;
    }

    private String extractTextFromPdf(byte[] bytes) {
        String pdfBoxText = tryExtractTextWithPdfBox(bytes);
        if (!pdfBoxText.isBlank()) {
            return pdfBoxText;
        }

        String raw = new String(bytes, StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder();
        boolean inTextBlock = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!inTextBlock && c == '(') {
                inTextBlock = true;
                current.setLength(0);
                continue;
            }
            if (inTextBlock) {
                if (c == ')' && (i == 0 || raw.charAt(i - 1) != '\\')) {
                    String piece = current.toString()
                            .replace("\\n", " ")
                            .replace("\\r", " ")
                            .replace("\\t", " ")
                            .replace("\\(", "(")
                            .replace("\\)", ")")
                            .trim();
                    if (piece.chars().filter(Character::isLetterOrDigit).count() >= 2) {
                        sb.append(piece).append(' ');
                    }
                    inTextBlock = false;
                    if (sb.length() > 7000) {
                        break;
                    }
                    continue;
                }
                current.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String tryExtractTextWithPdfBox(byte[] bytes) {
        try {
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdfStripperClass = Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            Object document = pdDocumentClass
                    .getMethod("load", java.io.InputStream.class)
                    .invoke(null, new ByteArrayInputStream(bytes));
            Object stripper = pdfStripperClass.getConstructor().newInstance();
            String text = (String) pdfStripperClass
                    .getMethod("getText", pdDocumentClass)
                    .invoke(stripper, document);
            pdDocumentClass.getMethod("close").invoke(document);
            return text == null ? "" : text.trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractArrayValueForKey(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return "";
        }
        int colon = findNextNonEscaped(json, ':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int open = findNextNonWhitespace(json, colon + 1);
        if (open < 0 || json.charAt(open) != '[') {
            return "";
        }
        int close = findMatchingBracket(json, open, '[', ']');
        if (close < 0) {
            return "";
        }
        return json.substring(open + 1, close);
    }

    private String extractObjectValueForKey(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return "";
        }
        int colon = findNextNonEscaped(json, ':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int open = findNextNonWhitespace(json, colon + 1);
        if (open < 0 || json.charAt(open) != '{') {
            return "";
        }
        int close = findMatchingBracket(json, open, '{', '}');
        if (close < 0) {
            return "";
        }
        return json.substring(open, close + 1);
    }

    private String extractStringValueForKey(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return "";
        }
        int colon = findNextNonEscaped(json, ':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int start = findNextNonWhitespace(json, colon + 1);
        if (start < 0 || json.charAt(start) != '"') {
            return "";
        }
        int end = findStringEnd(json, start + 1);
        if (end < 0) {
            return "";
        }
        return unescapeJsonString(json.substring(start + 1, end));
    }

    private List<String> splitTopLevelObjects(String arrayBody) {
        List<String> objects = new ArrayList<>();
        boolean inString = false;
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '"' && (i == 0 || arrayBody.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(arrayBody.substring(start, i + 1));
                }
            }
        }
        return objects;
    }

    private int findMatchingBracket(String text, int openIndex, char openChar, char closeChar) {
        int depth = 0;
        boolean inString = false;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findStringEnd(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '"' && text.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }

    private int findNextNonWhitespace(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findNextNonEscaped(String text, char target, int from) {
        boolean inString = false;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (!inString && c == target) {
                return i;
            }
        }
        return -1;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage == null ? "" : lastErrorMessage;
    }

    public long getCooldownRemainingMillis() {
        long remaining = cooldownUntilMillis - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    private long parseRetryDelayMillis(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return 0L;
        }

        int marker = responseBody.indexOf("\"retryDelay\"");
        if (marker < 0) {
            return 0L;
        }
        int colon = responseBody.indexOf(':', marker);
        if (colon < 0) {
            return 0L;
        }
        int firstQuote = responseBody.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return 0L;
        }
        int secondQuote = responseBody.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return 0L;
        }

        String raw = responseBody.substring(firstQuote + 1, secondQuote).trim();
        if (raw.isEmpty()) {
            return 0L;
        }

        if (raw.endsWith("s")) {
            try {
                double seconds = Double.parseDouble(raw.substring(0, raw.length() - 1));
                return (long) Math.ceil(seconds * 1000.0);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }

        return 0L;
    }

    private String getConfig(String key) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return null;
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private List<Question> readDiskCacheEntry(String cacheKey, int expectedCount) {
        Path cacheFile = getDiskCachePath(cacheKey);
        if (!Files.exists(cacheFile)) {
            return new ArrayList<>();
        }

        try {
            long modifiedAt = Files.getLastModifiedTime(cacheFile).toMillis();
            long age = System.currentTimeMillis() - modifiedAt;
            if (age > DISK_CACHE_TTL_MILLIS) {
                Files.deleteIfExists(cacheFile);
                return new ArrayList<>();
            }

            String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
            List<Question> questions = parser.parseQuestions(content);
            if (questions.size() < expectedCount) {
                Files.deleteIfExists(cacheFile);
                return new ArrayList<>();
            }
            return questions;
        } catch (Exception ignored) {
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException ignoredDelete) {
                // Ignore cleanup errors.
            }
            return new ArrayList<>();
        }
    }

    private void writeDiskCacheEntry(String cacheKey, List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(DISK_CACHE_DIR);
            Path cacheFile = getDiskCachePath(cacheKey);
            Files.writeString(cacheFile, parser.questionsToJson(questions), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Cache write failures should never block quiz generation.
        }
    }

    private Path getDiskCachePath(String cacheKey) {
        String fileName = Integer.toHexString(cacheKey.hashCode()) + ".json";
        return DISK_CACHE_DIR.resolve(fileName);
    }

    private boolean supportsJsonMode(String modelName) {
        if (modelName == null) {
            return false;
        }
        String lowered = modelName.toLowerCase(Locale.ROOT);
        return lowered.startsWith("gemini-");
    }

    private static class CacheEntry {
        private final List<Question> questions;
        private final long expiresAtMillis;

        private CacheEntry(List<Question> questions, long expiresAtMillis) {
            this.questions = new ArrayList<>(questions);
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
