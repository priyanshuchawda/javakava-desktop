package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {
    private double score;
    private int totalQuestions;
    private double accuracy;
    private Map<String, Double> topicPerformance;
    private List<Attempt> attempts;
    private String quizTitle;
    private String attemptedAt;

    public Result(double score, int totalQuestions, double accuracy,
                  Map<String, Double> topicPerformance, List<Attempt> attempts) {
        this(score, totalQuestions, accuracy, topicPerformance, attempts, "Untitled Test", "");
    }

    public Result(double score,
                  int totalQuestions,
                  double accuracy,
                  Map<String, Double> topicPerformance,
                  List<Attempt> attempts,
                  String quizTitle,
                  String attemptedAt) {
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.accuracy = accuracy;
        this.topicPerformance = new HashMap<>(topicPerformance);
        this.attempts = new ArrayList<>(attempts);
        this.quizTitle = quizTitle == null ? "Untitled Test" : quizTitle;
        this.attemptedAt = attemptedAt == null ? "" : attemptedAt;
    }

    public double getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public Map<String, Double> getTopicPerformance() {
        return topicPerformance;
    }

    public List<Attempt> getAttempts() {
        return attempts;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public String getAttemptedAt() {
        return attemptedAt;
    }

    public List<String> getWeakTopics() {
        List<String> weak = new ArrayList<>();
        for (Map.Entry<String, Double> entry : topicPerformance.entrySet()) {
            if (entry.getValue() < 50.0) {
                weak.add(entry.getKey());
            }
        }
        return weak;
    }

    public List<String> getStrongTopics() {
        List<String> strong = new ArrayList<>();
        for (Map.Entry<String, Double> entry : topicPerformance.entrySet()) {
            if (entry.getValue() > 75.0) {
                strong.add(entry.getKey());
            }
        }
        return strong;
    }
}
