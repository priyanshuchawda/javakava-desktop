package ui;

import model.Attempt;
import model.Question;
import model.Result;
import model.Student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ResultUI extends JFrame {
    private final JTable attemptTable;
    private final JTextArea explanationArea;
    private final JLabel questionDetailLabel;
    private final JLabel answerDetailLabel;
    private final JLabel topicDetailLabel;
    private final List<Attempt> attempts;

    public ResultUI(Student student, Result result, Runnable onBackToDashboard) {
        setTitle("Quiz Result - " + student.getName());
        setSize(1080, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Color bg = PremiumTheme.BG_TOP;
        Color card = PremiumTheme.CARD;
        Color text = PremiumTheme.TEXT;
        Color accent = PremiumTheme.ACCENT;
        Color sub = PremiumTheme.TEXT_SUB;

        this.attempts = result.getAttempts();

        JPanel root = PremiumTheme.createGradientPanel(new BorderLayout(12, 12));
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Quiz Performance Summary");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(text);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        statsPanel.setBackground(bg);
        statsPanel.add(createStatCard("Score", String.format(java.util.Locale.US, "%.2f / %d", result.getScore(), result.getTotalQuestions()), card, text, sub));
        statsPanel.add(createStatCard("Accuracy", String.format(java.util.Locale.US, "%.2f%%", result.getAccuracy()), card, text, sub));
        statsPanel.add(createStatCard("Weak Topics", String.valueOf(result.getWeakTopics().size()), card, text, sub));

        JLabel topicSummary = new JLabel("Topic Performance: " + buildTopicSummary(result.getTopicPerformance()));
        topicSummary.setForeground(sub);
        topicSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPanel head = new JPanel(new BorderLayout(8, 8));
        head.setBackground(bg);
        head.add(title, BorderLayout.NORTH);
        head.add(statsPanel, BorderLayout.CENTER);
        head.add(topicSummary, BorderLayout.SOUTH);

        DefaultTableModel model = new DefaultTableModel(new Object[]{"#", "Topic", "Your Answer", "Correct Answer", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int i = 0; i < attempts.size(); i++) {
            Attempt attempt = attempts.get(i);
            Question q = attempt.getQuestion();
            model.addRow(new Object[]{
                    i + 1,
                    q.getTopic(),
                    attempt.getSelectedAnswer(),
                    q.getCorrectAnswer(),
                    attempt.isCorrect() ? "Correct" : "Incorrect"
            });
        }

        attemptTable = new JTable(model);
        attemptTable.setRowHeight(28);
        attemptTable.setBackground(card);
        attemptTable.setForeground(text);
        attemptTable.setSelectionBackground(new Color(38, 56, 72));
        attemptTable.setSelectionForeground(text);
        attemptTable.setGridColor(new Color(36, 44, 56));
        attemptTable.setFillsViewportHeight(true);
        attemptTable.getTableHeader().setBackground(new Color(22, 28, 36));
        attemptTable.getTableHeader().setForeground(text);
        attemptTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        attemptTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        attemptTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateDetailPanel(attemptTable.getSelectedRow());
            }
        });

        JScrollPane tableScroll = new JScrollPane(attemptTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(PremiumTheme.BORDER, 1));
        tableScroll.getViewport().setBackground(card);

        JPanel detailPanel = new JPanel(new BorderLayout(10, 10));
        detailPanel.setBackground(card);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        questionDetailLabel = new JLabel();
        questionDetailLabel.setForeground(text);
        questionDetailLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));

        answerDetailLabel = new JLabel();
        answerDetailLabel.setForeground(sub);
        answerDetailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        topicDetailLabel = new JLabel();
        topicDetailLabel.setForeground(sub);
        topicDetailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        explanationArea = new JTextArea();
        explanationArea.setEditable(false);
        explanationArea.setLineWrap(true);
        explanationArea.setWrapStyleWord(true);
        explanationArea.setBackground(PremiumTheme.CARD_ALT);
        explanationArea.setForeground(text);
        explanationArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        explanationArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane explanationScroll = new JScrollPane(explanationArea);
        explanationScroll.setBorder(BorderFactory.createLineBorder(PremiumTheme.BORDER, 1));

        JPanel detailTop = new JPanel(new GridLayout(3, 1, 4, 4));
        detailTop.setBackground(card);
        detailTop.add(questionDetailLabel);
        detailTop.add(answerDetailLabel);
        detailTop.add(topicDetailLabel);

        detailPanel.add(detailTop, BorderLayout.NORTH);
        detailPanel.add(explanationScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, detailPanel);
        splitPane.setResizeWeight(0.56);
        splitPane.setDividerSize(8);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(bg);

        JButton backBtn = new JButton("Back to Dashboard");
        styleButton(backBtn, accent);
        backBtn.setToolTipText("Return to dashboard");
        backBtn.addActionListener(e -> {
            onBackToDashboard.run();
            dispose();
        });

        JButton copySummaryBtn = new JButton("Copy Summary");
        styleButton(copySummaryBtn, accent);
        copySummaryBtn.setToolTipText("Copy this result summary to clipboard");
        copySummaryBtn.addActionListener(e -> copySummaryToClipboard(student, result));

        JButton exportSummaryBtn = new JButton("Export Summary");
        styleButton(exportSummaryBtn, accent);
        exportSummaryBtn.setToolTipText("Save result summary as a text file in data/results");
        exportSummaryBtn.addActionListener(e -> exportSummary(student, result));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(bg);
        bottom.add(exportSummaryBtn);
        bottom.add(copySummaryBtn);
        bottom.add(backBtn);

        root.add(head, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        if (!attempts.isEmpty()) {
            attemptTable.setRowSelectionInterval(0, 0);
            updateDetailPanel(0);
        }
    }

    private JPanel createStatCard(String label, String value, Color card, Color text, Color sub) {
        JPanel stat = new JPanel(new GridLayout(2, 1));
        stat.setBackground(card);
        stat.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PremiumTheme.BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel l = new JLabel(label);
        l.setForeground(sub);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel v = new JLabel(value);
        v.setForeground(text);
        v.setFont(new Font("Segoe UI", Font.BOLD, 18));

        stat.add(l);
        stat.add(v);
        return stat;
    }

    private void updateDetailPanel(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= attempts.size()) {
            return;
        }

        Attempt attempt = attempts.get(selectedRow);
        Question q = attempt.getQuestion();
        questionDetailLabel.setText("Q" + (selectedRow + 1) + ": " + q.getQuestionText());

        String statusLabel = attempt.isCorrect() ? "Correct" : "Incorrect";
        answerDetailLabel.setText("Your Answer: " + attempt.getSelectedAnswer() + " | Correct: " + q.getCorrectAnswer() + " | Status: " + statusLabel);
        answerDetailLabel.setForeground(attempt.isCorrect() ? new Color(121, 212, 159) : new Color(244, 128, 128));

        topicDetailLabel.setText("Topic: " + q.getTopic() + " | Difficulty: " + q.getDifficulty());
        explanationArea.setText(q.getExplanation());
        explanationArea.setCaretPosition(0);
    }

    private String buildTopicSummary(Map<String, Double> topicPerformance) {
        if (topicPerformance == null || topicPerformance.isEmpty()) {
            return "No topic stats available.";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> entry : topicPerformance.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(entry.getKey())
                    .append(": ")
                    .append(String.format(java.util.Locale.US, "%.1f%%", entry.getValue()));
        }
        return sb.toString();
    }

    private void styleButton(JButton button, Color color) {
        PremiumTheme.stylePrimaryButton(button);
    }

    private void copySummaryToClipboard(Student student, Result result) {
        String summary = buildSummary(student, result);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(summary), null);
        JOptionPane.showMessageDialog(this, "Result summary copied to clipboard.", "Copied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportSummary(Student student, Result result) {
        try {
            Path resultDir = Paths.get("data/results");
            if (!Files.exists(resultDir)) {
                Files.createDirectories(resultDir);
            }

            String safeName = student.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path out = resultDir.resolve("summary_" + safeName + "_" + stamp + ".txt");

            Files.writeString(out, buildSummary(student, result), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(this, "Summary exported: " + out.getFileName(), "Exported", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export summary: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String buildSummary(Student student, Result result) {
        return "Student: " + student.getName() + "\n"
                + "Quiz: " + result.getQuizTitle() + "\n"
                + "Score: " + String.format(java.util.Locale.US, "%.2f / %d", result.getScore(), result.getTotalQuestions()) + "\n"
                + "Accuracy: " + String.format(java.util.Locale.US, "%.2f%%", result.getAccuracy()) + "\n"
                + "Weak Topics: " + String.join(", ", result.getWeakTopics()) + "\n"
                + "Strong Topics: " + String.join(", ", result.getStrongTopics()) + "\n"
                + "Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
