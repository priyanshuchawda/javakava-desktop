import model.Student;
import service.EvaluationService;
import service.QuizService;
import ui.DashboardUI;
import ui.LoginUI;
import utils.EnvLoader;
import utils.FileHandler;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        EnvLoader.loadDotEnv(".env");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fallback to default look and feel if system look and feel is unavailable.
            }

            FileHandler fileHandler = new FileHandler();
            EvaluationService evaluationService = new EvaluationService();
            QuizService quizService = new QuizService(evaluationService);

            final LoginUI[] loginUIRef = new LoginUI[1];
            loginUIRef[0] = new LoginUI((username, classLevel, rollNumber) -> {
                Student student = new Student(username, classLevel, rollNumber);
                DashboardUI dashboardUI = new DashboardUI(student, quizService, fileHandler);
                dashboardUI.setVisible(true);
                loginUIRef[0].dispose();
            });

            loginUIRef[0].setVisible(true);
        });
    }
}
