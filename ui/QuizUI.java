package ui;

import model.Question;
import model.Quiz;
import model.Result;
import model.Student;
import service.QuizService;
import utils.FileHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class QuizUI extends JFrame {
    private final Student student;
    private final QuizService quizService;
    private final FileHandler fileHandler;
    private final Runnable onBackToDashboard;
    private final int questionTimeLimitSeconds;

    private final JLabel questionLabel;
    private final JRadioButton[] options;
    private final ButtonGroup optionGroup;
    private final JLabel progressLabel;
    private final JLabel timerLabel;
    private final JProgressBar progressBar;
    private final JButton nextButton;
    private final JButton skipButton;
    private final boolean fullTestTimerMode;
    private final int fullTestTimeLimitSeconds;

    private Timer timer;
    private int secondsLeft;
    private int totalSecondsLeft;
    private boolean quizFinished;
    private boolean closeHandled;

    public QuizUI(Student student, Quiz quiz, QuizService quizService, FileHandler fileHandler, Runnable onBackToDashboard) {
        this.student = student;
        this.quizService = quizService;
        this.fileHandler = fileHandler;
        this.onBackToDashboard = onBackToDashboard;
        this.fullTestTimerMode = quiz.isFullTestTimerMode() && quiz.getFullTestTimeSeconds() > 0;
        this.fullTestTimeLimitSeconds = Math.max(5, quiz.getFullTestTimeSeconds());
        this.questionTimeLimitSeconds = Math.max(5, quiz.getTimePerQuestionSeconds());
        this.totalSecondsLeft = fullTestTimeLimitSeconds;
        this.quizFinished = false;
        this.closeHandled = false;

        setTitle(quiz.getTitle() + " - Quiz");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCloseRequest();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                stopTimer();
                if (!quizFinished && !closeHandled) {
                    closeHandled = true;
                    onBackToDashboard.run();
                }
            }
        });

        quizService.startQuiz(quiz);

        Color bg = PremiumTheme.BG_TOP;
        Color card = PremiumTheme.CARD;
        Color text = PremiumTheme.TEXT;
        Color accent = PremiumTheme.ACCENT;

        JPanel root = PremiumTheme.createGradientPanel(new BorderLayout(14, 14));
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(bg);
        progressLabel = new JLabel("Question 1/" + quizService.getTotalQuestions());
        progressLabel.setForeground(text);
        progressLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        timerLabel = new JLabel(timeLabelText(), SwingConstants.RIGHT);
        timerLabel.setForeground(new Color(255, 205, 117));
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));

        top.add(progressLabel, BorderLayout.WEST);
        top.add(timerLabel, BorderLayout.EAST);

        progressBar = new JProgressBar(0, Math.max(1, quizService.getTotalQuestions()));
        progressBar.setValue(1);
        progressBar.setStringPainted(true);
        progressBar.setForeground(accent);
        progressBar.setBackground(new Color(26, 31, 38));

        JPanel questionCard = new JPanel(new BorderLayout(12, 12));
        questionCard.setBackground(card);
        questionCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
            new EmptyBorder(18, 18, 18, 18)
        ));

        questionLabel = new JLabel();
        questionLabel.setForeground(text);
        questionLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        optionsPanel.setBackground(card);
        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            options[i] = new JRadioButton();
                options[i].setBackground(PremiumTheme.CARD_ALT);
            options[i].setForeground(text);
            options[i].setFont(new Font("Segoe UI", Font.PLAIN, 15));
            options[i].setFocusPainted(false);
                options[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                    new EmptyBorder(8, 8, 8, 8)
                ));
            optionGroup.add(options[i]);
            optionsPanel.add(options[i]);
        }

        questionCard.add(questionLabel, BorderLayout.NORTH);
        questionCard.add(optionsPanel, BorderLayout.CENTER);

        nextButton = new JButton("Next");
        PremiumTheme.stylePrimaryButton(nextButton);
        nextButton.setToolTipText("Submit selected answer and move ahead");
        nextButton.addActionListener(e -> onNextPressed());

        skipButton = new JButton("Skip");
        PremiumTheme.styleSecondaryButton(skipButton);
        skipButton.setToolTipText("Skip this question (shortcut: Ctrl+S)");
        skipButton.addActionListener(e -> onSkipPressed());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(bg);
        bottom.add(skipButton);
        bottom.add(nextButton);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(bg);
        center.add(progressBar, BorderLayout.NORTH);
        center.add(questionCard, BorderLayout.CENTER);

        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        bindShortcuts();

        loadCurrentQuestion();
        restartTimer();
    }

    private void loadCurrentQuestion() {
        Question question = quizService.getCurrentQuestion();
        if (question == null) {
            finishQuizAndShowResult();
            return;
        }

        int questionNumber = quizService.getCurrentQuestionNumber();
        int total = quizService.getTotalQuestions();

        progressLabel.setText("Question " + questionNumber + "/" + total);
        progressBar.setMaximum(Math.max(1, total));
        progressBar.setValue(Math.min(questionNumber, total));
        nextButton.setText(questionNumber == total ? "Submit Quiz" : "Next");

        questionLabel.setText("<html><body style='width:730px'>" + question.getQuestionText() + "</body></html>");

        String[] questionOptions = question.getOptions();
        for (int i = 0; i < options.length; i++) {
            options[i].setText(questionOptions[i]);
            options[i].setActionCommand(questionOptions[i]);
        }
        optionGroup.clearSelection();
    }

    private void onNextPressed() {
        if (optionGroup.getSelection() == null) {
            JOptionPane.showMessageDialog(this, "Select an option before moving to next question.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String selectedAnswer = optionGroup.getSelection().getActionCommand();
        submitCurrentAnswer(selectedAnswer, false);

        if (!quizService.hasNextQuestion()) {
            finishQuizAndShowResult();
            return;
        }

        loadCurrentQuestion();
        restartTimer();
    }

    private void onSkipPressed() {
        submitCurrentAnswer("No Answer", true);

        if (!quizService.hasNextQuestion()) {
            finishQuizAndShowResult();
            return;
        }

        loadCurrentQuestion();
        restartTimer();
    }

    private void restartTimer() {
        stopTimer();

        if (fullTestTimerMode) {
            if (totalSecondsLeft <= 0) {
                totalSecondsLeft = fullTestTimeLimitSeconds;
            }
            timerLabel.setText(timeLabelText());
            timer = new Timer(1000, e -> {
                if (!isDisplayable() || quizFinished) {
                    stopTimer();
                    return;
                }

                totalSecondsLeft--;
                timerLabel.setText(timeLabelText());

                if (totalSecondsLeft <= 0) {
                    timer.stop();
                    handleFullTimeExpired();
                }
            });
            timer.start();
            return;
        }

        secondsLeft = questionTimeLimitSeconds;
        timerLabel.setText(timeLabelText());

        timer = new Timer(1000, e -> {
            if (!isDisplayable() || quizFinished) {
                stopTimer();
                return;
            }

            secondsLeft--;
            timerLabel.setText(timeLabelText());

            if (secondsLeft <= 0) {
                timer.stop();
                handleTimeExpired();
            }
        });
        timer.start();
    }

    private void handleTimeExpired() {
        if (!isDisplayable() || quizFinished) {
            return;
        }

        Question current = quizService.getCurrentQuestion();
        if (current == null) {
            finishQuizAndShowResult();
            return;
        }

        submitCurrentAnswer("No Answer", true);

        if (!quizService.hasNextQuestion()) {
            finishQuizAndShowResult();
            return;
        }

        JOptionPane.showMessageDialog(this, "Time up. Moving to next question.",
            "Timer", JOptionPane.INFORMATION_MESSAGE);
        loadCurrentQuestion();
        restartTimer();
    }

    private void handleFullTimeExpired() {
        if (!isDisplayable() || quizFinished) {
            return;
        }

        submitCurrentAnswer("No Answer", true);

        while (quizService.hasNextQuestion()) {
            Question pending = quizService.getCurrentQuestion();
            if (pending == null) {
                break;
            }
            quizService.submitDetailedAnswer("No Answer", 0, true, false);
        }

        finishQuizAndShowResult();
    }

    private void submitCurrentAnswer(String selectedAnswer, boolean skipped) {
        int elapsedSeconds = fullTestTimerMode
                ? Math.max(0, fullTestTimeLimitSeconds - totalSecondsLeft)
                : Math.max(0, questionTimeLimitSeconds - secondsLeft);
        quizService.submitDetailedAnswer(selectedAnswer, elapsedSeconds, skipped, false);
    }

    private String timeLabelText() {
        if (fullTestTimerMode) {
            return "Total time left: " + Math.max(0, totalSecondsLeft) + "s";
        }
        return "Time left: " + Math.max(0, secondsLeft) + "s";
    }

    private void finishQuizAndShowResult() {
        quizFinished = true;
        stopTimer();

        Result result = quizService.finishQuiz();
        student.addResult(result);
        saveResult(result);

        ResultUI resultUI = new ResultUI(student, result, onBackToDashboard);
        resultUI.setVisible(true);
        dispose();
    }

    private void handleCloseRequest() {
        if (quizFinished) {
            dispose();
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Leave this test now? Current progress will not be saved.",
                "Exit Test",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            stopTimer();
            dispose();
        }
    }

    private void bindShortcuts() {
        JRootPane root = getRootPane();
        InputMap inputMap = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = root.getActionMap();

        for (int i = 0; i < options.length; i++) {
            final int index = i;
            String actionKey = "selectOption" + i;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), actionKey);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD1 + i, 0), actionKey);
            actionMap.put(actionKey, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (index >= 0 && index < options.length) {
                        options[index].setSelected(true);
                    }
                }
            });
        }

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextQuestion");
        actionMap.put("nextQuestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onNextPressed();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "skipQuestion");
        actionMap.put("skipQuestion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSkipPressed();
            }
        });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void saveResult(Result result) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String basePath = "data/results/" + student.getName() + "_" + timestamp;

        try {
            fileHandler.saveResultToJson(basePath + ".json", result, student.getName());
            fileHandler.appendResultToCsv(
                    "data/results/leaderboard.csv",
                    result,
                    student.getName(),
                    student.getClassLevel(),
                    student.getRollNumber()
            );
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save result: " + ex.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
