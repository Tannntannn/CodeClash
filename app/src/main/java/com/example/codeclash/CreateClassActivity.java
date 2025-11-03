package com.example.codeclash;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CreateClassActivity extends AppCompatActivity {

    private EditText etYearLevel, etBlock;
    private MaterialButton btnCreateClass;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_addpage);

        etYearLevel = findViewById(R.id.etYearLevel);
        etBlock = findViewById(R.id.etBlock);
        btnCreateClass = findViewById(R.id.btnCreateClass);

        db = FirebaseFirestore.getInstance();

        btnCreateClass.setOnClickListener(v -> {
            String yearLevel = etYearLevel.getText().toString().trim();
            String block = etBlock.getText().toString().trim();

            if (yearLevel.isEmpty() || block.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            String generatedCode = generateClassCode();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
                return;
            }
            String teacherUID = currentUser.getUid();

            Map<String, Object> classData = new HashMap<>();
            classData.put("yearLevel", yearLevel);
            classData.put("block", block);
            classData.put("classCode", generatedCode);
            classData.put("createdBy", teacherUID); // ✅ link to teacher
            classData.put("lessons", new String[]{ // default lessons
                    "INTRODUCTION TO JAVA",
                    "VARIABLES and DATA",
                    "OPERATORS and EXPRESSIONS",
                    "CONDITIONAL STATEMENTS",
                    "CONDITIONAL LOOPS",
                    "ARRAYS"
            });

            String[] defaultLessons = new String[]{
                    "INTRODUCTION TO JAVA",
                    "VARIABLES and DATA",
                    "OPERATORS and EXPRESSIONS",
                    "CONDITIONAL STATEMENTS",
                    "CONDITIONAL LOOPS",
                    "ARRAYS"
            };
            
            db.collection("Classes").document(generatedCode)
                    .set(classData)
                    .addOnSuccessListener(aVoid -> {
                        // Initialize all lessons with locked status
                        LessonManager.initializeLessons(generatedCode, defaultLessons);
                        
                        Toast.makeText(this, "Class created!\nCode: " + generatedCode, Toast.LENGTH_LONG).show();
                        etYearLevel.setText("");
                        etBlock.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to create class", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private String generateClassCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return code.toString();
    }
}
