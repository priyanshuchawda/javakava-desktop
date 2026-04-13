package model;

public class Attempt {
    private Question question;
    private String selectedAnswer;
    private boolean isCorrect;
    private String topic;
    private String difficulty;
    private int questionNumber;
    private int timeSpentSeconds;
    private boolean skipped;
    private boolean markedForReview;

    public Attempt(Question question, String selectedAnswer, boolean isCorrect) {
        this(question, selectedAnswer, isCorrect, 0, 0, false, false);
    }

    public Attempt(Question question, String selectedAnswer, boolean isCorrect,
                   int questionNumber, int timeSpentSeconds, boolean skipped, boolean markedForReview) {
        this.question = question;
        this.selectedAnswer = selectedAnswer;
        this.isCorrect = isCorrect;
        this.topic = question.getTopic();
        this.difficulty = question.getDifficulty();
        this.questionNumber = questionNumber;
        this.timeSpentSeconds = Math.max(0, timeSpentSeconds);
        this.skipped = skipped;
        this.markedForReview = markedForReview;
    }

    public Question getQuestion() {
        return question;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isMarkedForReview() {
        return markedForReview;
    }
}
