package com.example.codeclash;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeacherApprovalActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private ResilientFirebaseHelper resilientHelper;
    private String classCode;
    private String teacherUID;
    
    private RecyclerView joinRequestsRecyclerView;
    private RecyclerView leaveRequestsRecyclerView;
    private JoinRequestAdapter joinRequestAdapter;
    private LeaveRequestAdapter leaveRequestAdapter;
    
    private List<JoinRequest> joinRequests = new ArrayList<>();
    private List<LeaveRequest> leaveRequests = new ArrayList<>();
    
    private ListenerRegistration joinRequestsListener;
    private ListenerRegistration leaveRequestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_approval);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        resilientHelper = new ResilientFirebaseHelper(this);
        teacherUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Get class code from intent
        classCode = getIntent().getStringExtra("classCode");
        if (classCode == null) {
            Toast.makeText(this, "Error: No class code provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI
        initializeViews();
        setupRecyclerViews();
        
        // Load requests
        loadJoinRequests();
        loadLeaveRequests();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Start real-time listeners
        startJoinRequestsListener();
        startLeaveRequestsListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop real-time listeners to save resources
        stopJoinRequestsListener();
        stopLeaveRequestsListener();
    }
    
    private void initializeViews() {
        joinRequestsRecyclerView = findViewById(R.id.joinRequestsRecyclerView);
        leaveRequestsRecyclerView = findViewById(R.id.leaveRequestsRecyclerView);
    }
    
    private void setupRecyclerViews() {
        // Join requests
        joinRequestAdapter = new JoinRequestAdapter(joinRequests, this::onJoinRequestAction);
        joinRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        joinRequestsRecyclerView.setAdapter(joinRequestAdapter);
        
        // Leave requests
        leaveRequestAdapter = new LeaveRequestAdapter(leaveRequests, this::onLeaveRequestAction);
        leaveRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        leaveRequestsRecyclerView.setAdapter(leaveRequestAdapter);
    }
    
    private void loadJoinRequests() {
        db.collection("Classes").document(classCode)
          .collection("JoinRequests")
          .whereEqualTo("status", "pending")
          .get()
          .addOnSuccessListener(querySnapshot -> {
              joinRequests.clear();
              for (QueryDocumentSnapshot doc : querySnapshot) {
                  JoinRequest request = new JoinRequest();
                  request.setRequestId(doc.getId());
                  request.setStudentId(doc.getString("studentId"));
                  request.setStudentName(doc.getString("studentName"));
                  Long requestTime = doc.getLong("requestTime");
                  request.setRequestTime(requestTime != null ? requestTime : 0);
                  request.setStatus(doc.getString("status"));
                  joinRequests.add(request);
              }
              joinRequestAdapter.notifyDataSetChanged();
              System.out.println("🔍 Loaded " + joinRequests.size() + " join requests");
          })
          .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to load join requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
              System.out.println("❌ Failed to load join requests: " + e.getMessage());
          });
    }
    
    private void loadLeaveRequests() {
        db.collection("Classes").document(classCode)
          .collection("LeaveRequests")
          .whereEqualTo("status", "pending")
          .get()
          .addOnSuccessListener(querySnapshot -> {
              leaveRequests.clear();
              for (QueryDocumentSnapshot doc : querySnapshot) {
                  LeaveRequest request = new LeaveRequest();
                  request.setRequestId(doc.getId());
                  request.setStudentId(doc.getString("studentId"));
                  request.setStudentName(doc.getString("studentName"));
                  Long requestTime = doc.getLong("requestTime");
                  request.setRequestTime(requestTime != null ? requestTime : 0);
                  request.setStatus(doc.getString("status"));
                  leaveRequests.add(request);
              }
              leaveRequestAdapter.notifyDataSetChanged();
              System.out.println("🔍 Loaded " + leaveRequests.size() + " leave requests");
          })
          .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to load leave requests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
              System.out.println("❌ Failed to load leave requests: " + e.getMessage());
          });
    }
    
    private void onJoinRequestAction(JoinRequest request, String action) {
        if ("approve".equals(action)) {
            approveJoinRequest(request);
        } else if ("reject".equals(action)) {
            rejectJoinRequest(request);
        }
    }
    
    private void onLeaveRequestAction(LeaveRequest request, String action) {
        if ("approve".equals(action)) {
            approveLeaveRequest(request);
        } else if ("reject".equals(action)) {
            rejectLeaveRequest(request);
        }
    }
    
    private void approveJoinRequest(JoinRequest request) {
        String studentId = request.getStudentId();
        
        // Get class info first to use class's year and block
        db.collection("Classes").document(classCode)
          .get()
          .addOnSuccessListener(classDoc -> {
              if (!classDoc.exists()) {
                  Toast.makeText(this, "Class not found", Toast.LENGTH_SHORT).show();
                  return;
              }
              
              // Get year and block from class document
              String classYear = classDoc.getString("yearLevel");
              String classBlock = classDoc.getString("block");
              
              // Fetch student's full profile from Users collection
              db.collection("Users").document(studentId).get()
                .addOnSuccessListener(userDoc -> {
                    // Get student details from user profile with fallbacks
                    String fullName = null;
                    
                    if (userDoc.exists()) {
                        fullName = userDoc.getString("fullName");
                        if (fullName == null || fullName.isEmpty()) {
                            fullName = userDoc.getString("name");
                        }
                    }
                    
                    // Use request data as fallback if profile data is missing
                    if (fullName == null || fullName.isEmpty()) {
                        fullName = request.getStudentName();
                    }
                    if (fullName == null || fullName.isEmpty()) {
                        fullName = "Student";
                    }
                    
                    // Prepare student data for class using class's year and block
                    Map<String, Object> studentData = new HashMap<>();
                    studentData.put("studentId", studentId);
                    studentData.put("userId", studentId);
                    studentData.put("fullName", fullName);
                    studentData.put("yearBlock", (classYear != null ? classYear : "N/A") + " - " + (classBlock != null ? classBlock : "N/A"));
                    studentData.put("joinedAt", System.currentTimeMillis());
                    
                    // Add student to class
                    db.collection("Classes").document(classCode)
                      .collection("Students").document(studentId)
                      .set(studentData)
                      .addOnSuccessListener(aVoid -> {
                          // Add class to student's MyJoinedClasses with class details
                          Map<String, Object> classData = new HashMap<>();
                          classData.put("classCode", classCode);
                          classData.put("joinedAt", System.currentTimeMillis());
                          
                          // Add year and block info
                          if (classYear != null) classData.put("yearLevel", classYear);
                          if (classBlock != null) classData.put("block", classBlock);
                          
                          db.collection("Users").document(studentId)
                            .collection("MyJoinedClasses").document(classCode)
                            .set(classData)
                            .addOnSuccessListener(unused -> {
                                // Update request status
                                updateJoinRequestStatus(request, "approved");
                                
                                // Send notification to student via FCM
                                FCMHelper.sendJoinRequestNotification(studentId, classCode, true);
                                
                                Toast.makeText(this, "Join request approved", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to add class to student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                      })
                      .addOnFailureListener(e -> {
                          Toast.makeText(this, "Failed to add student to class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                      });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch student profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
          })
          .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to fetch class details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          });
    }
    
    private void rejectJoinRequest(JoinRequest request) {
        String studentId = request.getStudentId();
        
        // Update request status
        updateJoinRequestStatus(request, "rejected");
        
        // Remove the class from student's MyJoinedClasses if it exists (cleanup)
        db.collection("Users").document(studentId)
          .collection("MyJoinedClasses").document(classCode)
          .delete()
          .addOnSuccessListener(aVoid -> {
              // Send notification to student via FCM
              FCMHelper.sendJoinRequestNotification(studentId, classCode, false);
              
              Toast.makeText(this, "Join request rejected", Toast.LENGTH_SHORT).show();
          })
          .addOnFailureListener(e -> {
              // Even if delete fails, still show success since request is rejected
              Toast.makeText(this, "Join request rejected", Toast.LENGTH_SHORT).show();
          });
    }
    
    private void approveLeaveRequest(LeaveRequest request) {
        String studentId = request.getStudentId();
        
        // Get class name for notification
        db.collection("Classes").document(classCode)
          .get()
          .addOnSuccessListener(classDoc -> {
              String className = classDoc.exists() ? classDoc.getString("className") : classCode;
              
              // Remove student from class
              db.collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove class from student's MyJoinedClasses
                    db.collection("Users").document(studentId)
                      .collection("MyJoinedClasses").document(classCode)
                      .delete()
                      .addOnSuccessListener(unused -> {
                          // Update request status
                          updateLeaveRequestStatus(request, "approved");
                          
                          // Send notification to student via FCM
                          FCMHelper.sendLeaveRequestNotification(studentId, classCode, className, true);
                          
                          // Clean up all request documents for this student
                          cleanupStudentRequests(studentId);
                          
                          Toast.makeText(this, "Leave request approved", Toast.LENGTH_SHORT).show();
                      })
                      .addOnFailureListener(e -> {
                          Toast.makeText(this, "Failed to remove class from student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                      });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove student from class: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
          })
          .addOnFailureListener(e -> {
              Toast.makeText(this, "Failed to fetch class info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          });
    }
    
    private void rejectLeaveRequest(LeaveRequest request) {
        // Just update status - student stays in class
        updateLeaveRequestStatus(request, "rejected");
        
        // Send notification to student via FCM
        FCMHelper.sendLeaveRequestNotification(request.getStudentId(), classCode, null, false);
        
        Toast.makeText(this, "Leave request rejected - student remains in class", Toast.LENGTH_SHORT).show();
    }
    
    private void updateJoinRequestStatus(JoinRequest request, String status) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("processedAt", System.currentTimeMillis());
        
        // Use resilient Firebase helper for better poor connection handling
        resilientHelper.updateDocument(
            "Classes/" + classCode + "/JoinRequests", 
            request.getRequestId(), 
            updateData,
            new ResilientFirebaseHelper.SimpleCallback() {
                @Override
                public void onSuccess() {
                    // Remove from local list immediately
                    joinRequests.remove(request);
                    if (joinRequestAdapter != null) {
                        joinRequestAdapter.notifyDataSetChanged();
                    }
                    System.out.println("✅ Join request " + status + " - removed from list");
                }
                
                @Override
                public void onFailure(Exception exception) {
                    System.out.println("❌ Failed to update join request status: " + 
                        (exception != null ? exception.getMessage() : "Unknown error"));
                    resilientHelper.showErrorToast(exception);
                }
                
                @Override
                public void onTimeout() {
                    System.out.println("⏰ Timeout updating join request status");
                    resilientHelper.showTimeoutDialog();
                }
            }
        );
    }
    
    private void updateLeaveRequestStatus(LeaveRequest request, String status) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", status);
        updateData.put("processedAt", System.currentTimeMillis());
        
        db.collection("Classes").document(classCode)
          .collection("LeaveRequests").document(request.getRequestId())
          .update(updateData)
          .addOnSuccessListener(aVoid -> {
              // Remove from local list immediately
              leaveRequests.remove(request);
              if (leaveRequestAdapter != null) {
                  leaveRequestAdapter.notifyDataSetChanged();
              }
              System.out.println("✅ Leave request " + status + " - removed from list");
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ Failed to update leave request status: " + e.getMessage());
          });
    }
    
    // Data classes
    public static class JoinRequest {
        private String requestId;
        private String studentId;
        private String studentName;
        private long requestTime;
        private String status;
        
        // Constructors, getters, setters
        public JoinRequest() {}
        
        public JoinRequest(String studentId, String studentName, long requestTime, String status) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.requestTime = requestTime;
            this.status = status;
        }
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public long getRequestTime() { return requestTime; }
        public void setRequestTime(long requestTime) { this.requestTime = requestTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class LeaveRequest {
        private String requestId;
        private String studentId;
        private String studentName;
        private long requestTime;
        private String status;
        
        // Constructors, getters, setters
        public LeaveRequest() {}
        
        public LeaveRequest(String studentId, String studentName, long requestTime, String status) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.requestTime = requestTime;
            this.status = status;
        }
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
        public long getRequestTime() { return requestTime; }
        public void setRequestTime(long requestTime) { this.requestTime = requestTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    private void cleanupStudentRequests(String studentId) {
        // Delete join request
        db.collection("Classes").document(classCode)
            .collection("JoinRequests").document(studentId)
            .delete()
            .addOnSuccessListener(aVoid -> System.out.println("✅ Cleaned up join request for " + studentId))
            .addOnFailureListener(e -> System.out.println("❌ Failed to clean up join request: " + e.getMessage()));
        
        // Delete leave request
        db.collection("Classes").document(classCode)
            .collection("LeaveRequests").document(studentId)
            .delete()
            .addOnSuccessListener(aVoid -> System.out.println("✅ Cleaned up leave request for " + studentId))
            .addOnFailureListener(e -> System.out.println("❌ Failed to clean up leave request: " + e.getMessage()));
    }
    
    private void startJoinRequestsListener() {
        if (joinRequestsListener != null) {
            joinRequestsListener.remove();
        }
        
        joinRequestsListener = db.collection("Classes").document(classCode)
            .collection("JoinRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    System.out.println("❌ Join requests listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null) {
                    joinRequests.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        JoinRequest request = new JoinRequest(
                            doc.getId(), // studentId
                            doc.getString("studentName"),
                            doc.getLong("requestTime") != null ? doc.getLong("requestTime") : 0,
                            doc.getString("status") != null ? doc.getString("status") : "pending"
                        );
                        request.setRequestId(doc.getId()); // Set the requestId to the document ID
                        joinRequests.add(request);
                    }
                    
                    if (joinRequestAdapter != null) {
                        joinRequestAdapter.notifyDataSetChanged();
                    }
                    
                    System.out.println("🔄 Join requests auto-reloaded: " + joinRequests.size() + " requests");
                }
            });
    }
    
    private void startLeaveRequestsListener() {
        if (leaveRequestsListener != null) {
            leaveRequestsListener.remove();
        }
        
        leaveRequestsListener = db.collection("Classes").document(classCode)
            .collection("LeaveRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    System.out.println("❌ Leave requests listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null) {
                    leaveRequests.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        LeaveRequest request = new LeaveRequest(
                            doc.getId(), // studentId
                            doc.getString("studentName"),
                            doc.getLong("requestTime") != null ? doc.getLong("requestTime") : 0,
                            doc.getString("status") != null ? doc.getString("status") : "pending"
                        );
                        request.setRequestId(doc.getId()); // Set the requestId to the document ID
                        leaveRequests.add(request);
                    }
                    
                    if (leaveRequestAdapter != null) {
                        leaveRequestAdapter.notifyDataSetChanged();
                    }
                    
                    System.out.println("🔄 Leave requests auto-reloaded: " + leaveRequests.size() + " requests");
                }
            });
    }
    
    private void stopJoinRequestsListener() {
        if (joinRequestsListener != null) {
            joinRequestsListener.remove();
            joinRequestsListener = null;
        }
    }
    
    private void stopLeaveRequestsListener() {
        if (leaveRequestsListener != null) {
            leaveRequestsListener.remove();
            leaveRequestsListener = null;
        }
    }
}
