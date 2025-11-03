package com.example.codeclash;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinedClassAdapter extends RecyclerView.Adapter<JoinedClassAdapter.ClassViewHolder> {

    private List<JoinedClass> classList;
    private FirebaseFirestore db;
    private String studentUID;

    public JoinedClassAdapter(List<JoinedClass> classList) {
        this.classList = classList;
        this.db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.studentUID = currentUser != null ? currentUser.getUid() : null;
        
        // Debug logging
        System.out.println("🔧 JoinedClassAdapter: Constructor called");
        System.out.println("🔧 JoinedClassAdapter: Current user: " + (currentUser != null ? "authenticated" : "null"));
        System.out.println("🔧 JoinedClassAdapter: Student UID: " + (studentUID != null ? studentUID : "null"));
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassCode, tvYearBlock, tvTeacherName, tvClassCodeDisplay;
        View btnLessons, btnLeaveClass;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvYearBlock = itemView.findViewById(R.id.tvYearBlock);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            tvClassCodeDisplay = itemView.findViewById(R.id.tvClassCodeDisplay);

            btnLessons = itemView.findViewById(R.id.btnLessons);
            btnLeaveClass = itemView.findViewById(R.id.btnLeaveClass);
        }
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_joined_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        JoinedClass joinedClass = classList.get(position);
        holder.tvClassCode.setText(joinedClass.getClassCode());
        holder.tvYearBlock.setText("Year " + joinedClass.getYearLevel() + " • Block " + joinedClass.getBlock());
        holder.tvClassCodeDisplay.setText("Class Code: " + joinedClass.getClassCode());
        
        // Load teacher name for this class
        loadTeacherName(joinedClass.getClassCode(), holder.tvTeacherName);
        
        // Set progress (demo: 2/6 lessons completed)

        
        // Lessons button with animation
        holder.btnLessons.setOnClickListener(v -> {
            // Add subtle animation
            holder.btnLessons.setScaleX(0.95f);
            holder.btnLessons.setScaleY(0.95f);
            holder.btnLessons.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start();
            
            Intent intent = new Intent(v.getContext(), LessonsActivity.class);
            intent.putExtra("classCode", joinedClass.getClassCode());
            intent.putExtra("isTeacher", false); // Student role
            v.getContext().startActivity(intent);
        });

        // Leave class button with animation
        holder.btnLeaveClass.setOnClickListener(v -> {
            // Add subtle animation
            holder.btnLeaveClass.setScaleX(0.95f);
            holder.btnLeaveClass.setScaleY(0.95f);
            holder.btnLeaveClass.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start();
            
            showLeaveClassConfirmation(joinedClass, position, v.getContext());
        });
    }

    private void loadTeacherName(String classCode, TextView teacherTextView) {
        // First get the class document to find the teacher ID
        db.collection("Classes").document(classCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String teacherId = documentSnapshot.getString("createdBy");
                        if (teacherId != null && !teacherId.isEmpty()) {
                            // Use UserNameManager to get teacher name
                            UserNameManager.getTeacherName(teacherId, new UserNameManager.NameCallback() {
                                @Override
                                public void onSuccess(String teacherName) {
                                    teacherTextView.setText("Teacher: " + teacherName);
                                }
                                
                                @Override
                                public void onFailure(String fallbackName) {
                                    teacherTextView.setText("Teacher: " + fallbackName);
                                }
                            });
                        } else {
                            teacherTextView.setText("Teacher: Unknown");
                        }
                    } else {
                        teacherTextView.setText("Teacher: Unknown");
                    }
                })
                .addOnFailureListener(e -> {
                    teacherTextView.setText("Teacher: Unknown");
                });
    }

    private void showLeaveClassConfirmation(JoinedClass joinedClass, int position, android.content.Context context) {
        new AlertDialog.Builder(context)
            .setTitle("🚪 Leave Class")
            .setMessage("Are you sure you want to leave this class?\n\n" +
                       "Class: " + joinedClass.getClassCode() + "\n" +
                       "Year: " + joinedClass.getYearLevel() + " | Block: " + joinedClass.getBlock() + "\n\n" +
                       "You will lose access to all lessons and progress.")
            .setPositiveButton("Leave Class", (dialog, which) -> {
                leaveClass(joinedClass, position, context);
            })
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void leaveClass(JoinedClass joinedClass, int position, android.content.Context context) {
        String classCode = joinedClass.getClassCode();
        
        // Check if studentUID is valid
        if (studentUID == null || studentUID.isEmpty()) {
            System.out.println("❌ JoinedClassAdapter: studentUID is null or empty");
            Toast.makeText(context, "❌ Error: User not authenticated", Toast.LENGTH_LONG).show();
            return;
        }
        
        System.out.println("🚪 JoinedClassAdapter: Starting leave class request process");
        System.out.println("🚪 JoinedClassAdapter: Class Code: " + classCode);
        System.out.println("🚪 JoinedClassAdapter: Student UID: " + studentUID);
        
        // Check if there's already a pending leave request
        checkExistingLeaveRequest(classCode, position, context);
    }
    
    private void checkExistingLeaveRequest(String classCode, int position, android.content.Context context) {
        db.collection("Classes").document(classCode)
                .collection("LeaveRequests").document(studentUID)
                .get()
                .addOnSuccessListener(docSnapshot -> {
                    if (docSnapshot.exists()) {
                        String status = docSnapshot.getString("status");
                        if ("pending".equals(status)) {
                            Toast.makeText(context, "You already have a pending leave request for this class", Toast.LENGTH_LONG).show();
                        } else if ("approved".equals(status)) {
                            Toast.makeText(context, "Your leave request was already approved. Please refresh the app.", Toast.LENGTH_LONG).show();
                        } else if ("rejected".equals(status)) {
                            // Allow new request after rejection
                            sendLeaveRequest(classCode, position, context);
                        }
                    } else {
                        // No existing request, send new one
                        sendLeaveRequest(classCode, position, context);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error checking leave request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void sendLeaveRequest(String classCode, int position, android.content.Context context) {
        // Get student name for the request
        db.collection("Users").document(studentUID).get()
                .addOnSuccessListener(userDoc -> {
                    String studentName; // Default name
                    if (userDoc.exists()) {
                        String name = userDoc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            studentName = name;
                        } else {
                            String fullName = userDoc.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                studentName = fullName;
                            } else {
                                studentName = "Student";
                            }
                        }
                    } else {
                        studentName = "Student";
                    }
                    
                    // Create leave request
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("studentId", studentUID);
                    requestData.put("studentName", studentName);
                    requestData.put("requestTime", System.currentTimeMillis());
                    requestData.put("status", "pending");
                    
                    db.collection("Classes").document(classCode)
                            .collection("LeaveRequests").document(studentUID)
                            .set(requestData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Leave request sent to teacher for approval", Toast.LENGTH_LONG).show();
                                
                                // Send notification to teacher via Firestore
                                sendLeaveRequestNotificationToTeacher(classCode, studentName);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to send leave request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Use default name if can't get user info
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("studentId", studentUID);
                    requestData.put("studentName", "Student");
                    requestData.put("requestTime", System.currentTimeMillis());
                    requestData.put("status", "pending");
                    
                    db.collection("Classes").document(classCode)
                            .collection("LeaveRequests").document(studentUID)
                            .set(requestData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Leave request sent to teacher for approval", Toast.LENGTH_LONG).show();
                                
                                // Send notification to teacher via Firestore
                                sendLeaveRequestNotificationToTeacher(classCode, "Student");
                            })
                            .addOnFailureListener(err -> {
                                Toast.makeText(context, "Failed to send leave request: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }
    
    private void sendLeaveRequestNotificationToTeacher(String classCode, String studentName) {
        // Get teacher UID from class document
        db.collection("Classes").document(classCode).get()
            .addOnSuccessListener(classDoc -> {
                if (classDoc.exists()) {
                    String teacherUID = classDoc.getString("createdBy");
                    if (teacherUID != null) {
                        // Create notification for teacher
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "leave_request");
                        notification.put("title", "New Leave Request");
                        notification.put("body", studentName + " wants to leave class " + classCode);
                        notification.put("classCode", classCode);
                        notification.put("studentName", studentName);
                        notification.put("timestamp", System.currentTimeMillis());
                        notification.put("read", false);
                        
                        db.collection("Users").document(teacherUID)
                            .collection("Notifications")
                            .document()
                            .set(notification)
                            .addOnSuccessListener(aVoid -> {
                                System.out.println("📬 Leave request notification sent to teacher " + teacherUID);
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

    @Override
    public int getItemCount() {
        return classList.size();
    }
    
    /**
     * Refresh the student UID in case authentication state changed
     */
    public void refreshStudentUID() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.studentUID = currentUser != null ? currentUser.getUid() : null;
        System.out.println("🔄 JoinedClassAdapter: Refreshed student UID: " + (studentUID != null ? studentUID : "null"));
    }
}
