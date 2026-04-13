package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class PremiumTheme {
    public static final Color BG_TOP = new Color(10, 13, 18);
    public static final Color BG_BOTTOM = new Color(5, 7, 10);
    public static final Color CARD = new Color(20, 26, 34);
    public static final Color CARD_ALT = new Color(24, 31, 40);
    public static final Color TEXT = new Color(240, 245, 250);
    public static final Color TEXT_SUB = new Color(166, 176, 188);
    public static final Color ACCENT = new Color(105, 228, 205);
    public static final Color ACCENT_DARK = new Color(72, 189, 167);
    public static final Color DANGER = new Color(232, 111, 111);
    public static final Color BORDER = new Color(48, 58, 72);

    private PremiumTheme() {
    }

    public static JPanel createGradientPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gradient = new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM);
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    public static void stylePrimaryButton(JButton button) {
        styleButton(button, ACCENT, ACCENT_DARK, Color.BLACK, new Color(64, 168, 148));
    }

    public static void styleSecondaryButton(JButton button) {
        styleButton(button, new Color(113, 140, 177), new Color(92, 119, 156), Color.BLACK, new Color(73, 96, 127));
    }

    public static void styleDangerButton(JButton button) {
        styleButton(button, DANGER, new Color(208, 90, 90), Color.BLACK, new Color(173, 74, 74));
    }

    private static void styleButton(JButton button, Color bg, Color hover, Color fg, Color borderColor) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Border line = BorderFactory.createLineBorder(borderColor, 1);
        Border pad = new EmptyBorder(10, 16, 10, 16);
        button.setBorder(new CompoundBorder(line, pad));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bg);
            }
        });
    }

    public static void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBackground(CARD_ALT);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    public static void styleTextArea(JTextArea area) {
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBackground(CARD_ALT);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
    }

    public static void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField tf = defaultEditor.getTextField();
            styleTextField(tf);
        }
    }
}
