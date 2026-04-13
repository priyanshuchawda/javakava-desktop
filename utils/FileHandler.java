package utils;

import model.Question;
import model.Quiz;
import model.Result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileHandler {
    private final JSONParser jsonParser;

    public FileHandler() {
        this.jsonParser = new JSONParser();
    }

    public List<Question> loadQuestionsFromJson(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new IOException("Question file not found: " + path);
        }
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        return jsonParser.parseQuestions(content);
    }

    public void saveQuestionsToJson(String path, List<Question> questions) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        Files.writeString(filePath, jsonParser.questionsToJson(questions), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void saveQuiz(String path, Quiz quiz) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        Files.writeString(filePath, jsonParser.quizToJson(quiz), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Quiz loadQuiz(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new IOException("Quiz file not found: " + path);
        }
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        return jsonParser.parseQuiz(content);
    }

    public List<QuizFileInfo> loadAvailableQuizzes(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            return new ArrayList<>();
        }

        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .forEach(files::add);
        }

        files.sort(Comparator.comparing((Path p) -> p.toFile().lastModified()).reversed());

        List<QuizFileInfo> infos = new ArrayList<>();
        for (Path file : files) {
            try {
                Quiz quiz = loadQuiz(file.toString());
                String timerLabel = quiz.isFullTestTimerMode()
                        ? ("Full " + quiz.getFullTestTimeSeconds() + "s")
                        : (quiz.getTimePerQuestionSeconds() + "s/q");
                String displayName = quiz.getTitle()
                        + " | " + quiz.getSubject()
                        + " | " + quiz.getClassLevel()
                        + " | V" + quiz.getVersion()
                        + " | Q:" + quiz.getQuestions().size()
                        + " | " + timerLabel;
                infos.add(new QuizFileInfo(
                    displayName,
                    file.toString(),
                    quiz.getTitle(),
                    quiz.getSubject(),
                    quiz.getClassLevel(),
                    quiz.getQuestions().size(),
                    file.toFile().lastModified()
                ));
            } catch (Exception ignored) {
                // Ignore invalid quiz files while listing available tests.
            }
        }

        return infos;
    }

    public void saveResultToJson(String path, Result result, String studentName) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        Files.writeString(filePath, jsonParser.resultToJson(result, studentName), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void appendResultToCsv(String path, Result result, String studentName, String classLevel, String rollNumber) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        boolean exists = Files.exists(filePath);

        StringBuilder sb = new StringBuilder();
        if (!exists) {
            sb.append("timestamp,student,class,roll,quizTitle,score,totalQuestions,accuracy,weakTopics,strongTopics\n");
        }

        List<String> weakTopics = new ArrayList<>();
        List<String> strongTopics = new ArrayList<>();
        for (Map.Entry<String, Double> entry : result.getTopicPerformance().entrySet()) {
            if (entry.getValue() < 50.0) {
                weakTopics.add(entry.getKey());
            }
            if (entry.getValue() > 75.0) {
                strongTopics.add(entry.getKey());
            }
        }

        sb.append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append(",")
                .append(sanitizeCsv(studentName)).append(",")
                .append(sanitizeCsv(classLevel)).append(",")
                .append(sanitizeCsv(rollNumber)).append(",")
                .append(sanitizeCsv(result.getQuizTitle())).append(",")
            .append(String.format(java.util.Locale.US, "%.2f", result.getScore())).append(",")
                .append(result.getTotalQuestions()).append(",")
                .append(String.format(java.util.Locale.US, "%.2f", result.getAccuracy())).append(",")
                .append(sanitizeCsv(String.join("|", weakTopics))).append(",")
                .append(sanitizeCsv(String.join("|", strongTopics))).append("\n");

        Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public List<String> loadNotesFromTxt(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new IOException("Notes file not found: " + path);
        }
        return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    }

    public List<String> loadLeaderboard(String path, int topN) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<String> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            rows.add(lines.get(i));
        }

        rows.sort((a, b) -> {
            double scoreA = parseScore(a);
            double scoreB = parseScore(b);
            return Double.compare(scoreB, scoreA);
        });

        List<String> topRows = rows.size() > topN ? new ArrayList<>(rows.subList(0, topN)) : rows;
        List<String> formatted = new ArrayList<>();
        int rank = 1;
        for (String row : topRows) {
            LeaderboardRow parsed = parseLeaderboardRow(row);
            if (parsed == null) {
                continue;
            }
            formatted.add(rank + ". " + parsed.student + " -> " + parsed.quizTitle + " -> Score " + parsed.score + "/" + parsed.totalQuestions + " | Accuracy " + parsed.accuracy + "%");
            rank++;
        }
        return formatted;
    }

    public List<String> loadRecentAttemptsForStudent(String path, String studentName, int limit) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<String> filtered = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 1; i--) {
            LeaderboardRow parsed = parseLeaderboardRow(lines.get(i));
            if (parsed == null) {
                continue;
            }
            if (!parsed.student.equalsIgnoreCase(studentName)) {
                continue;
            }
            String timestamp = parsed.timestamp.replace('T', ' ');
            filtered.add(timestamp + " -> " + parsed.quizTitle + " -> Score " + parsed.score + "/" + parsed.totalQuestions + ", Accuracy: " + parsed.accuracy + "%");
            if (filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    public List<String> loadRecentAttemptsForStudentAndTest(String path, String studentName, String quizTitle, int limit) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<String> filtered = new ArrayList<>();
        for (int i = lines.size() - 1; i >= 1; i--) {
            LeaderboardRow parsed = parseLeaderboardRow(lines.get(i));
            if (parsed == null) {
                continue;
            }
            if (!parsed.student.equalsIgnoreCase(studentName)) {
                continue;
            }
            if (!parsed.quizTitle.equalsIgnoreCase(quizTitle)) {
                continue;
            }
            filtered.add(lines.get(i));
            if (filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    public int countAttemptsForStudentAndTest(String path, String studentName, String quizTitle) throws IOException {
        return loadRecentAttemptsForStudentAndTest(path, studentName, quizTitle, Integer.MAX_VALUE).size();
    }

    public void duplicateQuiz(String sourcePath, String targetPath, String newTitle) throws IOException {
        Quiz quiz = loadQuiz(sourcePath);
        quiz.setTitle(newTitle);
        quiz.setPublished(false);
        quiz.setVersion(Math.max(1, quiz.getVersion()));
        saveQuiz(targetPath, quiz);
    }

    public void exportQuizPackAsCsv(String path, Quiz quiz) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);

        StringBuilder sb = new StringBuilder();
        sb.append("question,option1,option2,option3,option4,correctAnswer,topic,difficulty,mandatory,explanation\n");
        for (Question q : quiz.getQuestions()) {
            String[] options = q.getOptions();
            sb.append(sanitizeCsv(q.getQuestionText())).append(",")
                    .append(sanitizeCsv(options[0])).append(",")
                    .append(sanitizeCsv(options[1])).append(",")
                    .append(sanitizeCsv(options[2])).append(",")
                    .append(sanitizeCsv(options[3])).append(",")
                    .append(sanitizeCsv(q.getCorrectAnswer())).append(",")
                    .append(sanitizeCsv(q.getTopic())).append(",")
                    .append(sanitizeCsv(q.getDifficulty())).append(",")
                    .append(q.isMandatory()).append(",")
                    .append(sanitizeCsv(q.getExplanation())).append("\n");
        }

        Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<Question> importQuestionsFromCsv(String path) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new IOException("CSV file not found: " + path);
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<Question> questions = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> cols = parseCsvRow(lines.get(i));
            if (cols.size() < 10) {
                continue;
            }
            String[] options = new String[]{cols.get(1), cols.get(2), cols.get(3), cols.get(4)};
            boolean mandatory = "true".equalsIgnoreCase(cols.get(8));
            questions.add(new Question(cols.get(0), options, cols.get(5), cols.get(9), cols.get(6), cols.get(7), mandatory));
        }
        return questions;
    }

    public void appendWrongAnswerNotebook(String path, String studentName, Result result) throws IOException {
        Path filePath = Paths.get(path);
        ensureParentDirectory(filePath);
        StringBuilder sb = new StringBuilder();
        for (var attempt : result.getAttempts()) {
            if (attempt.isCorrect()) {
                continue;
            }
            sb.append(result.getAttemptedAt()).append(" | ")
                    .append(sanitizeCsv(studentName)).append(" | ")
                    .append(sanitizeCsv(result.getQuizTitle())).append(" | ")
                    .append(sanitizeCsv(attempt.getQuestion().getTopic())).append(" | Q: ")
                    .append(attempt.getQuestion().getQuestionText().replace("\n", " ")).append(" | Your: ")
                    .append(attempt.getSelectedAnswer()).append(" | Correct: ")
                    .append(attempt.getQuestion().getCorrectAnswer()).append("\n");
        }
        if (!sb.isEmpty()) {
            Files.writeString(filePath, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    public List<String> loadWrongAnswerNotebook(String path, String studentName) throws IOException {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(filePath, StandardCharsets.UTF_8).stream()
                .filter(line -> line.contains("| " + studentName + " |"))
                .collect(Collectors.toList());
    }

    public void exportAllAttemptsCsv(String sourcePath, String targetPath) throws IOException {
        Path src = Paths.get(sourcePath);
        if (!Files.exists(src)) {
            throw new IOException("Leaderboard source not found: " + sourcePath);
        }
        Path target = Paths.get(targetPath);
        ensureParentDirectory(target);
        Files.copy(src, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public void backupFolder(String sourceDir, String backupDir) throws IOException {
        Path source = Paths.get(sourceDir);
        Path target = Paths.get(backupDir);
        if (!Files.exists(source)) {
            return;
        }
        Files.createDirectories(target);
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    ensureParentDirectory(destination);
                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public void saveDraftQuiz(String path, Quiz quiz) throws IOException {
        saveQuiz(path, quiz);
    }

    public Quiz loadDraftQuiz(String path) throws IOException {
        return loadQuiz(path);
    }

    private double parseScore(String row) {
        LeaderboardRow parsed = parseLeaderboardRow(row);
        if (parsed == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(parsed.score);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private LeaderboardRow parseLeaderboardRow(String row) {
        List<String> cols = parseCsvRow(row);
        if (cols.size() >= 10) {
            return new LeaderboardRow(
                    safeGet(cols, 0),
                    safeGet(cols, 1),
                    safeGet(cols, 4),
                    safeGet(cols, 5),
                    safeGet(cols, 6),
                    safeGet(cols, 7)
            );
        }
        if (cols.size() >= 7) {
            return new LeaderboardRow(
                    safeGet(cols, 0),
                    safeGet(cols, 1),
                    "Untitled Test",
                    safeGet(cols, 2),
                    safeGet(cols, 3),
                    safeGet(cols, 4)
            );
        }
        return null;
    }

    private String safeGet(List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private String sanitizeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<String> parseCsvRow(String row) {
        List<String> parts = new ArrayList<>();
        if (row == null || row.isEmpty()) {
            return parts;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    private static class LeaderboardRow {
        private final String timestamp;
        private final String student;
        private final String quizTitle;
        private final String score;
        private final String totalQuestions;
        private final String accuracy;

        private LeaderboardRow(String timestamp, String student, String quizTitle, String score, String totalQuestions, String accuracy) {
            this.timestamp = timestamp;
            this.student = student;
            this.quizTitle = quizTitle;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.accuracy = accuracy;
        }
    }

    private void ensureParentDirectory(Path filePath) throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    public static class QuizFileInfo {
        private final String displayName;
        private final String filePath;
        private final String title;
        private final String subject;
        private final String classLevel;
        private final int questionCount;
        private final long modifiedAt;

        public QuizFileInfo(String displayName, String filePath, String title) {
            this(displayName, filePath, title, "General", "General", 0, 0L);
        }

        public QuizFileInfo(String displayName, String filePath, String title, String subject, String classLevel, int questionCount, long modifiedAt) {
            this.displayName = displayName;
            this.filePath = filePath;
            this.title = title;
            this.subject = subject;
            this.classLevel = classLevel;
            this.questionCount = questionCount;
            this.modifiedAt = modifiedAt;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getTitle() {
            return title;
        }

        public String getSubject() {
            return subject;
        }

        public String getClassLevel() {
            return classLevel;
        }

        public int getQuestionCount() {
            return questionCount;
        }

        public long getModifiedAt() {
            return modifiedAt;
        }
    }
}
