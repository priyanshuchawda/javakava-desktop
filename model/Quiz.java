package model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Quiz {
    private String title;
    private List<Question> questions;
    private boolean negativeMarkingEnabled;
    private int timePerQuestionSeconds;
    private String subject;
    private String classLevel;
    private Set<String> tags;
    private String difficultyProfile;
    private boolean fullTestTimerMode;
    private int fullTestTimeSeconds;
    private boolean published;
    private int version;
    private boolean randomizeQuestionOrder;
    private boolean randomizeOptionOrder;
    private int maxAttemptsPerStudent;

    public Quiz(String title, List<Question> questions, boolean negativeMarkingEnabled) {
        this(title, questions, negativeMarkingEnabled, 30,
                "General", "General", new HashSet<>(), "Mixed",
                false, 0, false, 1, false, false, 0);
    }

    public Quiz(String title, List<Question> questions, boolean negativeMarkingEnabled, int timePerQuestionSeconds) {
        this(title, questions, negativeMarkingEnabled, timePerQuestionSeconds,
                "General", "General", new HashSet<>(), "Mixed",
                false, 0, false, 1, false, false, 0);
    }

    public Quiz(String title,
                List<Question> questions,
                boolean negativeMarkingEnabled,
                int timePerQuestionSeconds,
                String subject,
                String classLevel,
                Set<String> tags,
                String difficultyProfile,
                boolean fullTestTimerMode,
                int fullTestTimeSeconds,
                boolean published,
                int version,
                boolean randomizeQuestionOrder,
                boolean randomizeOptionOrder,
                int maxAttemptsPerStudent) {
        this.title = title;
        this.questions = new ArrayList<>(questions);
        this.negativeMarkingEnabled = negativeMarkingEnabled;
        this.timePerQuestionSeconds = Math.max(5, timePerQuestionSeconds);
        this.subject = subject == null || subject.isBlank() ? "General" : subject;
        this.classLevel = classLevel == null || classLevel.isBlank() ? "General" : classLevel;
        this.tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
        this.difficultyProfile = difficultyProfile == null || difficultyProfile.isBlank() ? "Mixed" : difficultyProfile;
        this.fullTestTimerMode = fullTestTimerMode;
        this.fullTestTimeSeconds = Math.max(0, fullTestTimeSeconds);
        this.published = published;
        this.version = Math.max(1, version);
        this.randomizeQuestionOrder = randomizeQuestionOrder;
        this.randomizeOptionOrder = randomizeOptionOrder;
        this.maxAttemptsPerStudent = Math.max(0, maxAttemptsPerStudent);
    }

    public String getTitle() {
        return title;
    }

    public List<Question> getQuestions() {
        return new ArrayList<>(questions);
    }

    public boolean isNegativeMarkingEnabled() {
        return negativeMarkingEnabled;
    }

    public int getTimePerQuestionSeconds() {
        return timePerQuestionSeconds;
    }

    public String getSubject() {
        return subject;
    }

    public String getClassLevel() {
        return classLevel;
    }

    public Set<String> getTags() {
        return new HashSet<>(tags);
    }

    public String getDifficultyProfile() {
        return difficultyProfile;
    }

    public boolean isFullTestTimerMode() {
        return fullTestTimerMode;
    }

    public int getFullTestTimeSeconds() {
        return fullTestTimeSeconds;
    }

    public boolean isPublished() {
        return published;
    }

    public int getVersion() {
        return version;
    }

    public boolean isRandomizeQuestionOrder() {
        return randomizeQuestionOrder;
    }

    public boolean isRandomizeOptionOrder() {
        return randomizeOptionOrder;
    }

    public int getMaxAttemptsPerStudent() {
        return maxAttemptsPerStudent;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = new ArrayList<>(questions);
    }

    public void setNegativeMarkingEnabled(boolean negativeMarkingEnabled) {
        this.negativeMarkingEnabled = negativeMarkingEnabled;
    }

    public void setTimePerQuestionSeconds(int timePerQuestionSeconds) {
        this.timePerQuestionSeconds = Math.max(5, timePerQuestionSeconds);
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setClassLevel(String classLevel) {
        this.classLevel = classLevel;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags == null ? new HashSet<>() : new HashSet<>(tags);
    }

    public void setDifficultyProfile(String difficultyProfile) {
        this.difficultyProfile = difficultyProfile;
    }

    public void setFullTestTimerMode(boolean fullTestTimerMode) {
        this.fullTestTimerMode = fullTestTimerMode;
    }

    public void setFullTestTimeSeconds(int fullTestTimeSeconds) {
        this.fullTestTimeSeconds = Math.max(0, fullTestTimeSeconds);
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setVersion(int version) {
        this.version = Math.max(1, version);
    }

    public void setRandomizeQuestionOrder(boolean randomizeQuestionOrder) {
        this.randomizeQuestionOrder = randomizeQuestionOrder;
    }

    public void setRandomizeOptionOrder(boolean randomizeOptionOrder) {
        this.randomizeOptionOrder = randomizeOptionOrder;
    }

    public void setMaxAttemptsPerStudent(int maxAttemptsPerStudent) {
        this.maxAttemptsPerStudent = Math.max(0, maxAttemptsPerStudent);
    }
}
