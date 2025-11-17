package com.example.codeclash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CompilerModeActivity extends AppCompatActivity {
    
    private EditText codeEditor;
    private EditText inputEditor;
    private TextView outputText;
    private TextView inputHintText;
    private TextView problemTitle;
    private TextView problemDescription;
    private MaterialButton btnRun, btnClear, btnSubmit;
    private MaterialButton btnLoadSampleInput, btnClearInput;
    private ProgressBar progressBar;
    private com.google.android.material.card.MaterialCardView submissionStatusCard;
    private TextView submissionStatusTitle;
    private TextView submissionStatusText;
    private OkHttpClient client;
    private Handler mainHandler;
    private android.content.SharedPreferences preferences;
    private static final String PREFS_NAME = "CompilerPrefs";
    private static final String EXTRA_LESSON_NAME = "lessonName";
    private static final String EXTRA_CLASS_CODE = "classCode";
    private String lessonNameKey = "default";
    private String classCode = "";
    private String studentId = "";
    private FirebaseFirestore db;
    private String studentDisplayName = "Student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compiler_mode);

        initializeViews();
        setupToolbar();
        setupButtons();
        initializeHttpClient();
        
        // Set default Java template
        setDefaultJavaCode();

        // Restore last input if available
        restoreSavedInput();
        updateInputHint();

        // Restore saved code from Firebase
        restoreCodeFromFirebase();

        // Populate lesson task card if lesson is provided
        populateLessonTask();
        
        // Check for existing submission status
        checkSubmissionStatus();
    }
    
    private void initializeViews() {
        codeEditor = findViewById(R.id.codeEditor);
        outputText = findViewById(R.id.outputText);
        inputEditor = findViewById(R.id.inputEditor);
        inputHintText = findViewById(R.id.inputHintText);
        problemTitle = findViewById(R.id.problemTitle);
        problemDescription = findViewById(R.id.problemDescription);
        btnRun = findViewById(R.id.btnRun);
        btnClear = findViewById(R.id.btnClear);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnLoadSampleInput = findViewById(R.id.btnLoadSampleInput);
        btnClearInput = findViewById(R.id.btnClearInput);
        progressBar = findViewById(R.id.progressBar);
        submissionStatusCard = findViewById(R.id.submissionStatusCard);
        submissionStatusTitle = findViewById(R.id.submissionStatusTitle);
        submissionStatusText = findViewById(R.id.submissionStatusText);
        
        mainHandler = new Handler(Looper.getMainLooper());
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
        
        // Get lesson name and class code from intent
        String incomingLesson = getIntent() != null ? getIntent().getStringExtra(EXTRA_LESSON_NAME) : null;
        String incomingClassCode = getIntent() != null ? getIntent().getStringExtra(EXTRA_CLASS_CODE) : null;
        
        if (incomingLesson != null && !incomingLesson.trim().isEmpty()) {
            lessonNameKey = incomingLesson.trim();
        }
        if (incomingClassCode != null && !incomingClassCode.trim().isEmpty()) {
            classCode = incomingClassCode.trim();
        }
        
        // Get current user ID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            loadStudentDisplayName();
        } else {
            studentDisplayName = "Student";
        }
        
        // Disable spell check and auto-correction for code editor
        setupCodeEditor();

        // Watch for changes to update hint visibility
        codeEditor.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateInputHint(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        inputEditor.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateInputHint(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadStudentDisplayName() {
        if (studentId == null || studentId.isEmpty()) {
            studentDisplayName = getEmailFallbackName();
            return;
        }
        UserNameManager.getUserName(studentId, new UserNameManager.NameCallback() {
            @Override
            public void onSuccess(String name) {
                studentDisplayName = name;
            }

            @Override
            public void onFailure(String fallbackName) {
                studentDisplayName = getEmailFallbackName();
            }
        });
    }

    private String getEmailFallbackName() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String displayName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName.trim();
            }
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null && email.contains("@")) {
                return email.split("@")[0];
            }
        }
        return "Student";
    }

    private String resolveStudentName() {
        if (studentDisplayName != null && !studentDisplayName.trim().isEmpty() && !"Unknown".equalsIgnoreCase(studentDisplayName)) {
            return studentDisplayName;
        }
        return getEmailFallbackName();
    }
    
    private void setupCodeEditor() {
        // Disable spell check, auto-correction, and text suggestions
        codeEditor.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                               android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | 
                               android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        
        // Disable auto-correction and spell check programmatically
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            codeEditor.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        }
        
        // Set monospace font for better code readability
        codeEditor.setTypeface(android.graphics.Typeface.MONOSPACE);
        
        // Remove any text decorations (underlines, etc.)
        codeEditor.setPaintFlags(codeEditor.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupButtons() {
        btnRun.setOnClickListener(v -> runCode());
        btnClear.setOnClickListener(v -> clearCode());
        btnSubmit.setOnClickListener(v -> submitForReview());
        if (btnLoadSampleInput != null) {
            btnLoadSampleInput.setOnClickListener(v -> {
                String code = codeEditor.getText().toString();
                String sample = generateSampleInput(code);
                inputEditor.setText(sample);
                saveInput();
                updateInputHint();
                Toast.makeText(this, "Sample input loaded", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnClearInput != null) {
            btnClearInput.setOnClickListener(v -> {
                inputEditor.setText("");
                saveInput();
                updateInputHint();
            });
        }
    }
    
    private void initializeHttpClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    private void setDefaultJavaCode() {
        String defaultCode = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello CodeClash\");\n" +
                "    }\n" +
                "}";
        codeEditor.setText(defaultCode);
    }
    
    private void runCode() {
        String code = codeEditor.getText().toString().trim();
        String stdin = inputEditor != null ? inputEditor.getText().toString() : "";
        
        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter some Java code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check network connectivity for compilation
        NetworkManager.NetworkStatus networkStatus = NetworkManager.getNetworkStatus(this);
        if (!networkStatus.isAvailable) {
            showNetworkErrorDialog(networkStatus.message);
            return;
        }
        
        // Smart input detection and prompting
        if (JDoodleApiHelper.isInteractiveCode(code) && stdin.trim().isEmpty()) {
            showInteractiveInputDialog(code);
            return;
        }
        // Persist input for this lesson
        saveInput();
        
        // Show loading state
        showLoading(true);
        outputText.setText("Compiling and executing...");
        
        // Execute code via JDoodle API
        executeCodeOnJDoodle(code, stdin);
    }
    
    private void showInteractiveInputDialog(String code) {
        String prompt = JDoodleApiHelper.getInteractivePrompt(code);
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Interactive Input Required")
               .setMessage("Your code uses Scanner input. " + prompt + "\n\nWould you like to provide input now?")
               .setPositiveButton("Yes, Add Input", (dialog, which) -> {
                   // Focus on input field and show hint
                   inputEditor.requestFocus();
                   inputEditor.setHint(prompt);
                   Toast.makeText(this, "Add your input values above, then click RUN", Toast.LENGTH_LONG).show();
               })
               .setNegativeButton("Run Without Input", (dialog, which) -> {
                   // Run with empty input (will show error)
                   showLoading(true);
                   outputText.setText("Compiling and executing...");
                   executeCodeOnJDoodle(code, "");
               })
               .setNeutralButton("Cancel", null)
               .show();
    }
    
    private void clearCode() {
        codeEditor.setText("");
        outputText.setText("Output will appear here...");
    }

    private void populateLessonTask() {
        if (problemTitle == null || problemDescription == null) return;
        String lesson = resolveLessonKey(lessonNameKey);
        LessonProblem lp = ProblemManager.getProblemForLesson(lesson);
        if (lp != null) {
            problemTitle.setText(lp.getTitle());
            problemDescription.setText(lp.getDescription());
        } else {
            problemTitle.setText("Lesson Task");
            problemDescription.setText("Task details will appear here.");
        }
    }

    private String resolveLessonKey(String incoming) {
        if (incoming == null) return null;
        String key = incoming.trim();
        String lower = key.toLowerCase();
        // Direct keys supported
        if (ProblemManager.getProblemForLesson(key) != null) return key;
        // Map UI titles to lesson keys 1-6
        if (lower.contains("introduction") && lower.contains("java")) return "lesson 1";
        if (lower.contains("variables") || lower.contains("data")) return "lesson 2";
        if (lower.contains("operators") || lower.contains("expressions")) return "lesson 3";
        if (lower.contains("conditional statements") || lower.equals("conditional statements")) return "lesson 4";
        if (lower.contains("conditional loops") || lower.contains("loops")) return "lesson 5: guess the number game"; // default to first of lesson 5 activities
        if (lower.contains("arrays")) return "lesson 6";
        return key;
    }

    private void restoreSavedInput() {
        if (inputEditor == null) return;
        String key = "stdin_" + lessonNameKey;
        String saved = preferences.getString(key, null);
        if (saved != null) {
            inputEditor.setText(saved);
        }
    }

    private void saveInput() {
        if (inputEditor == null) return;
        String key = "stdin_" + lessonNameKey;
        preferences.edit().putString(key, inputEditor.getText().toString()).apply();
    }

    private void updateInputHint() {
        if (inputHintText == null) return;
        String code = codeEditor.getText().toString();
        boolean interactive = JDoodleApiHelper.isInteractiveCode(code);
        boolean hasInput = inputEditor != null && !inputEditor.getText().toString().trim().isEmpty();
        inputHintText.setVisibility(interactive && !hasInput ? View.VISIBLE : View.GONE);
    }

    private String generateSampleInput(String code) {
        StringBuilder input = new StringBuilder();
        int intCount = countOccurrences(code, "nextInt()");
        int doubleCount = countOccurrences(code, "nextDouble()");
        int lineCount = countOccurrences(code, "nextLine()");
        int wordCount = countOccurrences(code, "next()"); // next()
        for (int i = 0; i < intCount; i++) { input.append(10 + i * 5).append("\n"); }
        for (int i = 0; i < doubleCount; i++) { input.append(String.format(java.util.Locale.US, "%.2f", 1.5 + i * 0.5)).append("\n"); }
        for (int i = 0; i < lineCount; i++) { input.append("Sample Text ").append(i + 1).append("\n"); }
        for (int i = 0; i < wordCount; i++) { input.append("word").append(i + 1).append("\n"); }
        if (input.length() == 0 && code.contains("Scanner")) {
            input.append("10\n5\nHello\n");
        }
        return input.toString();
    }

    private int countOccurrences(String text, String token) {
        int count = 0; int idx = 0; if (text == null || token == null || token.isEmpty()) return 0;
        while ((idx = text.indexOf(token, idx)) != -1) { count++; idx += token.length(); }
        return count;
    }
    
    private void executeCodeOnJDoodle(String code, String stdin) {
        if (JDoodleApiHelper.DEMO_MODE) {
            // Demo mode - simulate API response
            executeDemoMode(code, stdin);
            return;
        }
        
        try {
            // Create JSON request body for JDoodle
            String requestBody = JDoodleApiHelper.createSubmissionRequest(code, "java", stdin);
            
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody, JSON);
            
            // Convert Map to Headers
            Map<String, String> headerMap = JDoodleApiHelper.getApiHeaders();
            Headers headers = Headers.of(headerMap);
            
            Request request = new Request.Builder()
                    .url(JDoodleApiHelper.JDOODLE_API_URL)
                    .post(body)
                    .headers(headers)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        outputText.setText("Error: " + e.getMessage());
                    });
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    mainHandler.post(() -> {
                        showLoading(false);
                        handleJDoodleResponse(responseBody);
                    });
                }
            });
            
        } catch (Exception e) {
            showLoading(false);
            outputText.setText("Error: " + e.getMessage());
        }
    }
    
    private void executeDemoMode(String code, String stdin) {
        // Simulate API delay
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            mainHandler.post(() -> {
                showLoading(false);
                try {
                    String jsonResult = JDoodleApiHelper.getMockSuccessResponse(code, stdin);
                    JSONObject result = new JSONObject(jsonResult);
                    displayResult(result);
                } catch (JSONException e) {
                    outputText.setText("Error: " + e.getMessage());
                }
            });
        }).start();
    }
    
    private void handleJDoodleResponse(String responseBody) {
        try {
            JSONObject response = new JSONObject(responseBody);
            
            // JDoodle returns direct results (no token system)
            if (response.has("output") || response.has("error")) {
                displayResult(response);
            } else {
                outputText.setText("Error: Invalid response from server");
            }
            
        } catch (JSONException e) {
            outputText.setText("Error parsing response: " + e.getMessage());
        }
    }
    

    
    private void displayResult(JSONObject result) {
        String output = result.optString("output", "");
        String error = result.optString("error", "");
        if (error == null || error.trim().isEmpty() || "null".equalsIgnoreCase(error.trim())) {
            error = "";
        }

        String textToShow;
        if (!error.isEmpty()) {
            // Show only the error text when present
            textToShow = error.trim();
        } else if (!output.trim().isEmpty()) {
            // Show only program output
            textToShow = output.trim();
        } else {
            textToShow = "No output produced.";
        }

        outputText.setText(textToShow);
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRun.setEnabled(!show);
    }
    
    private void showNetworkErrorDialog(String errorMessage) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_network_error, null);
        
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();
        
        // Set error message
        android.widget.TextView errorText = dialogView.findViewById(R.id.networkErrorMessage);
        errorText.setText("Network Error: " + errorMessage);
        
        // Retry button
        android.widget.Button retryButton = dialogView.findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            dialog.dismiss();
            // Retry the operation
            runCode();
        });
        
        // Cancel button
        android.widget.Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Auto-save code when activity is paused
        saveCodeToFirebase();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Final save before destruction
        saveCodeToFirebase();
    }
    
    @Override
    public void onBackPressed() {
        // Save code before going back
        saveCodeToFirebase();
        super.onBackPressed();
    }
    
    /**
     * Save current code to Firebase
     */
    private void saveCodeToFirebase() {
        if (codeEditor == null || classCode.isEmpty() || studentId.isEmpty() || lessonNameKey.equals("default")) {
            return; // Don't save if we don't have required data
        }
        
        String code = codeEditor.getText().toString();
        if (code.trim().isEmpty()) {
            return; // Don't save empty code
        }
        
        System.out.println("💾 CompilerModeActivity: Saving code to Firebase");
        System.out.println("💾 CompilerModeActivity: Class: " + classCode + ", Student: " + studentId + ", Lesson: " + lessonNameKey);
        
        // Save to Firebase under StudentCode collection
        db.collection("Classes").document(classCode)
          .collection("StudentCode").document(studentId + "_" + lessonNameKey)
          .set(Map.of(
              "studentId", studentId,
              "lessonName", lessonNameKey,
              "code", code,
              "lastSaved", System.currentTimeMillis(),
              "timestamp", com.google.firebase.Timestamp.now()
          ))
          .addOnSuccessListener(aVoid -> {
              System.out.println("✅ CompilerModeActivity: Code saved successfully to Firebase");
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ CompilerModeActivity: Failed to save code to Firebase: " + e.getMessage());
          });
    }
    
    /**
     * Restore saved code from Firebase
     */
    private void restoreCodeFromFirebase() {
        if (classCode.isEmpty() || studentId.isEmpty() || lessonNameKey.equals("default")) {
            return; // Don't restore if we don't have required data
        }
        
        System.out.println("📥 CompilerModeActivity: Restoring code from Firebase");
        System.out.println("📥 CompilerModeActivity: Class: " + classCode + ", Student: " + studentId + ", Lesson: " + lessonNameKey);
        
        db.collection("Classes").document(classCode)
          .collection("StudentCode").document(studentId + "_" + lessonNameKey)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                  String savedCode = documentSnapshot.getString("code");
                  if (savedCode != null && !savedCode.trim().isEmpty()) {
                      System.out.println("✅ CompilerModeActivity: Code restored from Firebase");
                      codeEditor.setText(savedCode);
                  } else {
                      System.out.println("📝 CompilerModeActivity: No saved code found, using default");
                  }
              } else {
                  System.out.println("📝 CompilerModeActivity: No saved code found, using default");
              }
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ CompilerModeActivity: Failed to restore code from Firebase: " + e.getMessage());
          });
    }
    
    /**
     * Submit code for teacher review
     */
    private void submitForReview() {
        if (classCode.isEmpty() || studentId.isEmpty() || lessonNameKey.equals("default")) {
            Toast.makeText(this, "Cannot submit: Missing class or lesson information", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String code = codeEditor.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Please write some code before submitting", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get student name (signup display name preferred)
        final String studentName = resolveStudentName();
        
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Submit for Review")
            .setMessage("Are you sure you want to submit your code for teacher review? You won't be able to edit it after submission.")
            .setPositiveButton("Submit", (dialog, which) -> {
                // Save submission to Firebase
                saveSubmissionToFirebase(code, studentName);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Save submission to Firebase for teacher review
     */
    private void saveSubmissionToFirebase(String code, String studentName) {
        String submissionId = studentId + "_" + lessonNameKey;
        
        System.out.println("📤 CompilerModeActivity: Submitting code for review");
        System.out.println("📤 CompilerModeActivity: Class: " + classCode + ", Student: " + studentId + ", Lesson: " + lessonNameKey);
        
        Map<String, Object> submission = new HashMap<>();
        submission.put("studentId", studentId);
        submission.put("studentName", studentName);
        submission.put("lessonName", lessonNameKey);
        submission.put("code", code);
        submission.put("status", "pending_review");
        submission.put("submittedAt", System.currentTimeMillis());
        submission.put("timestamp", com.google.firebase.Timestamp.now());
        submission.put("grade", null);
        submission.put("feedback", null);
        submission.put("gradedAt", null);
        
        db.collection("Classes").document(classCode)
          .collection("CompilerSubmissions").document(submissionId)
          .set(submission)
          .addOnSuccessListener(aVoid -> {
              System.out.println("✅ CompilerModeActivity: Code submitted successfully for review");
              Toast.makeText(this, "Code submitted for teacher review!", Toast.LENGTH_LONG).show();
              
              // Update UI to show submission status
              showSubmissionStatus("pending_review", null, null);
              
              // Disable submit button (can only submit once)
              btnSubmit.setEnabled(false);
              btnSubmit.setText("Submitted for Review");
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ CompilerModeActivity: Failed to submit code: " + e.getMessage());
              Toast.makeText(this, "Failed to submit code. Please try again.", Toast.LENGTH_SHORT).show();
          });
    }
    
    /**
     * Check existing submission status
     */
    private void checkSubmissionStatus() {
        if (classCode.isEmpty() || studentId.isEmpty() || lessonNameKey.equals("default")) {
            return;
        }
        
        String submissionId = studentId + "_" + lessonNameKey;
        
        db.collection("Classes").document(classCode)
          .collection("CompilerSubmissions").document(submissionId)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                  String status = documentSnapshot.getString("status");
                  Long grade = documentSnapshot.getLong("grade");
                  String feedback = documentSnapshot.getString("feedback");
                  
                  if (status != null) {
                      showSubmissionStatus(status, grade != null ? grade.intValue() : null, feedback);
                      
                      // Disable submit button if already submitted
                      if ("pending_review".equals(status) || "graded".equals(status)) {
                          btnSubmit.setEnabled(false);
                          if ("pending_review".equals(status)) {
                              btnSubmit.setText("Submitted for Review");
                          } else if ("graded".equals(status)) {
                              btnSubmit.setText("Already Graded");
                          }
                      }
                  }
              }
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ CompilerModeActivity: Failed to check submission status: " + e.getMessage());
          });
    }
    
    /**
     * Show submission status to student
     */
    private void showSubmissionStatus(String status, Integer grade, String feedback) {
        if (submissionStatusCard == null) return;
        
        submissionStatusCard.setVisibility(View.VISIBLE);
        
        if ("pending_review".equals(status)) {
            submissionStatusTitle.setText("Submission Status: Pending Review");
            submissionStatusText.setText("Your code has been submitted and is waiting for teacher review.");
        } else if ("graded".equals(status) && grade != null) {
            submissionStatusTitle.setText("Submission Status: Graded");
            String statusText = "Grade: " + grade + "/100";
            if (feedback != null && !feedback.trim().isEmpty()) {
                statusText += "\n\nTeacher Feedback:\n" + feedback;
            }
            submissionStatusText.setText(statusText);
        }
    }
} 