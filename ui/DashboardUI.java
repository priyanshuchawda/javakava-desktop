package ui;

import model.Question;
import model.Quiz;
import model.Student;
import service.GeminiService;
import service.QuizService;
import utils.FileHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardUI extends JFrame {
    private final Student student;
    private final QuizService quizService;
    private final FileHandler fileHandler;
    private final GeminiService geminiService;

    private final DefaultListModel<FileHandler.QuizFileInfo> quizListModel;
    private final JList<FileHandler.QuizFileInfo> quizList;
    private final JTextField quizSearchField;
    private final JComboBox<String> sortCombo;
    private final JTextField topicField;
    private final JSpinner questionCountSpinner;
    private final JSpinner timePerQuestionSpinner;
    private final JTextField aiTitleField;
    private final List<FileHandler.QuizFileInfo> allQuizzes;

    public DashboardUI(Student student, QuizService quizService, FileHandler fileHandler) {
        this.student = student;
        this.quizService = quizService;
        this.fileHandler = fileHandler;
        this.geminiService = new GeminiService();
        this.allQuizzes = new ArrayList<>();

        setTitle("Dashboard - " + student.getName());
        setSize(1080, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Color bg = PremiumTheme.BG_TOP;
        Color card = PremiumTheme.CARD;
        Color text = PremiumTheme.TEXT;
        Color sub = PremiumTheme.TEXT_SUB;
        Color accent = PremiumTheme.ACCENT;

        JPanel root = PremiumTheme.createGradientPanel(new BorderLayout(12, 12));
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Welcome, " + student.getName());
        title.setForeground(text);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel subtitle = new JLabel("Create tests manually or generate AI quizzes by topic with Easy/Medium/Hard mix.");
        subtitle.setForeground(sub);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 2));
        top.setBackground(bg);
        top.add(title);
        top.add(subtitle);

        JPanel left = new JPanel(new BorderLayout(8, 8));
        left.setBackground(card);
        left.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel quizListTitle = new JLabel("Available Tests");
        quizListTitle.setForeground(text);
        quizListTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        quizListModel = new DefaultListModel<>();
        quizList = new JList<>(quizListModel);
        quizList.setCellRenderer(new QuizFileCellRenderer());
        quizList.setBackground(PremiumTheme.CARD_ALT);
        quizList.setForeground(text);
        quizList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        quizList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    startSelectedQuiz();
                }
            }
        });

        quizSearchField = new JTextField();
        styleInput(quizSearchField, text);
        quizSearchField.setToolTipText("Filter tests by title, subject, class, or metadata");
        quizSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyQuizFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyQuizFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyQuizFilter();
            }
        });

        sortCombo = new JComboBox<>(new String[]{"Newest", "Title (A-Z)", "Questions (High-Low)"});
        sortCombo.setBackground(PremiumTheme.CARD_ALT);
        sortCombo.setForeground(text);
        sortCombo.setToolTipText("Choose how the test list is ordered");
        sortCombo.addActionListener(e -> applyQuizFilter());

        JScrollPane listScroll = new JScrollPane(quizList);
        listScroll.setBorder(BorderFactory.createLineBorder(PremiumTheme.BORDER, 1));

        JButton refreshBtn = createButton("Refresh", accent);
        JButton startBtn = createButton("Take Selected Test", accent);
        JButton manualBtn = createButton("Create Manual Test", accent);
        JButton folderBtn = createButton("Open Quiz Folder", accent);
        JButton resultsFolderBtn = createButton("Open Results Folder", accent);
        JButton recentBtn = createButton("My Recent Attempts", accent);
        JButton deleteBtn = createButton("Delete Selected", new Color(238, 134, 134));

        refreshBtn.setToolTipText("Reload all tests from disk");
        startBtn.setToolTipText("Start the currently selected test");
        manualBtn.setToolTipText("Open manual test builder");
        folderBtn.setToolTipText("Open data/quizzes in File Explorer");
        resultsFolderBtn.setToolTipText("Open data/results in File Explorer");
        recentBtn.setToolTipText("Show your last 5 quiz attempts");
        deleteBtn.setToolTipText("Delete selected test file permanently");

        refreshBtn.addActionListener(e -> refreshQuizList());
        startBtn.addActionListener(e -> startSelectedQuiz());
        manualBtn.addActionListener(e -> openManualBuilder());
        folderBtn.addActionListener(e -> openQuizFolder());
        resultsFolderBtn.addActionListener(e -> openResultsFolder());
        recentBtn.addActionListener(e -> showRecentAttempts());
        deleteBtn.addActionListener(e -> deleteSelectedQuiz());

        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        leftBottom.setBackground(card);
        leftBottom.add(refreshBtn);
        leftBottom.add(startBtn);
        leftBottom.add(manualBtn);
        leftBottom.add(folderBtn);
        leftBottom.add(resultsFolderBtn);
        leftBottom.add(recentBtn);
        leftBottom.add(deleteBtn);

        JPanel leftTop = new JPanel(new BorderLayout(8, 8));
        leftTop.setBackground(card);
        leftTop.add(quizListTitle, BorderLayout.NORTH);
        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setBackground(card);
        controls.add(quizSearchField, BorderLayout.CENTER);
        controls.add(sortCombo, BorderLayout.EAST);
        leftTop.add(controls, BorderLayout.SOUTH);

        left.add(leftTop, BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);
        left.add(leftBottom, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(card);
        right.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel aiTitle = new JLabel("AI Quiz Generator");
        aiTitle.setForeground(text);
        aiTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel form = new JPanel(new GridLayout(8, 1, 6, 6));
        form.setBackground(card);

        topicField = new JTextField();
        topicField.setText("Algebra");
        styleInput(topicField, text);

        questionCountSpinner = new JSpinner(new SpinnerNumberModel(9, 3, 60, 1));
        styleSpinner(questionCountSpinner, text);

        timePerQuestionSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
        styleSpinner(timePerQuestionSpinner, text);

        aiTitleField = new JTextField();
        aiTitleField.setText("");
        styleInput(aiTitleField, text);

        form.add(createLabel("Topic", text));
        form.add(topicField);
        form.add(createLabel("Total Questions (mixed Easy/Medium/Hard)", text));
        form.add(questionCountSpinner);
        form.add(createLabel("Time Per Question (seconds)", text));
        form.add(timePerQuestionSpinner);
        form.add(createLabel("Optional Test Title", text));
        form.add(aiTitleField);

        JTextArea notes = new JTextArea("Flow: Enter topic + question count, click Generate.\nThe system creates Easy/Medium/Hard MCQs, saves a quiz file, and you can start it immediately.");
        notes.setEditable(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setBackground(PremiumTheme.CARD_ALT);
        notes.setForeground(sub);
        notes.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        notes.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton generateBtn = createButton("Generate AI Test", accent);
        generateBtn.addActionListener(e -> generateAiQuiz(generateBtn));

        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBottom.setBackground(card);
        rightBottom.add(generateBtn);

        right.add(aiTitle, BorderLayout.NORTH);
        right.add(form, BorderLayout.CENTER);
        right.add(new JScrollPane(notes), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.62);
        split.setDividerSize(8);

        root.add(top, BorderLayout.NORTH);
        root.add(split, BorderLayout.CENTER);
        root.add(rightBottom, BorderLayout.SOUTH);

        setContentPane(root);
        refreshQuizList();
    }

    private void refreshQuizList() {
        allQuizzes.clear();
        try {
            List<FileHandler.QuizFileInfo> infos = fileHandler.loadAvailableQuizzes("data/quizzes");
            allQuizzes.addAll(infos);
            applyQuizFilter();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load quizzes: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyQuizFilter() {
        String filter = quizSearchField.getText() == null ? "" : quizSearchField.getText().trim().toLowerCase(Locale.ROOT);

        List<FileHandler.QuizFileInfo> filtered = new ArrayList<>();
        for (FileHandler.QuizFileInfo info : allQuizzes) {
            if (filter.isEmpty() || info.getDisplayName().toLowerCase(Locale.ROOT).contains(filter)) {
                filtered.add(info);
            }
        }

        Comparator<FileHandler.QuizFileInfo> comparator;
        String selectedSort = String.valueOf(sortCombo.getSelectedItem());
        if ("Title (A-Z)".equals(selectedSort)) {
            comparator = Comparator.comparing(FileHandler.QuizFileInfo::getTitle, String.CASE_INSENSITIVE_ORDER);
        } else if ("Questions (High-Low)".equals(selectedSort)) {
            comparator = Comparator.comparingInt(FileHandler.QuizFileInfo::getQuestionCount).reversed();
        } else {
            comparator = Comparator.comparingLong(FileHandler.QuizFileInfo::getModifiedAt).reversed();
        }
        filtered.sort(comparator);

        quizListModel.clear();
        for (FileHandler.QuizFileInfo info : filtered) {
            quizListModel.addElement(info);
        }
    }

    private void openManualBuilder() {
        QuizBuilderUI builder = new QuizBuilderUI(this, fileHandler, this::refreshQuizList);
        builder.setVisible(true);
    }

    private void startSelectedQuiz() {
        FileHandler.QuizFileInfo selected = quizList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a test first.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Quiz quiz = fileHandler.loadQuiz(selected.getFilePath());
            int maxAttempts = quiz.getMaxAttemptsPerStudent();
            if (maxAttempts > 0) {
                int attemptCount = fileHandler.countAttemptsForStudentAndTest("data/results/leaderboard.csv", student.getName(), quiz.getTitle());
                if (attemptCount >= maxAttempts) {
                    JOptionPane.showMessageDialog(this,
                            "You have reached the maximum allowed attempts for this test.",
                            "Attempt Limit Reached",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            setVisible(false);
            QuizUI quizUI = new QuizUI(student, quiz, quizService, fileHandler, () -> {
                setVisible(true);
                refreshQuizList();
            });
            quizUI.setVisible(true);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not open quiz: " + ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedQuiz() {
        FileHandler.QuizFileInfo selected = quizList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a test to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete test file?\n" + selected.getTitle(),
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(selected.getFilePath()));
            refreshQuizList();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not delete file: " + ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openQuizFolder() {
        try {
            Path quizDir = Paths.get("data/quizzes");
            if (!Files.exists(quizDir)) {
                Files.createDirectories(quizDir);
            }
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this, "Desktop integration is not supported on this system.", "Not Supported", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(quizDir.toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not open quiz folder: " + ex.getMessage(), "Open Folder Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openResultsFolder() {
        try {
            Path resultDir = Paths.get("data/results");
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(this, "Desktop integration is not supported on this system.", "Not Supported", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Desktop.getDesktop().open(resultDir.toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not open results folder: " + ex.getMessage(), "Open Folder Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRecentAttempts() {
        try {
            List<String> recent = fileHandler.loadRecentAttemptsForStudent("data/results/leaderboard.csv", student.getName(), 5);
            if (recent.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No attempts found yet for " + student.getName() + ".", "Recent Attempts", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JTextArea area = new JTextArea();
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setText(String.join("\n\n", recent));
            PremiumTheme.styleTextArea(area);

            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(720, 260));
            scroll.setBorder(BorderFactory.createLineBorder(PremiumTheme.BORDER, 1));

            JOptionPane.showMessageDialog(this, scroll, "Your Recent Attempts", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not load recent attempts: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateAiQuiz(JButton generateBtn) {
        String topic = topicField.getText().trim();
        if (topic.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Topic is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalCount = (int) questionCountSpinner.getValue();
        int timePerQuestion = (int) timePerQuestionSpinner.getValue();

        generateBtn.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            private int aiQuestionCount;

            @Override
            protected Path doInBackground() throws Exception {
                List<Question> generated = new ArrayList<>(geminiService.generateMixedDifficultyMCQs(topic, totalCount));
                generated = sanitizeQuestions(generated, topic, totalCount);
                int fillAttempts = 0;
                while (!generated.isEmpty() && generated.size() < totalCount && fillAttempts < 1) {
                    int missing = totalCount - generated.size();
                    generated.addAll(geminiService.generateMixedDifficultyMCQs(topic, missing));
                    generated = sanitizeQuestions(generated, topic, totalCount);
                    fillAttempts++;
                }

                aiQuestionCount = generated.size();
                if (generated.size() < totalCount) {
                    throw new IOException("AI returned only " + generated.size() + " of " + totalCount +
                            " questions. " + geminiService.getLastErrorMessage());
                }
                if (generated.size() > totalCount) {
                    generated = new ArrayList<>(generated.subList(0, totalCount));
                }

                for (Question q : generated) {
                    if (q.getTopic() == null || q.getTopic().isBlank()) {
                        q.setTopic(topic);
                    }
                    if (q.getDifficulty() == null || q.getDifficulty().isBlank()) {
                        q.setDifficulty("Medium");
                    }
                }

                String customTitle = aiTitleField.getText().trim();
                String title = customTitle.isEmpty() ? "AI " + topic + " Test" : customTitle;
                Set<String> tags = new HashSet<>();
                tags.add("ai-generated");
                tags.add(topic.toLowerCase(Locale.ROOT));

                Quiz quiz = new Quiz(
                        title,
                        generated,
                        true,
                        timePerQuestion,
                        topic,
                        student.getClassLevel(),
                        tags,
                        "Mixed",
                        false,
                        0,
                        false,
                        1,
                        false,
                        false,
                        0
                );

                Path quizDir = Paths.get("data/quizzes");
                if (!Files.exists(quizDir)) {
                    Files.createDirectories(quizDir);
                }

                String safeTopic = topic.replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase(Locale.ROOT);
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                Path file = quizDir.resolve("ai_" + safeTopic + "_" + stamp + ".json");

                fileHandler.saveQuiz(file.toString(), quiz);
                return file;
            }

            @Override
            protected void done() {
                generateBtn.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());

                try {
                    Path saved = get();
                    refreshQuizList();
                    String qualityNote = "\nAll questions generated by AI: " + aiQuestionCount;
                    JOptionPane.showMessageDialog(DashboardUI.this,
                            "AI test generated and saved: " + saved.getFileName() + qualityNote,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);

                    int choice = JOptionPane.showConfirmDialog(
                            DashboardUI.this,
                            "Start this test now?",
                            "Start Test",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (choice == JOptionPane.YES_OPTION) {
                        Quiz quiz = fileHandler.loadQuiz(saved.toString());
                        setVisible(false);
                        QuizUI quizUI = new QuizUI(student, quiz, quizService, fileHandler, () -> {
                            setVisible(true);
                            refreshQuizList();
                        });
                        quizUI.setVisible(true);
                    }
                } catch (Exception ex) {
                    String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    if (message == null || message.isBlank()) {
                        message = "Failed to generate quiz.";
                    }
                    JOptionPane.showMessageDialog(DashboardUI.this, message, "AI Generation Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private List<Question> sanitizeQuestions(List<Question> input, String defaultTopic, int maxCount) {
        Map<String, Question> unique = new LinkedHashMap<>();
        if (input == null) {
            return new ArrayList<>();
        }

        for (Question q : input) {
            if (q == null || !isUsableQuestion(q)) {
                continue;
            }

            String[] options = normalizeOptions(q.getOptions());
            String correctAnswer = q.getCorrectAnswer() == null ? "" : q.getCorrectAnswer().trim();
            int matchIndex = findOptionIndex(options, correctAnswer);
            if (matchIndex < 0) {
                matchIndex = 0;
                correctAnswer = options[0];
            } else {
                correctAnswer = options[matchIndex];
            }

            String topic = q.getTopic() == null || q.getTopic().isBlank() ? defaultTopic : q.getTopic().trim();
            String difficulty = q.getDifficulty() == null || q.getDifficulty().isBlank() ? "Medium" : q.getDifficulty().trim();
            String questionText = q.getQuestionText().trim();
            String explanation = q.getExplanation() == null ? "" : q.getExplanation().trim();

            String key = questionText.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            unique.putIfAbsent(key, new Question(questionText, options, correctAnswer, explanation, topic, difficulty, true));

            if (unique.size() >= maxCount) {
                break;
            }
        }

        return new ArrayList<>(unique.values());
    }

    private boolean isUsableQuestion(Question q) {
        if (q.getQuestionText() == null || q.getQuestionText().trim().length() < 12) {
            return false;
        }
        String[] options = q.getOptions();
        if (options == null || options.length != 4) {
            return false;
        }
        for (String option : options) {
            if (option == null || option.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String[] normalizeOptions(String[] rawOptions) {
        String[] normalized = new String[4];
        for (int i = 0; i < 4; i++) {
            String value = rawOptions[i] == null ? "" : rawOptions[i].trim();
            value = value.replaceFirst("^[A-Da-d][\\).:-]\\s*", "").trim();
            normalized[i] = value.isEmpty() ? ("Option " + (i + 1)) : value;
        }
        return normalized;
    }

    private int findOptionIndex(String[] options, String answer) {
        if (answer == null) {
            return -1;
        }
        String normalized = answer.trim();
        normalized = normalized.replaceFirst("^[A-Da-d][\\).:-]\\s*", "").trim();

        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(normalized)) {
                return i;
            }
        }

        if (normalized.length() == 1) {
            char c = Character.toUpperCase(normalized.charAt(0));
            if (c >= 'A' && c <= 'D') {
                return c - 'A';
            }
        }

        return -1;
    }

    private JButton createButton(String text, Color accent) {
        JButton button = new JButton(text);
        if (accent.getRed() > 220 && accent.getGreen() < 170) {
            PremiumTheme.styleDangerButton(button);
        } else {
            PremiumTheme.stylePrimaryButton(button);
        }
        return button;
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private void styleInput(JTextField field, Color text) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        PremiumTheme.styleTextField(field);
    }

    private void styleSpinner(JSpinner spinner, Color text) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        PremiumTheme.styleSpinner(spinner);
    }

    private static class QuizFileCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof FileHandler.QuizFileInfo info) {
                label.setText(info.getDisplayName());
            }
            label.setBorder(new EmptyBorder(8, 8, 8, 8));
            return label;
        }
    }
}
