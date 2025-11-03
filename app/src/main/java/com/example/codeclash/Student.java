package com.example.codeclash;

public class Student {
    public String studentId;
    public String userId;
    public String fullName;
    public String yearLevel;
    public String block;
    
    public Student() {
        // Default constructor required for Firebase
    }
    
    public Student(String studentId, String userId, String fullName, String yearLevel, String block) {
        this.studentId = studentId;
        this.userId = userId;
        this.fullName = fullName;
        this.yearLevel = yearLevel;
        this.block = block;
    }
}
