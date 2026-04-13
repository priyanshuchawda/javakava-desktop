package service;

import model.Attempt;
import model.Question;
import model.Quiz;
import model.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizService {
    private final EvaluationService evaluationService;

    private Quiz currentQuiz;
    private List<Attempt> attempts;
    private int currentIndex;

    private final Map<String, Integer> correctCount;
    private final Map<String, Integer> totalCount;

    public QuizService(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
        this.attempts = new ArrayList<>();
        this.currentIndex = 0;
        this.correctCount = new HashMap<>();
        this.totalCount = new HashMap<>();
    }

    public void startQuiz(Quiz quiz) {
        if (quiz != null && (quiz.isRandomizeQuestionOrder() || quiz.isRandomizeOptionOrder())) {
            List<Question> prepared = new ArrayList<>();
            for (Question original : quiz.getQuestions()) {
                Question copied = copyQuestionWithOptionalOptionShuffle(original, quiz.isRandomizeOptionOrder());
                if (copied != null) {
                    prepared.add(copied);
                }
            }

            if (quiz.isRandomizeQuestionOrder()) {
                Collections.shuffle(prepared);
            }

            Quiz randomized = new Quiz(
                    quiz.getTitle(),
                    prepared,
                    quiz.isNegativeMarkingEnabled(),
                    quiz.getTimePerQuestionSeconds(),
                    quiz.getSubject(),
                    quiz.getClassLevel(),
                    quiz.getTags(),
                    quiz.getDifficultyProfile(),
                    quiz.isFullTestTimerMode(),
                    quiz.getFullTestTimeSeconds(),
                    quiz.isPublished(),
                    quiz.getVersion(),
                    quiz.isRandomizeQuestionOrder(),
                    quiz.isRandomizeOptionOrder(),
                    quiz.getMaxAttemptsPerStudent()
            );
            this.currentQuiz = randomized;
        } else {
            this.currentQuiz = quiz;
        }
        this.attempts = new ArrayList<>();
        this.currentIndex = 0;
        this.correctCount.clear();
        this.totalCount.clear();
    }

    public Question getCurrentQuestion() {
        if (currentQuiz == null || currentIndex >= currentQuiz.getQuestions().size()) {
            return null;
        }
        return currentQuiz.getQuestions().get(currentIndex);
    }

    public void submitAnswer(String selectedAnswer) {
        submitDetailedAnswer(selectedAnswer, 0, false, false);
    }

    public void submitDetailedAnswer(String selectedAnswer, int timeSpentSeconds, boolean skipped, boolean markedForReview) {
        Question question = getCurrentQuestion();
        if (question == null) {
            return;
        }

        boolean isCorrect = question.getCorrectAnswer().equalsIgnoreCase(selectedAnswer);
        Attempt attempt = new Attempt(question, selectedAnswer, isCorrect, currentIndex + 1, timeSpentSeconds, skipped, markedForReview);
        attempts.add(attempt);

        String topic = question.getTopic();
        totalCount.put(topic, totalCount.getOrDefault(topic, 0) + 1);
        if (isCorrect) {
            correctCount.put(topic, correctCount.getOrDefault(topic, 0) + 1);
        }

        currentIndex++;
    }

    public boolean hasNextQuestion() {
        return currentQuiz != null && currentIndex < currentQuiz.getQuestions().size();
    }

    public int getCurrentQuestionNumber() {
        return currentIndex + 1;
    }

    public int getTotalQuestions() {
        if (currentQuiz == null) {
            return 0;
        }
        return currentQuiz.getQuestions().size();
    }

    public Result finishQuiz() {
        if (currentQuiz == null) {
            throw new IllegalStateException("Quiz is not started.");
        }
        return evaluationService.evaluate(attempts, currentQuiz.isNegativeMarkingEnabled(), currentQuiz.getTitle());
    }

    public Quiz getCurrentQuiz() {
        return currentQuiz;
    }

    public List<Attempt> getAttempts() {
        return new ArrayList<>(attempts);
    }

    public Map<String, Integer> getCorrectCount() {
        return new HashMap<>(correctCount);
    }

    public Map<String, Integer> getTotalCount() {
        return new HashMap<>(totalCount);
    }

    public List<String> getWeakTopics() {
        List<String> weak = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : totalCount.entrySet()) {
            String topic = entry.getKey();
            int total = entry.getValue();
            int correct = correctCount.getOrDefault(topic, 0);
            double accuracy = total == 0 ? 0.0 : (correct * 100.0) / total;
            if (accuracy < 50.0) {
                weak.add(topic);
            }
        }
        return weak;
    }

    public List<String> getStrongTopics() {
        List<String> strong = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : totalCount.entrySet()) {
            String topic = entry.getKey();
            int total = entry.getValue();
            int correct = correctCount.getOrDefault(topic, 0);
            double accuracy = total == 0 ? 0.0 : (correct * 100.0) / total;
            if (accuracy > 75.0) {
                strong.add(topic);
            }
        }
        return strong;
    }

    private Question copyQuestionWithOptionalOptionShuffle(Question question, boolean shuffleOptions) {
        if (question == null) {
            return null;
        }

        String[] options = question.getOptions();
        if (options == null || options.length == 0) {
            return new Question(
                    question.getQuestionText(),
                    new String[]{"Option 1", "Option 2", "Option 3", "Option 4"},
                    question.getCorrectAnswer(),
                    question.getExplanation(),
                    question.getTopic(),
                    question.getDifficulty(),
                    question.isMandatory()
            );
        }

        String[] copiedOptions = new String[options.length];
        System.arraycopy(options, 0, copiedOptions, 0, options.length);

        if (shuffleOptions && copiedOptions.length == 4) {
            List<String> shuffledOptions = new ArrayList<>();
            Collections.addAll(shuffledOptions, copiedOptions);
            Collections.shuffle(shuffledOptions);
            copiedOptions = shuffledOptions.toArray(new String[0]);
        }

        String correctAnswer = question.getCorrectAnswer();
        if (shuffleOptions && copiedOptions.length > 0) {
            for (String option : copiedOptions) {
                if (option != null && option.equalsIgnoreCase(correctAnswer)) {
                    correctAnswer = option;
                    break;
                }
            }
            if (correctAnswer == null || correctAnswer.isBlank()) {
                correctAnswer = copiedOptions[0];
            }
        }

        return new Question(
                question.getQuestionText(),
                copiedOptions,
                correctAnswer,
                question.getExplanation(),
                question.getTopic(),
                question.getDifficulty(),
                question.isMandatory()
        );
    }

}
