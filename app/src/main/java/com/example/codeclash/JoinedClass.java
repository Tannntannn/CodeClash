package com.example.codeclash;

public class JoinedClass {
    private String classCode;
    private String yearLevel;
    private String block;

    public JoinedClass() {
        // Needed for Firestore
    }

    public JoinedClass(String classCode, String yearLevel, String block) {
        this.classCode = classCode;
        this.yearLevel = yearLevel;
        this.block = block;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public String getBlock() {
        return block;
    }
}
