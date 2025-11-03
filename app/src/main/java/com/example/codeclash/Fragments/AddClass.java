package com.example.codeclash.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.codeclash.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AddClass extends Fragment {

    private TextInputEditText etClassCode, etYearLevel, etBlock;
    private Button btnCreateClass, btnGenerateCode;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_addpage, container, false);

        // Initialize views
        etClassCode = view.findViewById(R.id.etClassCode);
        etYearLevel = view.findViewById(R.id.etYearLevel);
        etBlock = view.findViewById(R.id.etBlock);
        btnCreateClass = view.findViewById(R.id.btnCreateClass);
        btnGenerateCode = view.findViewById(R.id.btnGenerateCode);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        btnCreateClass.setOnClickListener(v -> createClass());
        btnGenerateCode.setOnClickListener(v -> generateCode());

        return view;
    }

    private void generateCode() {
        // Create a short random code (6 characters)
        String generatedCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        etClassCode.setText(generatedCode);
        Toast.makeText(getContext(), "Class code generated!", Toast.LENGTH_SHORT).show();
    }

    private void createClass() {
        String classCode = etClassCode.getText().toString().trim();
        String year = etYearLevel.getText().toString().trim();
        String block = etBlock.getText().toString().trim();

        if (classCode.isEmpty() || year.isEmpty() || block.isEmpty()) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> classData = new HashMap<>();
        classData.put("yearLevel", year);
        classData.put("block", block);
        classData.put("createdBy", uid);
        classData.put("lessons", Arrays.asList(
                "INTRODUCTION TO JAVA",
                "VARIABLES and DATA",
                "OPERATORS and EXPRESSIONS",
                "CONDITIONAL STATEMENTS",
                "CONDITIONAL LOOPS",
                "ARRAYS"
        ));

        String[] defaultLessons = new String[]{
                "INTRODUCTION TO JAVA",
                "VARIABLES and DATA",
                "OPERATORS and EXPRESSIONS",
                "CONDITIONAL STATEMENTS",
                "CONDITIONAL LOOPS",
                "ARRAYS"
        };

        db.collection("Classes")
                .document(classCode)
                .set(classData)
                .addOnSuccessListener(aVoid -> {
                    // Initialize all lessons with locked status
                    com.example.codeclash.LessonManager.initializeLessons(classCode, defaultLessons);
                    
                    Toast.makeText(getContext(), "Class created!", Toast.LENGTH_SHORT).show();
                    etClassCode.setText("");
                    etYearLevel.setText("");
                    etBlock.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
