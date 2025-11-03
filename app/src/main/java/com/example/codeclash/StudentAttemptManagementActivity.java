package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentAttemptManagementActivity extends AppCompatActivity {
    
    private String classCode;
    private String lessonName;
    private String activityType;
    private RecyclerView recyclerView;
    private StudentAttemptAdapter adapter;
    private List<StudentAttemptInfo> students = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_attempt_management);
        
        // Get data from intent
        classCode = getIntent().getStringExtra("classCode");
        lessonName = getIntent().getStringExtra("lessonName");
        activityType = getIntent().getStringExtra("activityType");
        
        System.out.println("🔍 AttemptManagement: Starting for class=" + classCode + 
                         ", lesson=" + lessonName + ", activity=" + activityType);
        
        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Attempts - " + lessonName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Setup UI
        setupUI();
        
        // Load students
        loadStudents();
    }
    
    private void setupUI() {
        recyclerView = findViewById(R.id.recyclerViewStudents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Set activity type display
        TextView activityTypeText = findViewById(R.id.textActivityType);
        String displayType = activityType.equals("quiz") ? "Quiz" : "Code Builder";
        activityTypeText.setText("Managing " + displayType + " Attempts");
        
        // Setup adapter
        adapter = new StudentAttemptAdapter(students, this::onStudentSelected, this);
        recyclerView.setAdapter(adapter);
    }
    
    private void loadStudents() {
        System.out.println("🔍 AttemptManagement: Loading students for class: " + classCode);
        
        FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("Students")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                students.clear();
                System.out.println("🔍 AttemptManagement: Found " + querySnapshot.size() + " students");
                
                for (QueryDocumentSnapshot document : querySnapshot) {
                    StudentAttemptInfo student = new StudentAttemptInfo();
                    student.studentId = document.getId();
                    student.studentName = document.getString("fullName");
                    if (student.studentName == null) {
                        student.studentName = document.getString("name");
                    }
                    if (student.studentName == null) {
                        student.studentName = "Unknown Student";
                    }
                    
                    // Load current attempts
                    loadStudentAttempts(student);
                    students.add(student);
                }
                
                adapter.notifyDataSetChanged();
                System.out.println("🔍 AttemptManagement: Loaded " + students.size() + " students");
            })
            .addOnFailureListener(e -> {
                System.out.println("🔍 AttemptManagement: Failed to load students: " + e.getMessage());
                Toast.makeText(this, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadStudentAttempts(StudentAttemptInfo student) {
        String attemptDocId = lessonName + "_" + activityType;
        
        FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("Students").document(student.studentId)
            .collection("Attempts").document(attemptDocId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long attemptsUsed = documentSnapshot.getLong("attemptsUsed");
                    student.attemptsUsed = attemptsUsed != null ? attemptsUsed.intValue() : 0;
                } else {
                    student.attemptsUsed = 0;
                }
                
                System.out.println("🔍 AttemptManagement: " + student.studentName + " has " + student.attemptsUsed + " attempts");
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                System.out.println("🔍 AttemptManagement: Failed to load attempts for " + student.studentName + ": " + e.getMessage());
                student.attemptsUsed = 0;
                adapter.notifyDataSetChanged();
            });
    }
    
    private void onStudentSelected(StudentAttemptInfo student) {
        System.out.println("🔍 AttemptManagement: Student selected: " + student.studentName);
        showAttemptControls(student);
    }
    
    private void showAttemptControls(StudentAttemptInfo student) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Manage Attempts for " + student.studentName);
        builder.setMessage("Current attempts used: " + student.attemptsUsed + "/3");
        
        String[] options = {
            "Add 1 Attempt",
            "Subtract 1 Attempt", 
            "Reset to 0 Attempts"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Add 1
                    addAttemptForStudent(student);
                    break;
                case 1: // Subtract 1
                    subtractAttemptForStudent(student);
                    break;
                case 2: // Reset
                    resetAttemptsForStudent(student);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    public void addAttemptForStudent(StudentAttemptInfo student) {
        System.out.println("🔍 AttemptManagement: Adding attempt for " + student.studentName);
        
        // Get current user as teacher
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Use LeaderboardManager to add attempt
        LeaderboardManager.teacherIncreaseAttempts(classCode, lessonName, activityType, student.studentId, teacherId, new LeaderboardManager.AttemptUpdateCallback() {
            @Override
            public void onSuccess(int newAttemptCount) {
                System.out.println("🔍 AttemptManagement: Successfully added attempt for " + student.studentName);
                Toast.makeText(StudentAttemptManagementActivity.this, "Added 1 attempt for " + student.studentName, Toast.LENGTH_SHORT).show();
                student.attemptsUsed = newAttemptCount; // Update the student object
                adapter.notifyDataSetChanged(); // Refresh the UI
            }
            
            @Override
            public void onFailure(Exception e) {
                System.out.println("🔍 AttemptManagement: Failed to add attempt for " + student.studentName + ": " + e.getMessage());
                Toast.makeText(StudentAttemptManagementActivity.this, "Failed to add attempt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void subtractAttemptForStudent(StudentAttemptInfo student) {
        System.out.println("🔍 AttemptManagement: Subtracting attempt for " + student.studentName);
        
        // Get current user as teacher
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Use LeaderboardManager to subtract attempt
        LeaderboardManager.teacherDecreaseAttempts(classCode, lessonName, activityType, student.studentId, teacherId, new LeaderboardManager.AttemptUpdateCallback() {
            @Override
            public void onSuccess(int newAttemptCount) {
                System.out.println("🔍 AttemptManagement: Successfully subtracted attempt for " + student.studentName);
                Toast.makeText(StudentAttemptManagementActivity.this, "Subtracted 1 attempt for " + student.studentName, Toast.LENGTH_SHORT).show();
                student.attemptsUsed = newAttemptCount; // Update the student object
                adapter.notifyDataSetChanged(); // Refresh the UI
            }
            
            @Override
            public void onFailure(Exception e) {
                System.out.println("🔍 AttemptManagement: Failed to subtract attempt for " + student.studentName + ": " + e.getMessage());
                Toast.makeText(StudentAttemptManagementActivity.this, "Failed to subtract attempt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    public void resetAttemptsForStudent(StudentAttemptInfo student) {
        System.out.println("🔍 AttemptManagement: Resetting attempts for " + student.studentName);
        
        // Get current user as teacher
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Use LeaderboardManager to reset attempts
        LeaderboardManager.teacherResetAttempts(classCode, lessonName, activityType, student.studentId, teacherId, new LeaderboardManager.AttemptUpdateCallback() {
            @Override
            public void onSuccess(int newAttemptCount) {
                System.out.println("🔍 AttemptManagement: Successfully reset attempts for " + student.studentName);
                Toast.makeText(StudentAttemptManagementActivity.this, "Reset attempts for " + student.studentName, Toast.LENGTH_SHORT).show();
                student.attemptsUsed = newAttemptCount; // Update the student object
                adapter.notifyDataSetChanged(); // Refresh the UI
            }
            
            @Override
            public void onFailure(Exception e) {
                System.out.println("🔍 AttemptManagement: Failed to reset attempts for " + student.studentName + ": " + e.getMessage());
                Toast.makeText(StudentAttemptManagementActivity.this, "Failed to reset attempts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Student data class
    public static class StudentAttemptInfo {
        public String studentId;
        public String studentName;
        public int attemptsUsed = 0;
    }
}
