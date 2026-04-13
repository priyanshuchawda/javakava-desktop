package ui;

import model.Question;
import model.Quiz;
import utils.FileHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class QuizBuilderUI extends JDialog {
    private final FileHandler fileHandler;
    private final Runnable onSaved;
    private final String editingFilePath;

    private final JTextField testNameField;
    private final JSpinner questionCountSpinner;
    private final JSpinner timePerQuestionSpinner;
    private final JCheckBox negativeMarkingCheck;

    private final JTextArea questionTextArea;
    private final JTextField[] optionFields;
    private final JComboBox<String> correctOptionCombo;
    private final JTextField topicField;
    private final JComboBox<String> difficultyCombo;
    private final DefaultListModel<String> questionListModel;
    private final JList<String> questionList;

    private final List<Question> createdQuestions;
    private int currentEditIndex;

    public QuizBuilderUI(JFrame owner, FileHandler fileHandler, Runnable onSaved) {
        this(owner, fileHandler, onSaved, null, null);
    }

    public QuizBuilderUI(JFrame owner, FileHandler fileHandler, Runnable onSaved, Quiz existingQuiz, String existingFilePath) {
        super(owner, existingQuiz == null ? "Create New Quiz" : "Edit Test", true);
        this.fileHandler = fileHandler;
        this.onSaved = onSaved;
        this.editingFilePath = existingFilePath;
        this.createdQuestions = new ArrayList<>();
        this.currentEditIndex = -1;

        setSize(900, 760);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        Color bg = PremiumTheme.BG_TOP;
        Color card = PremiumTheme.CARD;
        Color text = PremiumTheme.TEXT;
        Color accent = PremiumTheme.ACCENT;

        JPanel root = PremiumTheme.createGradientPanel(new BorderLayout(12, 12));
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new GridLayout(2, 4, 10, 8));
        top.setBackground(bg);

        testNameField = new JTextField();
        questionCountSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        timePerQuestionSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 600, 5));
        negativeMarkingCheck = new JCheckBox("Enable -0.25 negative marking", true);

        styleInput(testNameField, card, text);
        styleSpinner(questionCountSpinner, card, text);
        styleSpinner(timePerQuestionSpinner, card, text);
        negativeMarkingCheck.setBackground(bg);
        negativeMarkingCheck.setForeground(text);

        top.add(createLabel("Test Name", text));
        top.add(createLabel("Total Questions", text));
        top.add(createLabel("Time per Question (s)", text));
        top.add(new JLabel(""));
        top.add(testNameField);
        top.add(questionCountSpinner);
        top.add(timePerQuestionSpinner);
        top.add(negativeMarkingCheck);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.setBackground(bg);

        JPanel questionEditor = new JPanel(new BorderLayout(10, 10));
        questionEditor.setBackground(card);
        questionEditor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        questionTextArea = new JTextArea(4, 20);
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        questionTextArea.setBackground(PremiumTheme.CARD_ALT);
        questionTextArea.setForeground(text);
        questionTextArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        optionFields = new JTextField[4];
        JPanel optionsPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        optionsPanel.setBackground(card);

        for (int i = 0; i < 4; i++) {
            optionFields[i] = new JTextField();
            styleInput(optionFields[i], new Color(24, 30, 37), text);
            optionsPanel.add(createLabel("Option " + (i + 1), text));
            optionsPanel.add(optionFields[i]);
        }

        correctOptionCombo = new JComboBox<>(new String[]{"Option 1", "Option 2", "Option 3", "Option 4"});
        correctOptionCombo.setBackground(PremiumTheme.CARD_ALT);
        correctOptionCombo.setForeground(text);

        topicField = new JTextField("General");
        styleInput(topicField, new Color(24, 30, 37), text);

        difficultyCombo = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        difficultyCombo.setBackground(PremiumTheme.CARD_ALT);
        difficultyCombo.setForeground(text);

        JPanel metaPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        metaPanel.setBackground(card);
        metaPanel.add(createLabel("Correct Answer", text));
        metaPanel.add(correctOptionCombo);
        metaPanel.add(createLabel("Topic", text));
        metaPanel.add(topicField);

        JPanel difficultyPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        difficultyPanel.setBackground(card);
        difficultyPanel.add(createLabel("Difficulty", text));
        difficultyPanel.add(difficultyCombo);

        JPanel editorBody = new JPanel(new BorderLayout(8, 8));
        editorBody.setBackground(card);
        editorBody.add(new JScrollPane(questionTextArea), BorderLayout.NORTH);
        editorBody.add(optionsPanel, BorderLayout.CENTER);
        editorBody.add(metaPanel, BorderLayout.SOUTH);

        questionEditor.add(createLabel("Question Builder", text), BorderLayout.NORTH);
        questionEditor.add(editorBody, BorderLayout.CENTER);
        questionEditor.add(difficultyPanel, BorderLayout.SOUTH);

        JPanel createdPanel = new JPanel(new BorderLayout(8, 8));
        createdPanel.setBackground(card);
        createdPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        questionListModel = new DefaultListModel<>();
        questionList = new JList<>(questionListModel);
        questionList.setBackground(PremiumTheme.CARD_ALT);
        questionList.setForeground(text);
        questionList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedQuestionForEdit();
            }
        });

        createdPanel.add(createLabel("Created Questions", text), BorderLayout.NORTH);
        createdPanel.add(new JScrollPane(questionList), BorderLayout.CENTER);

        center.add(questionEditor);
        center.add(createdPanel);

        JButton addQuestionBtn = createButton("Add Question", accent);
        JButton importCsvBtn = createButton("Import CSV", accent);
        JButton removeQuestionBtn = createButton("Remove Selected", accent);
        JButton clearEditorBtn = createButton("Clear Editor", accent);
        JButton saveQuizBtn = createButton("Save Test", accent);

        addQuestionBtn.addActionListener(e -> addQuestion());
        importCsvBtn.addActionListener(e -> importQuestionsFromCsv());
        removeQuestionBtn.addActionListener(e -> removeSelectedQuestion());
        clearEditorBtn.addActionListener(e -> clearEditor());
        saveQuizBtn.addActionListener(e -> saveQuiz());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(bg);
        bottom.add(addQuestionBtn);
        bottom.add(importCsvBtn);
        bottom.add(removeQuestionBtn);
        bottom.add(clearEditorBtn);
        bottom.add(saveQuizBtn);

        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        if (existingQuiz != null) {
            populateFromQuiz(existingQuiz);
        }
    }

    private void addQuestion() {
        int totalRequired = (int) questionCountSpinner.getValue();
        if (currentEditIndex < 0 && createdQuestions.size() >= totalRequired) {
            JOptionPane.showMessageDialog(this, "You already added all required questions.", "Limit Reached", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String qText = questionTextArea.getText().trim();
        if (qText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Question text is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = new String[4];
        for (int i = 0; i < 4; i++) {
            String value = optionFields[i].getText().trim();
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All 4 options are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            options[i] = value;
        }

        int correctIndex = correctOptionCombo.getSelectedIndex();
        String correct = options[correctIndex];
        String topic = topicField.getText().trim().isEmpty() ? "General" : topicField.getText().trim();
        String difficulty = String.valueOf(difficultyCombo.getSelectedItem());

        Question question = new Question(qText, options, correct, "", topic, difficulty);
        if (currentEditIndex >= 0) {
            createdQuestions.set(currentEditIndex, question);
        } else {
            createdQuestions.add(question);
        }
        refreshQuestionListModel();
        clearEditor();
    }

    private void removeSelectedQuestion() {
        int index = questionList.getSelectedIndex();
        if (index < 0 || index >= createdQuestions.size()) {
            JOptionPane.showMessageDialog(this, "Select a question to remove.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        createdQuestions.remove(index);
        refreshQuestionListModel();
        clearEditor();
    }

    private void loadSelectedQuestionForEdit() {
        int index = questionList.getSelectedIndex();
        if (index < 0 || index >= createdQuestions.size()) {
            return;
        }

        Question q = createdQuestions.get(index);
        questionTextArea.setText(q.getQuestionText());
        String[] options = q.getOptions();
        for (int i = 0; i < 4; i++) {
            optionFields[i].setText(options[i]);
        }

        int correctIndex = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(q.getCorrectAnswer())) {
                correctIndex = i;
                break;
            }
        }
        correctOptionCombo.setSelectedIndex(correctIndex);
        topicField.setText(q.getTopic());
        difficultyCombo.setSelectedItem(q.getDifficulty());
        currentEditIndex = index;
    }

    private void clearEditor() {
        currentEditIndex = -1;
        questionList.clearSelection();
        questionTextArea.setText("");
        for (JTextField field : optionFields) {
            field.setText("");
        }
        correctOptionCombo.setSelectedIndex(0);
        topicField.setText("General");
        difficultyCombo.setSelectedIndex(0);
    }

    private void refreshQuestionListModel() {
        questionListModel.clear();
        for (int i = 0; i < createdQuestions.size(); i++) {
            questionListModel.addElement("Q" + (i + 1) + ": " + createdQuestions.get(i).getQuestionText());
        }
    }

    private void populateFromQuiz(Quiz quiz) {
        testNameField.setText(quiz.getTitle());
        questionCountSpinner.setValue(quiz.getQuestions().size());
        timePerQuestionSpinner.setValue(quiz.getTimePerQuestionSeconds());
        negativeMarkingCheck.setSelected(quiz.isNegativeMarkingEnabled());
        createdQuestions.clear();
        createdQuestions.addAll(quiz.getQuestions());
        refreshQuestionListModel();
    }

    private void saveQuiz() {
        String testName = testNameField.getText().trim();
        if (testName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Test name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int totalRequired = (int) questionCountSpinner.getValue();
        if (createdQuestions.size() != totalRequired) {
            JOptionPane.showMessageDialog(this,
                    "You must add exactly " + totalRequired + " questions before saving.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int seconds = (int) timePerQuestionSpinner.getValue();
        boolean negative = negativeMarkingCheck.isSelected();

        Quiz quiz = new Quiz(testName, createdQuestions, negative, seconds);

        try {
            Path dir = Paths.get("data/quizzes");
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            Path file;
            if (editingFilePath != null && !editingFilePath.isBlank()) {
                file = Paths.get(editingFilePath);
            } else {
                String sanitized = testName.replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase();
                file = dir.resolve(sanitized + ".json");
                if (Files.exists(file)) {
                    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                    file = dir.resolve(sanitized + "_" + stamp + ".json");
                }
            }

            fileHandler.saveQuiz(file.toString(), quiz);
            JOptionPane.showMessageDialog(this, "Test saved successfully: " + file.getFileName(), "Saved", JOptionPane.INFORMATION_MESSAGE);
            onSaved.run();
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save quiz: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importQuestionsFromCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Questions CSV");
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        int totalRequired = (int) questionCountSpinner.getValue();
        int remaining = totalRequired - createdQuestions.size();
        if (remaining <= 0) {
            JOptionPane.showMessageDialog(this, "Question limit already reached for this test.", "Import", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            List<Question> imported = fileHandler.importQuestionsFromCsv(chooser.getSelectedFile().getAbsolutePath());
            if (imported.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No valid questions found in CSV.", "Import", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int added = 0;
            for (Question question : imported) {
                if (added >= remaining) {
                    break;
                }
                createdQuestions.add(question);
                added++;
            }

            refreshQuestionListModel();
            JOptionPane.showMessageDialog(this,
                    "Imported " + added + " question(s). Total: " + createdQuestions.size() + "/" + totalRequired,
                    "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "CSV import failed: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return label;
    }

    private void styleInput(JTextField field, Color bg, Color fg) {
        PremiumTheme.styleTextField(field);
    }

    private void styleSpinner(JSpinner spinner, Color bg, Color fg) {
        spinner.setBackground(bg);
        spinner.setForeground(fg);
        PremiumTheme.styleSpinner(spinner);
    }

    private JButton createButton(String text, Color bg) {
        JButton button = new JButton(text);
        PremiumTheme.stylePrimaryButton(button);
        return button;
    }
}
