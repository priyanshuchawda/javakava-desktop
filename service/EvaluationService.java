package service;

import model.Attempt;
import model.Result;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationService {

    public Result evaluate(List<Attempt> attempts, boolean negativeMarkingEnabled) {
        return evaluate(attempts, negativeMarkingEnabled, "Untitled Test");
    }

    public Result evaluate(List<Attempt> attempts, boolean negativeMarkingEnabled, String quizTitle) {
        double scoreValue = 0.0;
        Map<String, Integer> correctCount = new HashMap<>();
        Map<String, Integer> totalCount = new HashMap<>();

        for (Attempt attempt : attempts) {
            if (attempt.isSkipped() && !attempt.getQuestion().isMandatory()) {
                continue;
            }
            String topic = attempt.getTopic();
            totalCount.put(topic, totalCount.getOrDefault(topic, 0) + 1);

            if (attempt.isCorrect()) {
                scoreValue += 1.0;
                correctCount.put(topic, correctCount.getOrDefault(topic, 0) + 1);
            } else if (negativeMarkingEnabled) {
                scoreValue -= 0.25;
            }
        }

        int totalQuestions = attempts.size();
        double accuracy = totalQuestions == 0 ? 0.0 : (countCorrect(attempts) * 100.0) / totalQuestions;

        Map<String, Double> topicPerformance = new HashMap<>();
        for (Map.Entry<String, Integer> entry : totalCount.entrySet()) {
            String topic = entry.getKey();
            int total = entry.getValue();
            int correct = correctCount.getOrDefault(topic, 0);
            double topicAccuracy = total == 0 ? 0.0 : (correct * 100.0) / total;
            topicPerformance.put(topic, topicAccuracy);
        }

        String attemptedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new Result(scoreValue, totalQuestions, accuracy, topicPerformance, attempts, quizTitle, attemptedAt);
    }

    private int countCorrect(List<Attempt> attempts) {
        int correct = 0;
        for (Attempt attempt : attempts) {
            if (attempt.isCorrect()) {
                correct++;
            }
        }
        return correct;
    }
}
