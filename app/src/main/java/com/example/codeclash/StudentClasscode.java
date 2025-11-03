package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentClasscode extends AppCompatActivity {

    private EditText editTextClassCode, editTextYear, editTextBlock;
    private Button buttonJoinClass;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_classcode);

        firestore = FirebaseFirestore.getInstance();

        editTextClassCode = findViewById(R.id.editTextClassCode);
        editTextYear = findViewById(R.id.editTextYear);
        editTextBlock = findViewById(R.id.editTextBlock);
        buttonJoinClass = findViewById(R.id.buttonJoinClass);

        // 🔙 Back button functionality
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        buttonJoinClass.setOnClickListener(v -> joinClass());
    }

    private void joinClass() {
        String classCode = editTextClassCode.getText().toString().trim();
        String year = editTextYear.getText().toString().trim();
        String block = editTextBlock.getText().toString().trim();

        if (TextUtils.isEmpty(classCode) || TextUtils.isEmpty(year) || TextUtils.isEmpty(block)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String studentUID = currentUser.getUid();
        
        // First get the user's signup name via UserNameManager (supports Users/users and multiple fields)
        UserNameManager.getUserName(studentUID, new UserNameManager.NameCallback() {
            @Override
            public void onSuccess(String name) {
                joinClassWithName(classCode, year, block, name, studentUID);
            }
            
            @Override
            public void onFailure(String fallbackName) {
                System.out.println("❌ Failed to load user name, using fallback");
                joinClassWithName(classCode, year, block, fallbackName, studentUID);
            }
        });
    }
    
    private void joinClassWithName(String classCode, String year, String block, String signupName, String studentUID) {
        DocumentReference classRef = firestore.collection("Classes").document(classCode);

        classRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Check if student is already enrolled
                checkExistingEnrollment(classCode, studentUID, signupName, year, block);
            } else {
                Toast.makeText(StudentClasscode.this, "Class code does not exist", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(StudentClasscode.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
    
    private void checkExistingEnrollment(String classCode, String studentUID, String signupName, String year, String block) {
        // Check if student is already enrolled in this class
        firestore.collection("Users").document(studentUID)
                .collection("MyJoinedClasses").document(classCode)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        Toast.makeText(this, "You are already enrolled in this class", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Check if student has any pending join request
                    checkPendingJoinRequest(classCode, studentUID, signupName, year, block);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking enrollment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void checkPendingJoinRequest(String classCode, String studentUID, String signupName, String year, String block) {
        firestore.collection("Classes").document(classCode)
                .collection("JoinRequests").document(studentUID)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        String status = docSnapshot.getString("status");
                        if ("pending".equals(status)) {
                            Toast.makeText(this, "You already have a pending join request for this class", Toast.LENGTH_LONG).show();
                        } else if ("approved".equals(status)) {
                            Toast.makeText(this, "Your join request was already approved. Please refresh the app.", Toast.LENGTH_LONG).show();
                        } else if ("rejected".equals(status)) {
                            // Allow new request after rejection
                            sendJoinRequest(classCode, studentUID, signupName, year, block);
                        }
                    } else {
                        // No existing request, send new one
                        sendJoinRequest(classCode, studentUID, signupName, year, block);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking join request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void sendJoinRequest(String classCode, String studentUID, String signupName, String year, String block) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("studentId", studentUID);
        requestData.put("studentName", signupName);
        requestData.put("yearBlock", year + " - " + block);
        requestData.put("requestTime", System.currentTimeMillis());
        requestData.put("status", "pending");
        
        firestore.collection("Classes").document(classCode)
                .collection("JoinRequests").document(studentUID)
                .set(requestData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Join request sent to teacher for approval", Toast.LENGTH_LONG).show();
                    
                    // Send notification to teacher via Firestore (not local notification)
                    sendJoinRequestNotificationToTeacher(classCode, signupName);
                    
                    finish(); // Go back to previous screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send join request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void sendJoinRequestNotificationToTeacher(String classCode, String studentName) {
        // Get teacher UID from class document
        firestore.collection("Classes").document(classCode).get()
            .addOnSuccessListener(classDoc -> {
                if (classDoc.exists()) {
                    String teacherUID = classDoc.getString("createdBy");
                    if (teacherUID != null) {
                        // Create notification for teacher
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "join_request");
                        notification.put("title", "New Join Request");
                        notification.put("body", studentName + " wants to join class " + classCode);
                        notification.put("classCode", classCode);
                        notification.put("studentName", studentName);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);
                        
                        firestore.collection("Users").document(teacherUID)
                            .collection("Notifications")
                            .document()
                            .set(notification)
                            .addOnSuccessListener(aVoid -> {
                                System.out.println("📬 Join request notification sent to teacher " + teacherUID);
                            })
                            .addOnFailureListener(e -> {
                                System.out.println("❌ Failed to send notification to teacher: " + e.getMessage());
                            });
                    }
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("❌ Failed to get class document: " + e.getMessage());
            });
    }
}
