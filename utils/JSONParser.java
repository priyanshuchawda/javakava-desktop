package utils;

import model.Question;
import model.Quiz;
import model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONParser {

    public List<Question> parseQuestions(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("JSON content is empty.");
        }

        if (trimmed.startsWith("```")) {
            trimmed = extractCodeFence(trimmed);
        }

        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON format for question array.");
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        List<String> objectBlocks = splitObjects(body);
        List<Question> questions = new ArrayList<>();

        for (String object : objectBlocks) {
            String questionText = extractStringValue(object, "question");
            String[] options = extractArrayValue(object, "options");
            if (options.length != 4) {
                throw new IllegalArgumentException("Each question must have exactly 4 options.");
            }
            String correctAnswer = extractStringValue(object, "correctAnswer");
            String explanation = extractStringValue(object, "explanation");
            String topic = extractStringValue(object, "topic");
            String difficulty = extractStringValue(object, "difficulty");
            boolean mandatory = extractBooleanValue(object, "mandatory", true);

            questions.add(new Question(questionText, options, correctAnswer, explanation, topic, difficulty, mandatory));
        }

        return questions;
    }

    public String questionsToJson(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            sb.append("  {\n");
            sb.append("    \"question\": \"").append(escape(q.getQuestionText())).append("\",\n");
            sb.append("    \"options\": [");
            String[] options = q.getOptions();
            for (int j = 0; j < options.length; j++) {
                sb.append("\"").append(escape(options[j])).append("\"");
                if (j < options.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("],\n");
            sb.append("    \"correctAnswer\": \"").append(escape(q.getCorrectAnswer())).append("\",\n");
            sb.append("    \"explanation\": \"").append(escape(q.getExplanation())).append("\",\n");
            sb.append("    \"topic\": \"").append(escape(q.getTopic())).append("\",\n");
            sb.append("    \"difficulty\": \"").append(escape(q.getDifficulty())).append("\",\n");
            sb.append("    \"mandatory\": ").append(q.isMandatory()).append("\n");
            sb.append("  }");
            if (i < questions.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public String quizToJson(Quiz quiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"title\": \"").append(escape(quiz.getTitle())).append("\",\n");
        sb.append("  \"subject\": \"").append(escape(quiz.getSubject())).append("\",\n");
        sb.append("  \"classLevel\": \"").append(escape(quiz.getClassLevel())).append("\",\n");
        sb.append("  \"tags\": [");
        int tagIndex = 0;
        for (String tag : quiz.getTags()) {
            sb.append("\"").append(escape(tag)).append("\"");
            if (tagIndex < quiz.getTags().size() - 1) {
                sb.append(", ");
            }
            tagIndex++;
        }
        sb.append("],\n");
        sb.append("  \"difficultyProfile\": \"").append(escape(quiz.getDifficultyProfile())).append("\",\n");
        sb.append("  \"timePerQuestionSeconds\": ").append(quiz.getTimePerQuestionSeconds()).append(",\n");
        sb.append("  \"fullTestTimerMode\": ").append(quiz.isFullTestTimerMode()).append(",\n");
        sb.append("  \"fullTestTimeSeconds\": ").append(quiz.getFullTestTimeSeconds()).append(",\n");
        sb.append("  \"negativeMarkingEnabled\": ").append(quiz.isNegativeMarkingEnabled()).append(",\n");
        sb.append("  \"published\": ").append(quiz.isPublished()).append(",\n");
        sb.append("  \"version\": ").append(quiz.getVersion()).append(",\n");
        sb.append("  \"randomizeQuestionOrder\": ").append(quiz.isRandomizeQuestionOrder()).append(",\n");
        sb.append("  \"randomizeOptionOrder\": ").append(quiz.isRandomizeOptionOrder()).append(",\n");
        sb.append("  \"maxAttemptsPerStudent\": ").append(quiz.getMaxAttemptsPerStudent()).append(",\n");
        sb.append("  \"questions\": ").append(questionsToJson(quiz.getQuestions())).append("\n");
        sb.append("}");
        return sb.toString();
    }

    public Quiz parseQuiz(String json) {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new IllegalArgumentException("Invalid quiz JSON object.");
        }

        String title = extractStringValue(trimmed, "title");
        String subject = extractStringValueSafe(trimmed, "subject", "General");
        String classLevel = extractStringValueSafe(trimmed, "classLevel", "General");
        String difficultyProfile = extractStringValueSafe(trimmed, "difficultyProfile", "Mixed");
        int timePerQuestion = extractIntValue(trimmed, "timePerQuestionSeconds", 30);
        boolean fullTestTimerMode = extractBooleanValue(trimmed, "fullTestTimerMode", false);
        int fullTestTimeSeconds = extractIntValue(trimmed, "fullTestTimeSeconds", 0);
        boolean negativeMarkingEnabled = extractBooleanValue(trimmed, "negativeMarkingEnabled", true);
        boolean published = extractBooleanValue(trimmed, "published", false);
        int version = extractIntValue(trimmed, "version", 1);
        boolean randomizeQuestionOrder = extractBooleanValue(trimmed, "randomizeQuestionOrder", false);
        boolean randomizeOptionOrder = extractBooleanValue(trimmed, "randomizeOptionOrder", false);
        int maxAttemptsPerStudent = extractIntValue(trimmed, "maxAttemptsPerStudent", 0);
        String[] tagsArr = extractArrayValueSafe(trimmed, "tags");
        java.util.Set<String> tags = new java.util.HashSet<>();
        for (String t : tagsArr) {
            if (t != null && !t.isBlank()) {
                tags.add(t.trim());
            }
        }
        String questionsArray = extractRawArray(trimmed, "questions");
        List<Question> questions = parseQuestions(questionsArray);
        return new Quiz(
                title,
                questions,
                negativeMarkingEnabled,
                timePerQuestion,
                subject,
                classLevel,
                tags,
                difficultyProfile,
                fullTestTimerMode,
                fullTestTimeSeconds,
                published,
                version,
                randomizeQuestionOrder,
                randomizeOptionOrder,
                maxAttemptsPerStudent
        );
    }

    public String resultToJson(Result result, String studentName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"student\": \"").append(escape(studentName)).append("\",\n");
        sb.append("  \"quizTitle\": \"").append(escape(result.getQuizTitle())).append("\",\n");
        sb.append("  \"attemptedAt\": \"").append(escape(result.getAttemptedAt())).append("\",\n");
        sb.append("  \"score\": ").append(result.getScore()).append(",\n");
        sb.append("  \"totalQuestions\": ").append(result.getTotalQuestions()).append(",\n");
        sb.append("  \"accuracy\": ").append(String.format(java.util.Locale.US, "%.2f", result.getAccuracy())).append(",\n");
        sb.append("  \"topicPerformance\": {\n");

        int i = 0;
        for (Map.Entry<String, Double> entry : result.getTopicPerformance().entrySet()) {
            sb.append("    \"").append(escape(entry.getKey())).append("\": ")
                    .append(String.format(java.util.Locale.US, "%.2f", entry.getValue()));
            if (i < result.getTopicPerformance().size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            i++;
        }

        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String extractCodeFence(String text) {
        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private int extractIntValue(String object, String key, int defaultValue) {
        String raw = extractPrimitiveValue(object, key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean extractBooleanValue(String object, String key, boolean defaultValue) {
        String raw = extractPrimitiveValue(object, key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        return defaultValue;
    }

    private String extractPrimitiveValue(String object, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = object.indexOf(keyPattern);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = object.indexOf(':', keyIndex + keyPattern.length());
        if (colonIndex < 0) {
            return null;
        }
        int i = colonIndex + 1;
        while (i < object.length() && Character.isWhitespace(object.charAt(i))) {
            i++;
        }
        int j = i;
        while (j < object.length() && object.charAt(j) != ',' && object.charAt(j) != '}' && object.charAt(j) != ']') {
            j++;
        }
        return object.substring(i, j).trim();
    }

    private String extractRawArray(String object, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = object.indexOf(keyPattern);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing key: " + key);
        }

        int colonIndex = object.indexOf(':', keyIndex + keyPattern.length());
        int start = object.indexOf('[', colonIndex + 1);
        if (start < 0) {
            throw new IllegalArgumentException("Invalid array for key: " + key);
        }

        int depth = 0;
        boolean inQuote = false;
        for (int i = start; i < object.length(); i++) {
            char c = object.charAt(i);
            if (c == '"' && (i == 0 || object.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (inQuote) {
                continue;
            }
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return object.substring(start, i + 1);
                }
            }
        }
        throw new IllegalArgumentException("Unclosed array for key: " + key);
    }

    private List<String> splitObjects(String body) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inQuote = false;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '"' && (i == 0 || body.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
            }
            if (inQuote) {
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
                    objects.add(body.substring(start, i + 1));
                }
            }
        }

        if (objects.isEmpty()) {
            throw new IllegalArgumentException("No question objects found in JSON.");
        }

        return objects;
    }

    private String extractStringValue(String object, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = object.indexOf(keyPattern);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing key: " + key);
        }

        int colonIndex = object.indexOf(':', keyIndex + keyPattern.length());
        int firstQuote = findNextQuote(object, colonIndex + 1);
        int secondQuote = findClosingQuote(object, firstQuote + 1);

        if (firstQuote < 0 || secondQuote < 0) {
            throw new IllegalArgumentException("Invalid value for key: " + key);
        }

        return unescape(object.substring(firstQuote + 1, secondQuote).trim());
    }

    private String extractStringValueSafe(String object, String key, String defaultValue) {
        try {
            return extractStringValue(object, key);
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    private String[] extractArrayValue(String object, String key) {
        String keyPattern = "\"" + key + "\"";
        int keyIndex = object.indexOf(keyPattern);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing key: " + key);
        }

        int colonIndex = object.indexOf(':', keyIndex + keyPattern.length());
        int start = object.indexOf('[', colonIndex + 1);
        int end = object.indexOf(']', start + 1);
        if (start < 0 || end < 0) {
            throw new IllegalArgumentException("Invalid array value for key: " + key);
        }

        String arrayContent = object.substring(start + 1, end).trim();
        if (arrayContent.isEmpty()) {
            return new String[0];
        }

        List<String> parts = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '"' && (i == 0 || arrayContent.charAt(i - 1) != '\\')) {
                inQuote = !inQuote;
                continue;
            }
            if (c == ',' && !inQuote) {
                parts.add(unescape(current.toString().trim()));
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0) {
            parts.add(unescape(current.toString().trim()));
        }

        return parts.toArray(new String[0]);
    }

    private String[] extractArrayValueSafe(String object, String key) {
        try {
            return extractArrayValue(object, key);
        } catch (IllegalArgumentException ex) {
            return new String[0];
        }
    }

    private int findNextQuote(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                return i;
            }
        }
        return -1;
    }

    private int findClosingQuote(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (text.charAt(i) == '"' && text.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescape(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value;
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        return cleaned
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
