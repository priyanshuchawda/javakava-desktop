import model.Question;
import service.GeminiService;
import utils.EnvLoader;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeminiServiceLiveFormatTest {
    public static void main(String[] args) {
        EnvLoader.loadDotEnv(".env");
        String apiKey = readConfig("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing in .env");
        }

        final int expectedCount = 9;
        GeminiService geminiService = new GeminiService();
        List<Question> questions = geminiService.generateMixedDifficultyMCQs("Java", expectedCount);

        if (questions.size() != expectedCount) {
            throw new IllegalStateException(
                    "Live format test failed. Returned " + questions.size() + " of " + expectedCount +
                            ". Error: " + geminiService.getLastErrorMessage());
        }

        Map<String, Integer> difficultyCounts = new LinkedHashMap<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            assertQuestionFormat(q, i);
            String difficulty = q.getDifficulty().trim().toLowerCase(Locale.ROOT);
            difficultyCounts.put(difficulty, difficultyCounts.getOrDefault(difficulty, 0) + 1);
        }

        System.out.println("Gemini live format test passed. Questions generated: " + questions.size());
        System.out.println("Difficulty distribution: " + difficultyCounts);
    }

    private static void assertQuestionFormat(Question q, int index) {
        if (q == null) {
            throw new IllegalStateException("Question #" + index + " is null.");
        }
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            throw new IllegalStateException("Question #" + index + " has empty question text.");
        }
        if (q.getOptions() == null || q.getOptions().length != 4) {
            throw new IllegalStateException("Question #" + index + " must contain exactly 4 options.");
        }
        for (int i = 0; i < q.getOptions().length; i++) {
            String option = q.getOptions()[i];
            if (option == null || option.isBlank()) {
                throw new IllegalStateException("Question #" + index + " option #" + i + " is empty.");
            }
        }
        if (q.getCorrectAnswer() == null || q.getCorrectAnswer().isBlank()) {
            throw new IllegalStateException("Question #" + index + " has empty correctAnswer.");
        }
        String normalizedCorrect = q.getCorrectAnswer().trim();
        boolean matches = Arrays.stream(q.getOptions())
                .map(String::trim)
                .anyMatch(normalizedCorrect::equals);
        if (!matches) {
            throw new IllegalStateException("Question #" + index + " correctAnswer does not match options.");
        }
        if (q.getExplanation() == null || q.getExplanation().isBlank()) {
            throw new IllegalStateException("Question #" + index + " has empty explanation.");
        }
        if (q.getTopic() == null || q.getTopic().isBlank()) {
            throw new IllegalStateException("Question #" + index + " has empty topic.");
        }
        if (q.getDifficulty() == null || q.getDifficulty().isBlank()) {
            throw new IllegalStateException("Question #" + index + " has empty difficulty.");
        }
    }

    private static String readConfig(String key) {
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
}
