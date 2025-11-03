package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LessonsActivity extends AppCompatActivity {
    private RecyclerView recyclerViewLessons;
    private LessonAdapter lessonAdapter;
    private List<String> lessonNames = new ArrayList<>();
    private boolean isTeacher = false;
    private String classCode;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lessons);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get user role and class code from intent
        isTeacher = getIntent().getBooleanExtra("isTeacher", false);
        classCode = getIntent().getStringExtra("classCode");
        System.out.println("🔍 LessonsActivity: isTeacher = " + isTeacher + ", classCode = " + classCode);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();
        
        recyclerViewLessons = findViewById(R.id.recyclerViewLessons);
        recyclerViewLessons.setLayoutManager(new LinearLayoutManager(this));
        lessonAdapter = new LessonAdapter(lessonNames, isTeacher, this, classCode, currentUserId);
        recyclerViewLessons.setAdapter(lessonAdapter);

        // Setup refresh button
        FloatingActionButton fabRefresh = findViewById(R.id.fabRefresh);
        fabRefresh.setOnClickListener(v -> refreshLessons());

        if (classCode != null) {
            loadLessons();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh attempt data when returning to the activity
        // This ensures students see updated attempts after teacher changes
        if (classCode != null && !isTeacher) {
            System.out.println("🔄 LessonsActivity: Refreshing attempt data on resume");
            refreshAttemptData();
        }
    }
    
    private void refreshLessons() {
        System.out.println("🔄 LessonsActivity: Refreshing lessons...");
        
        // Show toast feedback
        Toast.makeText(this, "Refreshing lessons...", Toast.LENGTH_SHORT).show();
        
        // Show loading indicator
        if (lessonAdapter != null) {
            lessonAdapter.notifyDataSetChanged();
        }
        
        // Reload lessons from Firestore
        loadLessons();
        
        // Also refresh attempt data for students
        if (!isTeacher) {
            refreshAttemptData();
        }
    }
    
    private void refreshAttemptData() {
        // Refresh attempt data for all lessons
        if (lessonAdapter != null) {
            lessonAdapter.refreshAttemptData();
        }
    }
    
    public void refreshOpenDialogs() {
        System.out.println("🔄 LessonsActivity: Refreshing any open dialogs");
        // This method can be called to refresh attempt data in any open dialogs
        // For now, we'll rely on the dialog being reopened to get fresh data
    }

    private void loadLessons() {
        // Check network connectivity first
        if (!NetworkManager.isNetworkAvailable(this)) {
            NetworkManager.showOfflineMessage(this);
            // Still try to load from cache (offline persistence)
        }
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Classes").document(classCode).get()
            .addOnSuccessListener(documentSnapshot -> {
                lessonNames.clear();
                Object lessonsObj = documentSnapshot.get("lessons");
                if (lessonsObj instanceof List<?>) {
                    List<?> lessons = (List<?>) lessonsObj;
                    for (Object lessonItem : lessons) {
                        String lessonName = null;
                        if (lessonItem instanceof Map) {
                            Map<String, Object> lessonMap = (Map<String, Object>) lessonItem;
                            lessonName = (String) lessonMap.get("name");
                        } else if (lessonItem instanceof String) {
                            lessonName = (String) lessonItem;
                        }
                        if (lessonName != null) {
                            lessonNames.add(lessonName);
                        }
                    }
                }
                lessonAdapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load lessons: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}

class LessonAdapter extends RecyclerView.Adapter<LessonViewHolder> {
    private List<String> lessons;
    private boolean isTeacher;
    private LessonsActivity activity;
    private String classCode;
    private String currentUserId;
    
    public LessonAdapter(List<String> lessons, boolean isTeacher, LessonsActivity activity, 
                        String classCode, String currentUserId) { 
        this.lessons = lessons; 
        this.isTeacher = isTeacher;
        this.activity = activity;
        this.classCode = classCode;
        this.currentUserId = currentUserId;
    }
    
    @Override
    public LessonViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
        android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesson, parent, false);
        return new LessonViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(LessonViewHolder holder, int position) {
        String lessonName = lessons.get(position);
        holder.lessonName.setText(lessonName);
        
        // Set initial status
        holder.lessonStatus.setText("Loading...");
        holder.lessonStatus.setTextColor(activity.getResources().getColor(android.R.color.darker_gray));
        
        // Check lesson status
        if (isTeacher) {
            // Teacher sees lesson management options
            holder.lessonStatus.setText("Teacher Controls");
            holder.lessonStatus.setTextColor(activity.getResources().getColor(R.color.red_500));
        } else {
            // Student sees lesson status
            LessonManager.getLessonStatus(classCode, lessonName, currentUserId, 
                new LessonManager.OnLessonStatusCallback() {
                    @Override
                    public void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted) {
                        activity.runOnUiThread(() -> {
                            if (isCompleted) {
                                holder.lessonStatus.setText("✓ Completed");
                                holder.lessonStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                            } else if (lessonStatus.equals(LessonManager.STATUS_LOCKED)) {
                                holder.lessonStatus.setText("🔒 Locked");
                                holder.lessonStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                            } else {
                                holder.lessonStatus.setText("📚 Available");
                                holder.lessonStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_blue_dark));
                            }
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        activity.runOnUiThread(() -> {
                            holder.lessonStatus.setText("Error");
                            holder.lessonStatus.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                        });
                    }
                });
        }
        
        // Handle click based on user role
        holder.itemView.setOnClickListener(v -> {
            System.out.println("🔍 LessonsActivity: Lesson clicked: " + lessonName + ", isTeacher: " + isTeacher);
            if (isTeacher) {
                System.out.println("🔍 LessonsActivity: Showing teacher options for: " + lessonName);
                showTeacherOptions(lessonName);
            } else {
                // Check if lesson is locked before allowing access
                LessonManager.getLessonStatus(classCode, lessonName, currentUserId, 
                    new LessonManager.OnLessonStatusCallback() {
                        @Override
                        public void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted) {
                            if (lessonStatus.equals(LessonManager.STATUS_LOCKED)) {
                                activity.runOnUiThread(() -> {
                                    Toast.makeText(activity, "This lesson is locked by your teacher", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                activity.runOnUiThread(() -> {
                                    showModeSelectionDialog(lessonName);
                                });
                            }
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(activity, "Error checking lesson status", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
            }
        });
    }
    
    // Helper method to convert database lesson names to Godot-expected format
    private String mapLessonNameToGodot(String databaseLessonName) {
        switch (databaseLessonName) {
            case "INTRODUCTION TO JAVA": return "lesson 1";
            case "VARIABLES and DATA": return "lesson 2";
            case "OPERATORS and EXPRESSIONS": return "lesson 3";
            case "CONDITIONAL STATEMENTS": return "lesson 4";
            case "CONDITIONAL LOOPS": return "lesson 5";
            case "ARRAYS": return "lesson 6";
            default: return databaseLessonName; // Fallback to original
        }
    }
    
    private void showTeacherOptions(String lessonName) {
        System.out.println("🔍 LessonsActivity: showTeacherOptions called for: " + lessonName);
        
        // Create custom dialog with prominent UI
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_lesson_management, null);
        builder.setView(dialogView);
        
        // Set lesson name
        TextView lessonNameText = dialogView.findViewById(R.id.textLessonName);
        lessonNameText.setText(lessonName);
        
        // Get current lesson status
        LessonManager.getLessonStatus(classCode, lessonName, currentUserId, new LessonManager.OnLessonStatusCallback() {
            @Override
            public void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted) {
                activity.runOnUiThread(() -> {
                    TextView statusText = dialogView.findViewById(R.id.textLessonStatus);
                    if (lessonStatus.equals(LessonManager.STATUS_LOCKED)) {
                        statusText.setText("Locked");
                        statusText.setTextColor(activity.getResources().getColor(android.R.color.holo_red_dark));
                    } else {
                        statusText.setText("Unlocked");
                        statusText.setTextColor(activity.getResources().getColor(android.R.color.holo_green_dark));
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                activity.runOnUiThread(() -> {
                    TextView statusText = dialogView.findViewById(R.id.textLessonStatus);
                    statusText.setText("Unknown");
                    statusText.setTextColor(activity.getResources().getColor(android.R.color.darker_gray));
                });
            }
        });
        
        // Setup click listeners
        dialogView.findViewById(R.id.cardLessonStatus).setOnClickListener(v -> {
            // Toggle lesson status
            LessonManager.getLessonStatus(classCode, lessonName, currentUserId, new LessonManager.OnLessonStatusCallback() {
                @Override
                public void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted) {
                    activity.runOnUiThread(() -> {
                        if (lessonStatus.equals(LessonManager.STATUS_LOCKED)) {
                            LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_UNLOCKED);
                            Toast.makeText(activity, "Lesson unlocked", Toast.LENGTH_SHORT).show();
                        } else {
                            LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_LOCKED);
                            Toast.makeText(activity, "Lesson locked", Toast.LENGTH_SHORT).show();
                        }
                        notifyDataSetChanged(); // Refresh the list
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity, "Failed to check lesson status", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        
        dialogView.findViewById(R.id.cardManageAttempts).setOnClickListener(v -> {
            System.out.println("🔍 LessonsActivity: Opening attempt management for: " + lessonName);
            showAttemptManagementOptions(lessonName);
        });
        
        // Remove analytics card - not needed
        dialogView.findViewById(R.id.cardAnalytics).setVisibility(View.GONE);
        
        // Setup buttons
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            // Dialog will be dismissed automatically
        });
        
        dialogView.findViewById(R.id.btnQuickActions).setOnClickListener(v -> {
            showQuickActionsDialog(lessonName);
        });
        
        System.out.println("🔍 LessonsActivity: Showing prominent teacher options dialog");
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showQuickActionsDialog(String lessonName) {
        System.out.println("🔍 LessonsActivity: showQuickActionsDialog called for: " + lessonName);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Quick Actions for " + lessonName);
        
        String[] quickActions = {
            "🔓 Unlock Lesson",
            "🔒 Lock Lesson", 
            "👥 Manage Quiz Attempts",
            "👥 Manage Code Builder Attempts"
        };
        
        builder.setItems(quickActions, (dialog, which) -> {
            System.out.println("🔍 LessonsActivity: Quick action selected: " + which);
            switch (which) {
                case 0: // Unlock
                    LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_UNLOCKED);
                    Toast.makeText(activity, "Lesson unlocked", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    break;
                case 1: // Lock
                    LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_LOCKED);
                    Toast.makeText(activity, "Lesson locked", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                    break;
                case 2: // Quiz Attempts
                    Intent intent = new Intent(activity, StudentAttemptManagementActivity.class);
                    intent.putExtra("classCode", classCode);
                    intent.putExtra("lessonName", lessonName);
                    intent.putExtra("activityType", "quiz");
                    activity.startActivity(intent);
                    break;
                case 3: // Code Builder Attempts
                    Intent intent2 = new Intent(activity, StudentAttemptManagementActivity.class);
                    intent2.putExtra("classCode", classCode);
                    intent2.putExtra("lessonName", lessonName);
                    intent2.putExtra("activityType", "code_builder");
                    activity.startActivity(intent2);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        System.out.println("🔍 LessonsActivity: Showing quick actions dialog");
        builder.show();
    }
    
    private void showAttemptManagementOptions(String lessonName) {
        System.out.println("🔍 LessonsActivity: showAttemptManagementOptions called for: " + lessonName);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Manage Attempts for: " + lessonName);
        
        // Use setSingleChoiceItems instead of setItems for better visibility
        String[] activities = {"Quiz Attempts", "Code Builder Attempts"};
        System.out.println("🔍 LessonsActivity: Activities array length: " + activities.length);
        
        builder.setSingleChoiceItems(activities, -1, (dialog, which) -> {
            String activityType = (which == 0) ? "quiz" : "code_builder";
            System.out.println("🔍 LessonsActivity: Activity selected: " + activityType + " (index: " + which + ")");
            dialog.dismiss(); // Close the dialog
            
            // Open the dedicated attempt management activity
            Intent intent = new Intent(activity, StudentAttemptManagementActivity.class);
            intent.putExtra("classCode", classCode);
            intent.putExtra("lessonName", lessonName);
            intent.putExtra("activityType", activityType);
            activity.startActivity(intent);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            System.out.println("🔍 LessonsActivity: Activity selection cancelled");
            dialog.dismiss();
        });
        
        System.out.println("🔍 LessonsActivity: Showing activity selection dialog");
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showStudentAttemptList(String lessonName, String activityType) {
        System.out.println("🔍 LessonsActivity: Loading students for class: " + classCode);
        
        // Load students from the class
        FirebaseFirestore.getInstance()
                .collection("Classes").document(classCode)
                .collection("Students")
                .get()
                .addOnSuccessListener(studentSnapshot -> {
                    System.out.println("🔍 LessonsActivity: Found " + studentSnapshot.size() + " students");
                    
                    if (studentSnapshot.isEmpty()) {
                        System.out.println("🔍 LessonsActivity: No students found in class");
                        Toast.makeText(activity, "No students in this class", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Create list of students with their attempt data
                    List<StudentAttemptInfo> students = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot studentDoc : studentSnapshot) {
                        StudentAttemptInfo student = new StudentAttemptInfo();
                        student.studentId = studentDoc.getId();
                        student.studentName = studentDoc.getString("fullName");
                        if (student.studentName == null) student.studentName = "Unknown Student";
                        students.add(student);
                        System.out.println("🔍 LessonsActivity: Added student: " + student.studentName + " (ID: " + student.studentId + ")");
                    }
                    
                    System.out.println("🔍 LessonsActivity: Showing dialog with " + students.size() + " students");
                    showStudentAttemptDialog(lessonName, activityType, students);
                })
                .addOnFailureListener(e -> {
                    System.out.println("🔍 LessonsActivity: Failed to load students: " + e.getMessage());
                    Toast.makeText(activity, "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showStudentAttemptDialog(String lessonName, String activityType, List<StudentAttemptInfo> students) {
        System.out.println("🔍 LessonsActivity: showStudentAttemptDialog called with " + students.size() + " students");
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Manage Attempts - " + lessonName + " (" + activityType + ")");
        
        // Create a simple list view with student names
        String[] studentNames = new String[students.size()];
        for (int i = 0; i < students.size(); i++) {
            studentNames[i] = students.get(i).studentName;
            System.out.println("🔍 LessonsActivity: Student " + i + ": " + studentNames[i]);
        }
        
        builder.setItems(studentNames, (dialog, which) -> {
            StudentAttemptInfo selectedStudent = students.get(which);
            System.out.println("🔍 LessonsActivity: Student selected: " + selectedStudent.studentName);
            showIndividualStudentAttemptControls(lessonName, activityType, selectedStudent);
        });
        
        builder.setNegativeButton("Cancel", null);
        System.out.println("🔍 LessonsActivity: Showing student list dialog");
        builder.show();
    }
    
    private void showIndividualStudentAttemptControls(String lessonName, String activityType, StudentAttemptInfo student) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Manage Attempts for: " + student.studentName);
        builder.setMessage("Current attempts: " + student.attemptsUsed + "/3\n\nWhat would you like to do?");
        
        String[] options = {"Add 1 Attempt", "Subtract 1 Attempt", "Reset to 0 Attempts"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Add attempt
                    addAttemptForStudent(lessonName, activityType, student);
                    break;
                case 1: // Subtract attempt
                    subtractAttemptForStudent(lessonName, activityType, student);
                    break;
                case 2: // Reset attempts
                    resetAttemptsForStudent(lessonName, activityType, student);
                    break;
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void addAttemptForStudent(String lessonName, String activityType, StudentAttemptInfo student) {
        LeaderboardManager.teacherIncreaseAttempts(classCode, lessonName, activityType, 
                student.studentId, com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid(),
                new LeaderboardManager.AttemptUpdateCallback() {
                    @Override
                    public void onSuccess(int newAttemptCount) {
                        Toast.makeText(activity, "Added attempt for " + student.studentName + " (Total: " + newAttemptCount + ")", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(activity, "Failed to add attempt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void subtractAttemptForStudent(String lessonName, String activityType, StudentAttemptInfo student) {
        LeaderboardManager.teacherDecreaseAttempts(classCode, lessonName, activityType, 
                student.studentId, com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid(),
                new LeaderboardManager.AttemptUpdateCallback() {
                    @Override
                    public void onSuccess(int newAttemptCount) {
                        Toast.makeText(activity, "Subtracted attempt for " + student.studentName + " (Total: " + newAttemptCount + ")", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(activity, "Failed to subtract attempt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void resetAttemptsForStudent(String lessonName, String activityType, StudentAttemptInfo student) {
        LeaderboardManager.teacherResetAttempts(classCode, lessonName, activityType, 
                student.studentId, com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid(),
                new LeaderboardManager.AttemptUpdateCallback() {
                    @Override
                    public void onSuccess(int newAttemptCount) {
                        Toast.makeText(activity, "Reset attempts for " + student.studentName + " (Total: " + newAttemptCount + ")", Toast.LENGTH_SHORT).show();
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(activity, "Failed to reset attempts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    // Helper class for student attempt info
    private static class StudentAttemptInfo {
        String studentId;
        String studentName;
        int attemptsUsed = 0;
    }
    
    
    private void showModeSelectionDialog(String lessonName) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        android.view.View dialogView = android.view.LayoutInflater.from(activity).inflate(R.layout.dialog_mode_selection, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Set lesson name in title
        android.widget.TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        titleText.setText("Select Mode for: " + lessonName);
        
        // Load attempt counts for each mode
        loadAttemptCounts(dialogView, lessonName);
        
        // Set up button click listeners with attempt checking
        dialogView.findViewById(R.id.btnQuizMode).setOnClickListener(v -> {
            LeaderboardManager.checkAttemptsRemaining(classCode, lessonName, LeaderboardManager.ACTIVITY_QUIZ, 
                currentUserId, new LeaderboardManager.OnAttemptsCallback() {
                    @Override
                    public void onSuccess(boolean canAttempt, int attemptsUsed, int maxAttempts) {
                        if (canAttempt) {
            dialog.dismiss();
            launchQuizMode(lessonName);
                        } else {
                            Toast.makeText(activity, "No quiz attempts remaining for this lesson!", Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(activity, "Error checking attempts", Toast.LENGTH_SHORT).show();
                    }
                });
        });
        
        dialogView.findViewById(R.id.btnCodeBuilder).setOnClickListener(v -> {
            LeaderboardManager.checkAttemptsRemaining(classCode, lessonName, LeaderboardManager.ACTIVITY_CODE_BUILDER, 
                currentUserId, new LeaderboardManager.OnAttemptsCallback() {
                    @Override
                    public void onSuccess(boolean canAttempt, int attemptsUsed, int maxAttempts) {
                        if (canAttempt) {
            dialog.dismiss();
            launchCodeBuilder(lessonName);
                        } else {
                            Toast.makeText(activity, "No code builder attempts remaining for this lesson!", Toast.LENGTH_LONG).show();
                        }
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(activity, "Error checking attempts", Toast.LENGTH_SHORT).show();
                    }
                });
        });
        
        dialogView.findViewById(R.id.btnCompilerMode).setOnClickListener(v -> {
            dialog.dismiss();
            launchCompilerMode(lessonName);
        });
        
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void loadAttemptCounts(android.view.View dialogView, String lessonName) {
        android.widget.TextView quizAttemptText = dialogView.findViewById(R.id.quizAttemptCount);
        android.widget.TextView codeBuilderAttemptText = dialogView.findViewById(R.id.codeBuilderAttemptCount);
        android.view.View quizAttemptLoading = dialogView.findViewById(R.id.quizAttemptLoading);
        android.view.View codeAttemptLoading = dialogView.findViewById(R.id.codeBuilderAttemptLoading);
        if (quizAttemptLoading != null) quizAttemptLoading.setVisibility(android.view.View.VISIBLE);
        if (codeAttemptLoading != null) codeAttemptLoading.setVisibility(android.view.View.VISIBLE);
        
        // Load Quiz attempts
        LeaderboardManager.checkAttemptsRemaining(classCode, lessonName, LeaderboardManager.ACTIVITY_QUIZ, 
            currentUserId, new LeaderboardManager.OnAttemptsCallback() {
                @Override
                public void onSuccess(boolean canAttempt, int attemptsUsed, int maxAttempts) {
                        activity.runOnUiThread(() -> {
                    int remainingAttempts = maxAttempts - attemptsUsed;
                    String attemptText = remainingAttempts + "/" + maxAttempts + " attempts remaining";
                    if (!canAttempt || remainingAttempts <= 0) {
                        attemptText = "0/" + maxAttempts + " attempts remaining";
                        quizAttemptText.setTextColor(0xFFFF6B6B); // Red color
                    } else {
                        quizAttemptText.setTextColor(0xFF4CAF50); // Green color
                    }
                    quizAttemptText.setText(attemptText);
                    if (quizAttemptLoading != null) quizAttemptLoading.setVisibility(android.view.View.GONE);
                });
                }
                
                @Override
                public void onFailure(Exception e) {
                    activity.runOnUiThread(() -> {
                    quizAttemptText.setText("Error loading attempts");
                    quizAttemptText.setTextColor(0xFFFF6B6B);
                    if (quizAttemptLoading != null) quizAttemptLoading.setVisibility(android.view.View.GONE);
                    });
                }
            });
    
        // Load Code Builder attempts
        LeaderboardManager.checkAttemptsRemaining(classCode, lessonName, LeaderboardManager.ACTIVITY_CODE_BUILDER, 
            currentUserId, new LeaderboardManager.OnAttemptsCallback() {
                @Override
                public void onSuccess(boolean canAttempt, int attemptsUsed, int maxAttempts) {
                        activity.runOnUiThread(() -> {
                    int remainingAttempts = maxAttempts - attemptsUsed;
                    String attemptText = remainingAttempts + "/" + maxAttempts + " attempts remaining";
                    if (!canAttempt || remainingAttempts <= 0) {
                        attemptText = "0/" + maxAttempts + " attempts remaining";
                        codeBuilderAttemptText.setTextColor(0xFFFF6B6B); // Red color
                    } else {
                        codeBuilderAttemptText.setTextColor(0xFF4CAF50); // Green color
                    }
                    codeBuilderAttemptText.setText(attemptText);
                    if (codeAttemptLoading != null) codeAttemptLoading.setVisibility(android.view.View.GONE);
                });
                }
                
                @Override
                public void onFailure(Exception e) {
                    activity.runOnUiThread(() -> {
                    codeBuilderAttemptText.setText("Error loading attempts");
                    codeBuilderAttemptText.setTextColor(0xFFFF6B6B);
                    if (codeAttemptLoading != null) codeAttemptLoading.setVisibility(android.view.View.GONE);
                    });
                }
            });
    }
    
    private void launchQuizMode(String lessonName) {
        // Record attempt immediately before launching (attempts already checked in dialog)
        LeaderboardManager.recordAttempt(classCode, lessonName, LeaderboardManager.ACTIVITY_QUIZ, currentUserId);
        
        android.content.Intent intent = new android.content.Intent(activity, GodotHostActivity.class);
        intent.putExtra("lessonName", mapLessonNameToGodot(lessonName)); // Convert for Godot
        intent.putExtra("classCode", classCode);
        intent.putExtra("activityType", LeaderboardManager.ACTIVITY_QUIZ);
        intent.putExtra("studentId", currentUserId);
        activity.startActivity(intent);
    }
    
    private void launchCodeBuilder(String lessonName) {
        // Record attempt immediately before launching (attempts already checked in dialog)
        LeaderboardManager.recordAttempt(classCode, lessonName, LeaderboardManager.ACTIVITY_CODE_BUILDER, currentUserId);
        
        android.content.Intent intent = new android.content.Intent(activity, GodotHostActivity.class);
        intent.putExtra("lessonName", mapLessonNameToGodot(lessonName)); // Convert for Godot
        intent.putExtra("classCode", classCode);
        intent.putExtra("activityType", LeaderboardManager.ACTIVITY_CODE_BUILDER);
        intent.putExtra("studentId", currentUserId);
        activity.startActivity(intent);
    }
    
    private void launchCompilerMode(String lessonName) {
        // Record attempt immediately before launching (attempts already checked in dialog)
        LeaderboardManager.recordAttempt(classCode, lessonName, LeaderboardManager.ACTIVITY_COMPILER, currentUserId);
        
                            android.content.Intent intent = new android.content.Intent(activity, CompilerModeActivity.class);
                            intent.putExtra("lessonName", lessonName);
                            intent.putExtra("classCode", classCode);
                            activity.startActivity(intent);
    }
    
    private void showAttemptExhaustedDialog(String lessonName, String activityType, int attemptsUsed, int maxAttempts) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("No Attempts Remaining");
        builder.setMessage("You have used all " + maxAttempts + " attempts for " + activityType + " in " + lessonName + ".\n\nWould you like to request additional attempts from your teacher?");
        
        builder.setPositiveButton("Request Attempts", (dialog, which) -> {
            // Show dialog to enter reason
            showRequestAttemptsDialog(lessonName, activityType);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showRequestAttemptsDialog(String lessonName, String activityType) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Request Additional Attempts");
        builder.setMessage("Please provide a reason for requesting additional attempts:");
        
        android.widget.EditText reasonInput = new android.widget.EditText(activity);
        reasonInput.setHint("Enter your reason here...");
        builder.setView(reasonInput);
        
        builder.setPositiveButton("Submit Request", (dialog, which) -> {
            String reason = reasonInput.getText().toString().trim();
            if (!reason.isEmpty()) {
                // Get student name
                FirebaseFirestore.getInstance().collection("Classes").document(classCode)
                    .collection("Students").document(currentUserId)
                    .get()
                    .addOnSuccessListener(studentDoc -> {
                        String studentName = "Unknown Student";
                        if (studentDoc.exists()) {
                            studentName = studentDoc.getString("fullName");
                            if (studentName == null) studentName = "Unknown Student";
                        }
                        
                        LeaderboardManager.requestAdditionalAttempts(classCode, lessonName, activityType, 
                            currentUserId, studentName, reason);
                        
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity, "Request submitted to teacher", Toast.LENGTH_SHORT).show();
                        });
                    });
            } else {
                Toast.makeText(activity, "Please provide a reason", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    @Override
    public int getItemCount() { return lessons.size(); }
    
    public void refreshAttemptData() {
        System.out.println("🔄 LessonAdapter: Refreshing attempt data for all lessons");
        // Notify all items to refresh their attempt data
        notifyDataSetChanged();
        
        // Also refresh any open dialogs
        activity.refreshOpenDialogs();
    }
}

class LessonViewHolder extends RecyclerView.ViewHolder {
    TextView lessonName;
    TextView lessonStatus;
    
    public LessonViewHolder(android.view.View itemView) {
        super(itemView);
        lessonName = itemView.findViewById(R.id.lessonName);
        lessonStatus = itemView.findViewById(R.id.lessonStatus);
    }
} 