package model;

import java.util.Arrays;

public class Question {
    private String questionText;
    private String[] options;
    private String correctAnswer;
    private String explanation;
    private String topic;
    private String difficulty;
    private boolean mandatory;

    public Question(String questionText, String[] options, String correctAnswer,
                    String explanation, String topic, String difficulty) {
        this(questionText, options, correctAnswer, explanation, topic, difficulty, true);
    }

    public Question(String questionText, String[] options, String correctAnswer,
                    String explanation, String topic, String difficulty, boolean mandatory) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.topic = topic;
        this.difficulty = difficulty;
        this.mandatory = mandatory;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getTopic() {
        return topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public void setOptions(String[] options) {
        this.options = options;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    @Override
    public String toString() {
        return "Question{" +
                "questionText='" + questionText + '\'' +
                ", options=" + Arrays.toString(options) +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", topic='" + topic + '\'' +
                ", difficulty='" + difficulty + '\'' +
                ", mandatory=" + mandatory +
                '}';
    }
}
