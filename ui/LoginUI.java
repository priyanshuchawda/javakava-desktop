package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.prefs.Preferences;

public class LoginUI extends JFrame {
    private static final String PREF_NODE = "javakava/login";
    private static final String PREF_NAME = "name";
    private static final String PREF_CLASS = "class";
    private static final String PREF_ROLL = "roll";

    public interface LoginHandler {
        void onLogin(String name, String classLevel, String rollNumber);
    }

    private final JTextField nameField;
    private final JTextField classField;
    private final JTextField rollField;
    private final Preferences preferences;

    public LoginUI(LoginHandler onStartQuiz) {
        this.preferences = Preferences.userRoot().node(PREF_NODE);

        setTitle("AI-Powered Smart Quiz & Learning System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 420);
        setLocationRelativeTo(null);

        Color bg = PremiumTheme.BG_TOP;
        Color card = PremiumTheme.CARD;
        Color text = PremiumTheme.TEXT;
        Color accent = PremiumTheme.ACCENT;

        JPanel root = PremiumTheme.createGradientPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("AI-Powered Smart Quiz & Learning System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(text);

        JLabel subtitle = new JLabel("Personalized quiz practice with adaptive learning", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(PremiumTheme.TEXT_SUB);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setBackground(bg);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel cardPanel = new JPanel();
        cardPanel.setBackground(card);
        cardPanel.setLayout(new GridLayout(8, 1, 0, 10));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(38, 44, 54), 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel inputLabel = new JLabel("Enter Username");
        inputLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputLabel.setForeground(text);

        nameField = new JTextField();
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        PremiumTheme.styleTextField(nameField);

            JLabel classLabel = new JLabel("Class");
            classLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            classLabel.setForeground(text);

            classField = new JTextField("General");
            classField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            PremiumTheme.styleTextField(classField);

            JLabel rollLabel = new JLabel("Roll Number");
            rollLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            rollLabel.setForeground(text);

            rollField = new JTextField("N/A");
            rollField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            PremiumTheme.styleTextField(rollField);

        JButton startButton = createButton("Start Quiz", accent, text);
        startButton.addActionListener(e -> attemptLogin(onStartQuiz));

        cardPanel.add(inputLabel);
        cardPanel.add(nameField);
            cardPanel.add(classLabel);
            cardPanel.add(classField);
            cardPanel.add(rollLabel);
            cardPanel.add(rollField);
            cardPanel.add(new JLabel(""));
        cardPanel.add(startButton);

        root.add(titlePanel, BorderLayout.NORTH);
        root.add(cardPanel, BorderLayout.CENTER);

        setContentPane(root);

        loadLastLogin();
        getRootPane().setDefaultButton(startButton);
    }

    private void attemptLogin(LoginHandler onStartQuiz) {
        String name = sanitizeForDisplay(nameField.getText(), 40);
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String classLevel = sanitizeForDisplay(classField.getText(), 30);
        if (classLevel.isEmpty()) {
            classLevel = "General";
        }

        String rollNumber = sanitizeForDisplay(rollField.getText(), 30);
        if (rollNumber.isEmpty()) {
            rollNumber = "N/A";
        }

        nameField.setText(name);
        classField.setText(classLevel);
        rollField.setText(rollNumber);

        preferences.put(PREF_NAME, name);
        preferences.put(PREF_CLASS, classLevel);
        preferences.put(PREF_ROLL, rollNumber);

        onStartQuiz.onLogin(name, classLevel, rollNumber);
    }

    private void loadLastLogin() {
        String savedName = preferences.get(PREF_NAME, "").trim();
        String savedClass = preferences.get(PREF_CLASS, "General").trim();
        String savedRoll = preferences.get(PREF_ROLL, "N/A").trim();

        if (!savedName.isEmpty()) {
            nameField.setText(savedName);
        }
        classField.setText(savedClass.isEmpty() ? "General" : savedClass);
        rollField.setText(savedRoll.isEmpty() ? "N/A" : savedRoll);
    }

    private String sanitizeForDisplay(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        PremiumTheme.stylePrimaryButton(button);
        return button;
    }
}
