package com.example.codeclash;

import java.util.List;
import java.util.Map;

public class LessonProblem {
    private String problemId;
    private String title;
    private String description;
    private String difficulty; // "Easy", "Medium", "Hard"
    private String starterCode;
    private List<TestCase> testCases;
    private String hints;
    private int points;
    
    // Default constructor for Firestore
    public LessonProblem() {}
    
    public LessonProblem(String problemId, String title, String description, String difficulty, 
                        String starterCode, List<TestCase> testCases, String hints, int points) {
        this.problemId = problemId;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.starterCode = starterCode;
        this.testCases = testCases;
        this.hints = hints;
        this.points = points;
    }
    
    // Getters and Setters
    public String getProblemId() { return problemId; }
    public void setProblemId(String problemId) { this.problemId = problemId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }
    
    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }
    
    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }
    
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    
    // Inner class for test cases
    public static class TestCase {
        private String input;
        private String expectedOutput;
        private String description;
        
        public TestCase() {}
        
        public TestCase(String input, String expectedOutput, String description) {
            this.input = input;
            this.expectedOutput = expectedOutput;
            this.description = description;
        }
        
        public String getInput() { return input; }
        public void setInput(String input) { this.input = input; }
        
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
