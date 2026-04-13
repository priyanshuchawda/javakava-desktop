package model;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String name;
    private String classLevel;
    private String rollNumber;
    private List<Result> history;

    public Student(String name) {
        this(name, "General", "N/A");
    }

    public Student(String name, String classLevel, String rollNumber) {
        this.name = name;
        this.classLevel = classLevel == null || classLevel.isBlank() ? "General" : classLevel;
        this.rollNumber = rollNumber == null || rollNumber.isBlank() ? "N/A" : rollNumber;
        this.history = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Result> getHistory() {
        return history;
    }

    public String getClassLevel() {
        return classLevel;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void addResult(Result result) {
        this.history.add(result);
    }
}
