import model.Question;
import service.GeminiService;
import utils.EnvLoader;

import java.util.List;

public class GeminiServiceSmokeTest {
    public static void main(String[] args) {
        EnvLoader.loadDotEnv(".env");
        String apiKey = readConfig("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is missing in .env");
        }

        GeminiService geminiService = new GeminiService();
        List<Question> questions = geminiService.generateMCQsFromTopicWithDifficulty("Java", "Easy", 2);

        if (questions.size() < 2) {
            throw new IllegalStateException(
                    "Gemini smoke test failed. Returned " + questions.size() +
                            " questions. Error: " + geminiService.getLastErrorMessage());
        }

        for (Question question : questions) {
            if (question == null || question.getQuestionText() == null || question.getQuestionText().isBlank()) {
                throw new IllegalStateException("Invalid question payload: question text is empty.");
            }
            if (question.getOptions() == null || question.getOptions().length != 4) {
                throw new IllegalStateException("Invalid question payload: options must contain exactly 4 items.");
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                throw new IllegalStateException("Invalid question payload: correctAnswer is empty.");
            }
        }

        System.out.println("Gemini smoke test passed. Questions generated: " + questions.size());
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
