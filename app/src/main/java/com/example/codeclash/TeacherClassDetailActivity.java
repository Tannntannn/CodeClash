package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.formatter.ValueFormatter;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TeacherClassDetailActivity extends AppCompatActivity {

    private String classCode;
    private String className;
    private RecyclerView studentsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StudentAdapter studentAdapter;
    private List<Student> studentsList = new ArrayList<>();
    private List<Student> filteredStudentsList = new ArrayList<>();
    private TextInputEditText searchEditText;
    private ListenerRegistration studentsListener;
    private Map<String, ListenerRegistration> studentScoreListeners = new HashMap<>();
    private Set<String> expandedStudentIds = new HashSet<>();
    private List<String> lessonNames = new ArrayList<>(); // Store lessons in order from Firestore

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_class_detail);

        // Get class information from intent
        classCode = getIntent().getStringExtra("classCode");
        className = getIntent().getStringExtra("className");

        // Set up UI - using toolbar title instead
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(className + " (" + classCode + ")");
        }

        studentsRecyclerView = findViewById(R.id.recyclerViewStudents);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup search functionality
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Setup SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.teacherSwipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            System.out.println("🔍 TeacherClassDetail: Refresh triggered");
            refreshData();
        });

        // Ownership verification will load students upon success

        // Set up toolbar with back button
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(android.graphics.Color.WHITE);
        }

        // Setup Manage Requests button
        Button manageRequestsButton = findViewById(R.id.manageRequestsButton);
        manageRequestsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherApprovalActivity.class);
            intent.putExtra("classCode", classCode);
            startActivity(intent);
        });

        // Verify ownership before loading
        verifyOwnershipAndInit();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check for teacher notifications (join/leave requests)
        checkForTeacherNotifications();
        // Start real-time listener for students
        startStudentsListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop real-time listener to save resources
        stopStudentsListener();
    }
    
    private void checkForTeacherNotifications() {
        String teacherUID = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (teacherUID == null) return;
        
        FirebaseFirestore.getInstance()
            .collection("Users")
            .document(teacherUID)
            .collection("Notifications")
            .whereEqualTo("read", false)
            .whereEqualTo("classCode", classCode)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String type = doc.getString("type");
                    String title = doc.getString("title");
                    String body = doc.getString("body");
                    String studentName = doc.getString("studentName");
                    
                    System.out.println("🔔 Teacher notification: type=" + type + ", title=" + title);
                    
                    // Show Toast notification
                    if (body != null) {
                        Toast.makeText(this, body, Toast.LENGTH_LONG).show();
                    }
                    
                    // Show local notification based on type
                    if ("join_request".equals(type)) {
                        NotificationHelper.showJoinRequestNotification(this, classCode, studentName != null ? studentName : "Student");
                    } else if ("leave_request".equals(type)) {
                        NotificationHelper.showLeaveRequestNotification(this, classCode, studentName != null ? studentName : "Student");
                    }
                    
                    // Mark as read
                    doc.getReference().update("read", true);
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("🔔 Failed to check teacher notifications: " + e.getMessage());
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_teacher_class_detail, menu);
        
        // Setup Manage Attempts button
        MenuItem manageItem = menu.findItem(R.id.action_manage_attempts);
        android.view.View manageActionView = manageItem != null ? manageItem.getActionView() : null;
        if (manageActionView != null) {
            android.view.View btn = manageActionView.findViewById(R.id.btnManageAttempts);
            android.view.View target = btn != null ? btn : manageActionView;
            target.setOnClickListener(v -> {
                showAttemptManagementOptions();
            });
        }
        
        // Setup View Chart button
        MenuItem chartItem = menu.findItem(R.id.action_view_chart);
        android.view.View chartActionView = chartItem != null ? chartItem.getActionView() : null;
        if (chartActionView != null) {
            chartActionView.setOnClickListener(v -> {
                showPassingRateChart();
            });
        }
        
        // Setup Lesson Management button
        MenuItem lessonItem = menu.findItem(R.id.action_lesson_management);
        android.view.View lessonActionView = lessonItem != null ? lessonItem.getActionView() : null;
        if (lessonActionView != null) {
            lessonActionView.setOnClickListener(v -> {
                showLessonManagementOptions();
            });
        }
        
        // Setup Review Submissions button
        MenuItem reviewItem = menu.findItem(R.id.action_review_submissions);
        android.view.View reviewActionView = reviewItem != null ? reviewItem.getActionView() : null;
        if (reviewActionView != null) {
            android.view.View btn = reviewActionView.findViewById(R.id.btnReviewSubmissions);
            android.view.View target = btn != null ? btn : reviewActionView;
            target.setOnClickListener(v -> {
                openCompilerSubmissionsReview();
            });
        }
        
        return true;
    }

    private void verifyOwnershipAndInit() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        FirebaseFirestore.getInstance().collection("Classes").document(classCode)
                .get()
                .addOnSuccessListener(doc -> {
                    String owner = doc.getString("createdBy");
                    if (owner != null && owner.equals(uid)) {
                        loadLessons(); // Load lessons first to get the correct order
                        loadStudents();
                    } else {
                        Toast.makeText(this, "You don't own this class", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to verify ownership", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadLessons() {
        System.out.println("🔍 TeacherClassDetail: Loading lessons for class: " + classCode);
        FirebaseFirestore.getInstance().collection("Classes").document(classCode).get()
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
                System.out.println("🔍 TeacherClassDetail: Loaded " + lessonNames.size() + " lessons in order: " + lessonNames);
            })
            .addOnFailureListener(e -> {
                System.out.println("❌ TeacherClassDetail: Failed to load lessons: " + e.getMessage());
                // Fallback to default order if loading fails
                lessonNames.clear();
                lessonNames.addAll(getDefaultLessons());
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_manage_attempts) {
            showAttemptManagementOptions();
            return true;
        } else if (itemId == R.id.action_view_chart) {
            showPassingRateChart();
            return true;
        } else if (itemId == R.id.action_lesson_management) {
            showLessonManagementOptions();
            return true;
        } else if (itemId == R.id.action_review_submissions) {
            openCompilerSubmissionsReview();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void openCompilerSubmissionsReview() {
        Intent intent = new Intent(this, CompilerSubmissionsReviewActivity.class);
        intent.putExtra("classCode", classCode);
        intent.putExtra("className", className);
        startActivity(intent);
    }

    private void refreshData() {
        System.out.println("🔍 TeacherClassDetail: Refreshing data...");

        // Clear existing data
        studentsList.clear();
        filteredStudentsList.clear();
        if (studentAdapter != null) {
            studentAdapter.notifyDataSetChanged();
        }

        // Clear search
        if (searchEditText != null) {
            searchEditText.setText("");
        }

        // Reload students
        loadStudents();
    }

    private void filterStudents(String query) {
        if (query == null) query = "";
        String searchQuery = query.trim().toLowerCase();
        
        filteredStudentsList.clear();
        
        if (searchQuery.isEmpty()) {
            // Show all students
            filteredStudentsList.addAll(studentsList);
        } else {
            // Filter students based on name, year, or block
            for (Student student : studentsList) {
                String name = student.fullName != null ? student.fullName.toLowerCase() : "";
                String year = student.yearLevel != null ? student.yearLevel.toLowerCase() : "";
                String block = student.block != null ? student.block.toLowerCase() : "";
                
                if (name.contains(searchQuery) || year.contains(searchQuery) || block.contains(searchQuery)) {
                    filteredStudentsList.add(student);
                }
            }
        }
        
        // Update adapter
        if (studentAdapter != null) {
            studentAdapter.notifyDataSetChanged();
        }
        
        // Update student count
        TextView studentCountText = findViewById(R.id.studentCountText);
        if (studentCountText != null) {
            int count = filteredStudentsList.size();
            String label = count == 1 ? "1 Student" : (count + " Students");
            if (!searchQuery.isEmpty()) {
                label += " (filtered)";
            }
            studentCountText.setText(label);
        }
        
        // Show/hide empty state
        LinearLayout emptyStateContainer = findViewById(R.id.emptyStateContainer);
        TextView noStudentsText = findViewById(R.id.noStudentsText);
        if (emptyStateContainer != null && noStudentsText != null) {
            if (filteredStudentsList.isEmpty()) {
                if (searchQuery.isEmpty()) {
                    noStudentsText.setText("No students yet!");
                } else {
                    noStudentsText.setText("No students found for \"" + query + "\"");
                }
                emptyStateContainer.setVisibility(View.VISIBLE);
            } else {
                emptyStateContainer.setVisibility(View.GONE);
            }
        }
    }

    private void showAttemptManagementOptions() {
        System.out.println("🔍 TeacherClassDetail: Showing attempt management options");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Manage Student Attempts");

        // Use lessons loaded from Firestore (same order as student lesson list)
        List<String> lessonsList = lessonNames.isEmpty() ? getDefaultLessons() : lessonNames;
        String[] lessons = lessonsList.toArray(new String[0]);

        builder.setItems(lessons, (dialog, which) -> {
            String selectedLesson = lessons[which];
            showActivityTypeSelection(selectedLesson);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private List<String> getDefaultLessons() {
        List<String> defaultLessons = new ArrayList<>();
        defaultLessons.add("INTRODUCTION TO JAVA");
        defaultLessons.add("VARIABLES and DATA");
        defaultLessons.add("OPERATORS and EXPRESSIONS");
        defaultLessons.add("CONDITIONAL STATEMENTS");
        defaultLessons.add("CONDITIONAL LOOPS");
        defaultLessons.add("ARRAYS");
        return defaultLessons;
    }

    private void showActivityTypeSelection(String lessonName) {
        System.out.println("🔍 TeacherClassDetail: Showing activity type selection for: " + lessonName);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Activity Type for " + lessonName);

        String[] activities = {"Quiz Attempts", "Code Builder Attempts"};

        builder.setItems(activities, (dialog, which) -> {
            String activityType = (which == 0) ? "quiz" : "code_builder";
            System.out.println("🔍 TeacherClassDetail: Opening attempt management for " + lessonName + " " + activityType);

            Intent intent = new Intent(this, StudentAttemptManagementActivity.class);
            intent.putExtra("classCode", classCode);
            intent.putExtra("lessonName", lessonName);
            intent.putExtra("activityType", activityType);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLessonManagementOptions() {
        System.out.println("🔍 TeacherClassDetail: Showing lesson management options");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Lesson Management");

        // Use lessons loaded from Firestore (same order as student lesson list)
        List<String> lessonsList = lessonNames.isEmpty() ? getDefaultLessons() : lessonNames;
        String[] lessons = lessonsList.toArray(new String[0]);

        builder.setItems(lessons, (dialog, which) -> {
            String selectedLesson = lessons[which];
            showLessonControls(selectedLesson);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLessonControls(String lessonName) {
        System.out.println("🔍 TeacherClassDetail: Showing lesson controls for: " + lessonName);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Lesson Controls - " + lessonName);

        String[] controls = {"🔓 Unlock Lesson", "🔒 Lock Lesson"};

        builder.setItems(controls, (dialog, which) -> {
            if (which == 0) {
                // Unlock lesson
                LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_UNLOCKED);
                Toast.makeText(this, "Lesson unlocked", Toast.LENGTH_SHORT).show();
            } else if (which == 1) {
                // Lock lesson
                LessonManager.setLessonStatus(classCode, lessonName, LessonManager.STATUS_LOCKED);
                Toast.makeText(this, "Lesson locked", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showPassingRateChart() {
        System.out.println("🔍 TeacherClassDetail: Showing passing rate chart");
        
        // Create custom dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_passing_rate_chart, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Initialize chart components
        BarChart barChart = dialogView.findViewById(R.id.barChart);
        LinearLayout loadingContainer = dialogView.findViewById(R.id.loadingContainer);
        LinearLayout emptyStateContainer = dialogView.findViewById(R.id.emptyStateContainer);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnRefresh = dialogView.findViewById(R.id.btnRefresh);
        
        // Show loading state
        loadingContainer.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
        
        // Setup buttons
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRefresh.setOnClickListener(v -> {
            // Show loading and refresh data
            loadingContainer.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            barChart.setVisibility(View.GONE);
            loadChartData(barChart, loadingContainer, emptyStateContainer);
        });
        
        // Load chart data
        loadChartData(barChart, loadingContainer, emptyStateContainer);
    }
    
    private void loadChartData(BarChart barChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer) {
        System.out.println("🔍 TeacherClassDetail: Loading chart data for class: " + classCode);
        
        // Use lessons loaded from Firestore (same order as student lesson list)
        List<String> lessonsList = lessonNames.isEmpty() ? getDefaultLessons() : lessonNames;
        String[] lessons = lessonsList.toArray(new String[0]);
        
        ChartDataResult result = new ChartDataResult();
        loadAllLessonChartData(result, lessons, 0, barChart, loadingContainer, emptyStateContainer);
    }
    
    private void loadAllLessonChartData(ChartDataResult result, String[] lessons, int lessonIndex,
                                       BarChart barChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer) {
        
        if (lessonIndex >= lessons.length) {
            // All lessons processed, render chart
            renderChart(barChart, loadingContainer, emptyStateContainer, result);
            return;
        }
        
        String currentLesson = lessons[lessonIndex];
        System.out.println("🔍 TeacherClassDetail: Loading chart data for " + currentLesson);
        
        LessonChartData lessonData = new LessonChartData();
        lessonData.lessonName = currentLesson;
        lessonData.quizPassingPercent = 0;
        lessonData.codeBuilderPassingPercent = 0;
        lessonData.compilerPassingPercent = 0;
        
        // Load quiz passing count
        FirebaseFirestore.getInstance()
                .collection("Classes").document(classCode)
                .collection("Leaderboards").document(currentLesson + "_quiz")
                .collection("Scores")
                .get()
                .addOnSuccessListener(quizSnapshot -> {
                    int passingCount = 0;
                    int attemptedCount = 0;
                    for (DocumentSnapshot doc : quizSnapshot) {
                        Long score = doc.getLong("score");
                        if (score != null) {
                            attemptedCount++; // Count students who attempted
                            if (score >= 8) { // Quiz passing threshold: 8/15
                            passingCount++;
                        }
                    }
                    }
                    // Calculate percentage: (passing / attempted) * 100
                    // Only count students who actually attempted the activity
                    if (attemptedCount > 0) {
                        lessonData.quizPassingPercent = (int) Math.round((passingCount * 100.0) / attemptedCount);
                    } else {
                        lessonData.quizPassingPercent = 0;
                    }
                    
                    // Load code builder passing count
                    FirebaseFirestore.getInstance()
                            .collection("Classes").document(classCode)
                            .collection("Leaderboards").document(currentLesson + "_code_builder")
                            .collection("Scores")
                            .get()
                            .addOnSuccessListener(codeSnapshot -> {
                                int codePassingCount = 0;
                                int codeAttemptedCount = 0;
                                for (DocumentSnapshot doc : codeSnapshot) {
                                    Long score = doc.getLong("score");
                                    if (score != null) {
                                        codeAttemptedCount++; // Count students who attempted
                                        if (score >= 15) { // Code Builder passing threshold: 15/25
                                        codePassingCount++;
                                    }
                                }
                                }
                                // Calculate percentage: (passing / attempted) * 100
                                if (codeAttemptedCount > 0) {
                                    lessonData.codeBuilderPassingPercent = (int) Math.round((codePassingCount * 100.0) / codeAttemptedCount);
                                } else {
                                    lessonData.codeBuilderPassingPercent = 0;
                                }
                                
                                // Load compiler passing count
                                FirebaseFirestore.getInstance()
                                        .collection("Classes").document(classCode)
                                        .collection("Leaderboards").document(currentLesson + "_compiler")
                                        .collection("Scores")
                                        .get()
                                        .addOnSuccessListener(compilerSnapshot -> {
                                            int compilerPassingCount = 0;
                                            int compilerAttemptedCount = 0;
                                            for (DocumentSnapshot doc : compilerSnapshot) {
                                                Long score = doc.getLong("score");
                                                if (score != null) {
                                                    compilerAttemptedCount++; // Count students who attempted
                                                    if (score >= 50) { // Compiler passing threshold: 50/100
                                                        compilerPassingCount++;
                                                    }
                                                }
                                            }
                                            // Calculate percentage: (passing / attempted) * 100
                                            if (compilerAttemptedCount > 0) {
                                                lessonData.compilerPassingPercent = (int) Math.round((compilerPassingCount * 100.0) / compilerAttemptedCount);
                                            } else {
                                                lessonData.compilerPassingPercent = 0;
                                            }
                                
                                result.lessonData.add(lessonData);
                                loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        })
                                        .addOnFailureListener(e -> {
                                            System.out.println("🔍 TeacherClassDetail: Error loading compiler chart data: " + e.getMessage());
                                            lessonData.compilerPassingPercent = 0;
                                            result.lessonData.add(lessonData);
                                            loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                System.out.println("🔍 TeacherClassDetail: Error loading code builder chart data: " + e.getMessage());
                                lessonData.codeBuilderPassingPercent = 0;
                                // Still try to load compiler
                                FirebaseFirestore.getInstance()
                                        .collection("Classes").document(classCode)
                                        .collection("Leaderboards").document(currentLesson + "_compiler")
                                        .collection("Scores")
                                        .get()
                                        .addOnSuccessListener(compilerSnapshot -> {
                                            int compilerPassingCount = 0;
                                            int compilerAttemptedCount = 0;
                                            for (DocumentSnapshot doc : compilerSnapshot) {
                                                Long score = doc.getLong("score");
                                                if (score != null) {
                                                    compilerAttemptedCount++;
                                                    if (score >= 50) {
                                                        compilerPassingCount++;
                                                    }
                                                }
                                            }
                                            if (compilerAttemptedCount > 0) {
                                                lessonData.compilerPassingPercent = (int) Math.round((compilerPassingCount * 100.0) / compilerAttemptedCount);
                                            } else {
                                                lessonData.compilerPassingPercent = 0;
                                            }
                                            result.lessonData.add(lessonData);
                                            loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        })
                                        .addOnFailureListener(e2 -> {
                                            lessonData.compilerPassingPercent = 0;
                                            result.lessonData.add(lessonData);
                                            loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    System.out.println("🔍 TeacherClassDetail: Error loading quiz chart data: " + e.getMessage());
                    lessonData.quizPassingPercent = 0;
                    // Still try to load code builder and compiler
                    FirebaseFirestore.getInstance()
                            .collection("Classes").document(classCode)
                            .collection("Leaderboards").document(currentLesson + "_code_builder")
                            .collection("Scores")
                            .get()
                            .addOnSuccessListener(codeSnapshot -> {
                                int codePassingCount = 0;
                                int codeAttemptedCount = 0;
                                for (DocumentSnapshot doc : codeSnapshot) {
                                    Long score = doc.getLong("score");
                                    if (score != null) {
                                        codeAttemptedCount++;
                                        if (score >= 15) {
                                            codePassingCount++;
                                        }
                                    }
                                }
                                if (codeAttemptedCount > 0) {
                                    lessonData.codeBuilderPassingPercent = (int) Math.round((codePassingCount * 100.0) / codeAttemptedCount);
                                } else {
                                    lessonData.codeBuilderPassingPercent = 0;
                                }
                                
                                FirebaseFirestore.getInstance()
                                        .collection("Classes").document(classCode)
                                        .collection("Leaderboards").document(currentLesson + "_compiler")
                                        .collection("Scores")
                                        .get()
                                        .addOnSuccessListener(compilerSnapshot -> {
                                            int compilerPassingCount = 0;
                                            int compilerAttemptedCount = 0;
                                            for (DocumentSnapshot doc : compilerSnapshot) {
                                                Long score = doc.getLong("score");
                                                if (score != null) {
                                                    compilerAttemptedCount++;
                                                    if (score >= 50) {
                                                        compilerPassingCount++;
                                                    }
                                                }
                                            }
                                            if (compilerAttemptedCount > 0) {
                                                lessonData.compilerPassingPercent = (int) Math.round((compilerPassingCount * 100.0) / compilerAttemptedCount);
                                            } else {
                                                lessonData.compilerPassingPercent = 0;
                                            }
                                            result.lessonData.add(lessonData);
                                            loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        })
                                        .addOnFailureListener(e2 -> {
                                            lessonData.compilerPassingPercent = 0;
                    result.lessonData.add(lessonData);
                    loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                                        });
                            })
                            .addOnFailureListener(e2 -> {
                                lessonData.codeBuilderPassingPercent = 0;
                                lessonData.compilerPassingPercent = 0;
                                result.lessonData.add(lessonData);
                                loadAllLessonChartData(result, lessons, lessonIndex + 1, barChart, loadingContainer, emptyStateContainer);
                            });
                });
    }
    
    private void renderChart(BarChart barChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer, ChartDataResult result) {
        loadingContainer.setVisibility(View.GONE);
        
        if (result.lessonData.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            barChart.setVisibility(View.GONE);
            return;
        }
        
        barChart.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        
        // Prepare data entries (now showing percentages 0-100)
        List<BarEntry> quizEntries = new ArrayList<>();
        List<BarEntry> codeBuilderEntries = new ArrayList<>();
        List<BarEntry> compilerEntries = new ArrayList<>();
        List<String> lessonLabels = new ArrayList<>();

        int lessonCount = result.lessonData.size();
        for (int i = 0; i < lessonCount; i++) {
            LessonChartData data = result.lessonData.get(i);
            float x = i; // L1 -> 0f, L2 -> 1f, etc.

            // Debug log so we can see exactly what the chart is plotting
            System.out.println("📊 Chart lesson " + (i + 1) + " -> Quiz=" + data.quizPassingPercent
                    + "% CodeBuilder=" + data.codeBuilderPassingPercent
                    + "% Compiler=" + data.compilerPassingPercent + "%");

            quizEntries.add(new BarEntry(x, data.quizPassingPercent));
            codeBuilderEntries.add(new BarEntry(x, data.codeBuilderPassingPercent));
            compilerEntries.add(new BarEntry(x, data.compilerPassingPercent));
            lessonLabels.add("L" + (i + 1));
        }
        
        // Create datasets with improved styling
        BarDataSet quizDataSet = new BarDataSet(quizEntries, "Quiz");
        quizDataSet.setColor(android.graphics.Color.parseColor("#4F46E5")); // Vibrant indigo
        quizDataSet.setDrawValues(false);
        
        BarDataSet codeBuilderDataSet = new BarDataSet(codeBuilderEntries, "Code Builder");
        codeBuilderDataSet.setColor(android.graphics.Color.parseColor("#10B981")); // Emerald
        codeBuilderDataSet.setDrawValues(false);
        
        BarDataSet compilerDataSet = new BarDataSet(compilerEntries, "Compiler");
        compilerDataSet.setColor(android.graphics.Color.parseColor("#F97316")); // Bright orange
        compilerDataSet.setDrawValues(false);
        
        float groupSpace = 0.32f;
        float barSpace = 0.02f;
        float barWidth = (1f - groupSpace) / 3f - barSpace;
        BarData barData = new BarData(quizDataSet, codeBuilderDataSet, compilerDataSet);
        barData.setBarWidth(barWidth);
        barData.setValueTextSize(10f);
        barChart.setData(barData);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.groupBars(0f, groupSpace, barSpace);
        
        // Configure chart appearance
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setBackgroundColor(android.graphics.Color.WHITE);
        barChart.setDrawGridBackground(false);
        barChart.setExtraOffsets(16f, 20f, 24f, 22f); // Top, Right, Bottom, Left padding
        barChart.setNoDataText("No data available");
        barChart.setNoDataTextColor(getResources().getColor(R.color.game_text_secondary));
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setHighlightPerTapEnabled(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.animateY(900, Easing.EaseInOutQuad);
        barChart.setFitBars(false);
        
        // Configure X-axis: keep lesson labels centered while showing 3 bars per lesson
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.game_text_primary));
        xAxis.setTextSize(12.5f);
        xAxis.setLabelCount(lessonLabels.size(), true); // Show all lessons
        xAxis.setGranularity(1f); // 1 step between lessons
        float groupWidth = barData.getGroupWidth(groupSpace, barSpace);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(0f + groupWidth * lessonLabels.size());
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(getResources().getColor(R.color.game_gray_border));
        xAxis.setGridLineWidth(0.8f);
        xAxis.setAxisLineColor(getResources().getColor(R.color.game_gray_border));
        xAxis.setAxisLineWidth(2f);
        xAxis.setYOffset(8f); // Space between labels and axis
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (groupWidth == 0f) {
                    return "";
                }
                int index = Math.round(value / groupWidth);
                if (index >= 0 && index < lessonLabels.size()) {
                    return lessonLabels.get(index);
                }
                return "";
            }
        });
        
        // Configure Y-axis (now showing percentages 0-100%)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.game_text_primary));
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f); // Percentage scale (0-100%)
        leftAxis.setGranularity(20f); // Show labels at 0, 20, 40, 60, 80, 100
        leftAxis.setLabelCount(6, true); // Show 6 labels on Y-axis
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getResources().getColor(R.color.game_gray_border));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setAxisLineColor(getResources().getColor(R.color.game_gray_border));
        leftAxis.setAxisLineWidth(1.5f);
        leftAxis.setXOffset(10f); // Space between labels and axis
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value) + "%";
            }
        });
        
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Configure legend
        barChart.getLegend().setTextColor(getResources().getColor(R.color.game_text_primary));
        barChart.getLegend().setTextSize(13f);
        barChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        barChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        barChart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        barChart.getLegend().setDrawInside(false);
        barChart.getLegend().setFormSize(12f);
        barChart.getLegend().setFormToTextSpace(8f);
        barChart.getLegend().setXEntrySpace(20f);
        barChart.getLegend().setYOffset(10f);
        
        // Refresh chart
        barChart.invalidate();
        
        System.out.println("🔍 TeacherClassDetail: Chart rendered with " + result.lessonData.size() + " lessons");
    }

    private void loadStudents() {
        System.out.println("🔍 TeacherClassDetail: Loading students for class: " + classCode);

        FirebaseFirestore.getInstance()
                .collection("Classes").document(classCode)
                .collection("Students")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            studentsList.clear();
                            for (DocumentSnapshot document : task.getResult()) {
                                // Create student object manually to handle the actual Firebase structure
                                Student student = new Student();
                                student.studentId = document.getId();
                                student.userId = document.getString("userId");
                                student.fullName = document.getString("fullName");

                                // Handle yearBlock field (stored as "Year - Block" format)
                                String yearBlock = document.getString("yearBlock");
                                if (yearBlock != null && yearBlock.contains(" - ")) {
                                    String[] parts = yearBlock.split(" - ");
                                    student.yearLevel = parts[0];
                                    student.block = parts[1];
                                } else {
                                    // Fallback: get from class document
                                    student.yearLevel = "N/A";
                                    student.block = "N/A";
                                }

                                studentsList.add(student);
                                System.out.println("🔍 TeacherClassDetail: Loaded student: " + student.fullName + " (ID: " + student.studentId + ", Year: " + student.yearLevel + ", Block: " + student.block + ")");
                            }

                            System.out.println("🔍 TeacherClassDetail: Found " + studentsList.size() + " students");

                            // Update roster count below header
                            TextView studentCountText = findViewById(R.id.studentCountText);
                            if (studentCountText != null) {
                                int count = studentsList.size();
                                String label = count == 1 ? "1 Student" : (count + " Students");
                                studentCountText.setText(label);
                            }

                            // Update filtered list and adapter
                            filteredStudentsList.clear();
                            filteredStudentsList.addAll(studentsList);
                            studentAdapter = new StudentAdapter(filteredStudentsList);
                            studentsRecyclerView.setAdapter(studentAdapter);
                            
                            // Apply current search filter if any
                            String currentSearch = searchEditText.getText() != null ? searchEditText.getText().toString() : "";
                            filterStudents(currentSearch);
                        } else {
                            System.out.println("🔍 TeacherClassDetail: Error loading students: " + task.getException());
                            Toast.makeText(TeacherClassDetailActivity.this, "Error loading students", Toast.LENGTH_SHORT).show();
                        }

                        // Stop refresh animation
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });
    }
    
    private void showEditStudentDialog(Student student, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_student, null);
        builder.setView(dialogView);
        
        TextInputEditText editName = dialogView.findViewById(R.id.editStudentName);
        TextInputEditText editYear = dialogView.findViewById(R.id.editYearLevel);
        TextInputEditText editBlock = dialogView.findViewById(R.id.editBlock);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEdit);
        Button btnSave = dialogView.findViewById(R.id.btnSaveEdit);
        
        // Pre-fill current values
        editName.setText(student.fullName != null ? student.fullName : "");
        editYear.setText(student.yearLevel != null ? student.yearLevel : "");
        editBlock.setText(student.block != null ? student.block : "");
        
        AlertDialog dialog = builder.create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newYear = editYear.getText().toString().trim();
            String newBlock = editBlock.getText().toString().trim();
            
            if (newName.isEmpty()) {
                Toast.makeText(this, "Student name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update in Firebase
            updateStudentInfo(student.studentId, newName, newYear, newBlock);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void showKickStudentConfirmDialog(Student student, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Student");
        builder.setMessage("Are you sure you want to remove " + student.fullName + " from this class? This action cannot be undone.");
        
        builder.setPositiveButton("Remove", (dialog, which) -> {
            kickStudent(student);
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.show();
    }
    
    private void kickStudent(Student student) {
        String studentId = student.studentId;
        
        // Get class name for notification
        FirebaseFirestore.getInstance().collection("Classes").document(classCode)
          .get()
          .addOnSuccessListener(classDoc -> {
              String className = classDoc.exists() ? classDoc.getString("className") : classCode;
              
              // Remove student from class
              FirebaseFirestore.getInstance().collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove class from student's MyJoinedClasses
                    FirebaseFirestore.getInstance().collection("Users").document(studentId)
                      .collection("MyJoinedClasses").document(classCode)
                      .delete()
                      .addOnSuccessListener(unused -> {
                          // Send kick notification to student via FCM
                          FCMHelper.sendKickNotification(studentId, classCode, className);
                          
                          // Clean up all request documents for this student
                          cleanupStudentRequests(studentId, classCode);
                          
                          Toast.makeText(this, student.fullName + " has been removed from the class", Toast.LENGTH_SHORT).show();
                          
                          // Reload students list
                          loadStudents();
                      })
                      .addOnFailureListener(e -> {
                          Toast.makeText(this, "Failed to remove class from student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                          // Still reload students list
                          loadStudents();
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
    
    private void cleanupStudentRequests(String studentId, String classCode) {
        // Delete join request
        FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("JoinRequests").document(studentId)
            .delete()
            .addOnSuccessListener(aVoid -> System.out.println("✅ Cleaned up join request for " + studentId))
            .addOnFailureListener(e -> System.out.println("❌ Failed to clean up join request: " + e.getMessage()));
        
        // Delete leave request
        FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("LeaveRequests").document(studentId)
            .delete()
            .addOnSuccessListener(aVoid -> System.out.println("✅ Cleaned up leave request for " + studentId))
            .addOnFailureListener(e -> System.out.println("❌ Failed to clean up leave request: " + e.getMessage()));
    }
    
    private void startStudentsListener() {
        if (studentsListener != null) {
            studentsListener.remove();
        }
        
        studentsListener = FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("Students")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    System.out.println("❌ Students listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null) {
                    studentsList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Student student = doc.toObject(Student.class);
                        if (student != null) {
                            student.studentId = doc.getId();
                            studentsList.add(student);
                        }
                    }
                    
                    // Update filtered list and adapter
                    filterStudents(searchEditText.getText().toString());
                    if (studentAdapter != null) {
                        studentAdapter.notifyDataSetChanged();
                    }
                    
                    System.out.println("🔄 Students list auto-reloaded: " + studentsList.size() + " students");
                }
            });
    }
    
    private void stopStudentsListener() {
        if (studentsListener != null) {
            studentsListener.remove();
            studentsListener = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStudentsListener();
        // Remove all score listeners
        for (ListenerRegistration listener : studentScoreListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        studentScoreListeners.clear();
        System.out.println("🔍 TeacherClassDetail: Cleaned up all listeners in onDestroy");
    }
    
    private void updateStudentInfo(String studentId, String newName, String newYear, String newBlock) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", newName);
        
        // Update yearBlock
        String yearBlock = (newYear.isEmpty() ? "N/A" : newYear) + " - " + (newBlock.isEmpty() ? "N/A" : newBlock);
        updates.put("yearBlock", yearBlock);
        
        FirebaseFirestore.getInstance()
            .collection("Classes").document(classCode)
            .collection("Students").document(studentId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Student information updated", Toast.LENGTH_SHORT).show();
                // Reload students to reflect changes
                loadStudents();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update student: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<Student> students;

        public StudentAdapter(List<Student> students) {
            this.students = students;
        }
    
    @NonNull
    @Override
        public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_detailed, parent, false);
        return new StudentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = students.get(position);
            holder.nameText.setText(student.fullName);
            // Display year and block in the middle section
            String yearLevel = student.yearLevel != null ? student.yearLevel : "N/A";
            String block = student.block != null ? student.block : "N/A";
            String yearBlockDisplay = yearLevel + " - " + block;
            holder.yearBlockText.setText(yearBlockDisplay);
            holder.yearBlockText.setVisibility(View.VISIBLE);

            System.out.println("🔍 TeacherClassDetail: Binding student " + position + ": " + student.fullName + ", Year: " + yearLevel + ", Block: " + block);
            System.out.println("🔍 TeacherClassDetail: Setting yearBlockText to: " + yearBlockDisplay);

            // Check if this student's card was expanded
            boolean wasExpanded = expandedStudentIds.contains(student.studentId);
            
            if (wasExpanded) {
                // Card was expanded - restore expanded state and reload scores
                holder.expandableContainer.setVisibility(View.VISIBLE);
                holder.loadingContainer.setVisibility(View.VISIBLE);
                holder.scoresContainer.setVisibility(View.GONE);
                holder.noScoresContainer.setVisibility(View.GONE);
                // Reload scores with real-time listeners
                loadDetailedStudentScores(student, holder);
            } else {
            // Initially hide expandable container
            holder.expandableContainer.setVisibility(View.GONE);
            }
            
            // Edit button click listener
            holder.btnEdit.setOnClickListener(v -> {
                showEditStudentDialog(student, holder.getAdapterPosition());
            });
            
            // Kick button click listener
            holder.btnKick.setOnClickListener(v -> {
                showKickStudentConfirmDialog(student, holder.getAdapterPosition());
            });
            
            // Set click listener for the entire card to expand/collapse
            holder.itemView.setOnClickListener(v -> {
                if (holder.expandableContainer.getVisibility() == View.GONE) {
                    // Expand and load scores
                    expandedStudentIds.add(student.studentId);
                    holder.expandableContainer.setVisibility(View.VISIBLE);
                    holder.loadingContainer.setVisibility(View.VISIBLE);
                    holder.scoresContainer.setVisibility(View.GONE);
                    holder.noScoresContainer.setVisibility(View.GONE);
                    
                    // Load detailed student scores with real-time listeners
                    loadDetailedStudentScores(student, holder);
                } else {
                    // Collapse - remove listeners for this student
                    expandedStudentIds.remove(student.studentId);
                    removeStudentScoreListeners(student.studentId);
                    holder.expandableContainer.setVisibility(View.GONE);
                }
            });
    }
    
    @Override
        public int getItemCount() {
            return students.size();
        }


        private void loadDetailedStudentScores(Student student, StudentViewHolder holder) {
            System.out.println("🔍 TeacherClassDetail: Loading detailed scores for " + student.fullName + " (ID: " + student.studentId + ")");

            // Remove old listeners for this student
            removeStudentScoreListeners(student.studentId);

            // Clear existing scores
            holder.scoresContainer.removeAllViews();

            // Use lessons loaded from Firestore (same order as student lesson list)
            // If lessons haven't loaded yet, use fallback
            if (lessonNames.isEmpty()) {
                System.out.println("⚠️ TeacherClassDetail: Lessons not loaded yet, using fallback");
                lessonNames.addAll(getDefaultLessons());
            }

            // Create map to store lesson scores (maintain order from lessonNames)
            Map<String, LessonScore> lessonScoreMap = new HashMap<>();
            for (String lesson : lessonNames) {
            LessonScore lessonScore = new LessonScore();
                lessonScore.lessonName = lesson;
            lessonScore.quizScore = 0;
            lessonScore.quizAttempts = 0;
            lessonScore.codeBuilderScore = 0;
            lessonScore.codeBuilderAttempts = 0;
            lessonScore.compilerScore = 0;
            lessonScore.compilerAttempts = 0;
                lessonScoreMap.put(lesson, lessonScore);
            }

            // Add real-time listeners for each lesson and activity (in order from lessonNames)
            for (String lessonName : lessonNames) {
                // Quiz score listener
                String quizKey = student.studentId + "_" + lessonName + "_quiz";
                ListenerRegistration quizListener = FirebaseFirestore.getInstance()
                    .collection("Classes").document(classCode)
                    .collection("Leaderboards").document(lessonName + "_quiz")
                    .collection("Scores").document(student.studentId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || isFinishing() || isDestroyed()) return;
                        
                        LessonScore lessonScore = lessonScoreMap.get(lessonName);
                        if (lessonScore != null && snapshot != null && snapshot.exists()) {
                            Long score = snapshot.getLong("score");
                            Long attempts = snapshot.getLong("attemptsUsed");
                            if (score != null) lessonScore.quizScore = score.intValue();
                            if (attempts != null) lessonScore.quizAttempts = attempts.intValue();
                            System.out.println("🔍 TeacherClassDetail: Real-time update - " + lessonName + " Quiz - Score: " + lessonScore.quizScore + ", Attempts: " + lessonScore.quizAttempts);
                        } else if (lessonScore != null) {
                            lessonScore.quizScore = 0;
                            lessonScore.quizAttempts = 0;
                        }
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            // Find the holder again in case RecyclerView recycled it
                            StudentViewHolder currentHolder = findViewHolderForStudent(student.studentId);
                            if (currentHolder != null && currentHolder.expandableContainer.getVisibility() == View.VISIBLE) {
                                // Holder found and card is expanded - update directly
                                updateStudentScoresUI(student, currentHolder, new ArrayList<>(lessonScoreMap.values()));
                            } else {
                                // Holder not found or card collapsed - notify adapter to rebind
                                // This will reload scores if card is expanded
                                int position = findStudentPosition(student.studentId);
                                if (position >= 0 && studentAdapter != null) {
                                    studentAdapter.notifyItemChanged(position);
                                }
                            }
                        });
                    });
                studentScoreListeners.put(quizKey, quizListener);

                // Code builder score listener
                String codeBuilderKey = student.studentId + "_" + lessonName + "_code_builder";
                ListenerRegistration codeBuilderListener = FirebaseFirestore.getInstance()
                                .collection("Classes").document(classCode)
                    .collection("Leaderboards").document(lessonName + "_code_builder")
                                .collection("Scores").document(student.studentId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || isFinishing() || isDestroyed()) return;
                        
                        LessonScore lessonScore = lessonScoreMap.get(lessonName);
                        if (lessonScore != null && snapshot != null && snapshot.exists()) {
                            Long score = snapshot.getLong("score");
                            Long attempts = snapshot.getLong("attemptsUsed");
                                        if (score != null) lessonScore.codeBuilderScore = score.intValue();
                                        if (attempts != null) lessonScore.codeBuilderAttempts = attempts.intValue();
                            System.out.println("🔍 TeacherClassDetail: Real-time update - " + lessonName + " Code Builder - Score: " + lessonScore.codeBuilderScore + ", Attempts: " + lessonScore.codeBuilderAttempts);
                        } else if (lessonScore != null) {
                            lessonScore.codeBuilderScore = 0;
                            lessonScore.codeBuilderAttempts = 0;
                                    }

                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            // Find the holder again in case RecyclerView recycled it
                            StudentViewHolder currentHolder = findViewHolderForStudent(student.studentId);
                            if (currentHolder != null && currentHolder.expandableContainer.getVisibility() == View.VISIBLE) {
                                // Holder found and card is expanded - update directly
                                updateStudentScoresUI(student, currentHolder, new ArrayList<>(lessonScoreMap.values()));
                            } else {
                                // Holder not found or card collapsed - notify adapter to rebind
                                // This will reload scores if card is expanded
                                int position = findStudentPosition(student.studentId);
                                if (position >= 0 && studentAdapter != null) {
                                    studentAdapter.notifyItemChanged(position);
                                }
                            }
                        });
                    });
                studentScoreListeners.put(codeBuilderKey, codeBuilderListener);

                // Compiler score listener
                String compilerKey = student.studentId + "_" + lessonName + "_compiler";
                ListenerRegistration compilerListener = FirebaseFirestore.getInstance()
                    .collection("Classes").document(classCode)
                    .collection("Leaderboards").document(lessonName + "_compiler")
                    .collection("Scores").document(student.studentId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || isFinishing() || isDestroyed()) return;
                        
                        LessonScore lessonScore = lessonScoreMap.get(lessonName);
                        if (lessonScore != null && snapshot != null && snapshot.exists()) {
                            Long score = snapshot.getLong("score");
                            Long attempts = snapshot.getLong("attemptsUsed");
                            if (score != null) lessonScore.compilerScore = score.intValue();
                            if (attempts != null) lessonScore.compilerAttempts = attempts.intValue();
                            System.out.println("🔍 TeacherClassDetail: Real-time update - " + lessonName + " Compiler - Score: " + lessonScore.compilerScore + ", Attempts: " + lessonScore.compilerAttempts);
                        } else if (lessonScore != null) {
                            lessonScore.compilerScore = 0;
                            lessonScore.compilerAttempts = 0;
                        }
                        
                        // Update UI on main thread
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            StudentViewHolder currentHolder = findViewHolderForStudent(student.studentId);
                            if (currentHolder != null && currentHolder.expandableContainer.getVisibility() == View.VISIBLE) {
                                updateStudentScoresUI(student, currentHolder, new ArrayList<>(lessonScoreMap.values()));
                            } else {
                                int position = findStudentPosition(student.studentId);
                                if (position >= 0 && studentAdapter != null) {
                                    studentAdapter.notifyItemChanged(position);
                                }
                            }
                        });
                    });
                studentScoreListeners.put(compilerKey, compilerListener);
            }
        }

        private void updateStudentScoresUI(Student student, StudentViewHolder holder, List<LessonScore> lessonScores) {
            if (isFinishing() || isDestroyed() || holder == null) return;
            
            // Verify holder is still bound to this student
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION || position >= filteredStudentsList.size()) {
                return;
            }
            
            Student currentStudent = filteredStudentsList.get(position);
            if (!currentStudent.studentId.equals(student.studentId)) {
                // Holder was recycled for a different student
                return;
            }
            
            displayLessonScores(student, holder, lessonScores);
        }

        private void removeStudentScoreListeners(String studentId) {
            List<String> keysToRemove = new ArrayList<>();
            for (String key : studentScoreListeners.keySet()) {
                if (key.startsWith(studentId + "_")) {
                    ListenerRegistration listener = studentScoreListeners.get(key);
                    if (listener != null) {
                        listener.remove();
                    }
                    keysToRemove.add(key);
                }
            }
            for (String key : keysToRemove) {
                studentScoreListeners.remove(key);
            }
            System.out.println("🔍 TeacherClassDetail: Removed " + keysToRemove.size() + " listeners for student " + studentId);
        }

        private StudentViewHolder findViewHolderForStudent(String studentId) {
            if (studentsRecyclerView == null) return null;
            
            // Find the adapter position for this student
            int position = findStudentPosition(studentId);
            if (position == -1) return null;
            
            // Get the ViewHolder from RecyclerView
            RecyclerView.ViewHolder viewHolder = studentsRecyclerView.findViewHolderForAdapterPosition(position);
            if (viewHolder instanceof StudentViewHolder) {
                return (StudentViewHolder) viewHolder;
            }
            
            return null;
        }

        private int findStudentPosition(String studentId) {
            for (int i = 0; i < filteredStudentsList.size(); i++) {
                if (filteredStudentsList.get(i).studentId.equals(studentId)) {
                    return i;
                }
            }
            return -1;
        }

        private void displayLessonScores(Student student, StudentViewHolder holder, List<LessonScore> lessonScores) {
            // Hide loading
            holder.loadingContainer.setVisibility(View.GONE);

            if (lessonScores.isEmpty()) {
                holder.noScoresContainer.setVisibility(View.VISIBLE);
                holder.scoresContainer.setVisibility(View.GONE);
                return;
            }

            holder.scoresContainer.setVisibility(View.VISIBLE);
            holder.noScoresContainer.setVisibility(View.GONE);

            // Clear and rebuild all views
            holder.scoresContainer.removeAllViews();

            // Create a map for quick lookup
            Map<String, LessonScore> lessonScoreMap = new HashMap<>();
            for (LessonScore score : lessonScores) {
                lessonScoreMap.put(score.lessonName, score);
            }

            // Add lesson score views in the same order as lessonNames (matching student lesson list)
            for (String lessonName : lessonNames) {
                LessonScore lessonScore = lessonScoreMap.get(lessonName);
                if (lessonScore == null) {
                    // Create empty score if not found
                    lessonScore = new LessonScore();
                    lessonScore.lessonName = lessonName;
                    lessonScore.quizScore = 0;
                    lessonScore.quizAttempts = 0;
                    lessonScore.codeBuilderScore = 0;
                    lessonScore.codeBuilderAttempts = 0;
                    lessonScore.compilerScore = 0;
                    lessonScore.compilerAttempts = 0;
                }
                View lessonView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_lesson_score, holder.scoresContainer, false);

                TextView lessonNameText = lessonView.findViewById(R.id.lessonName);
                TextView quizScoreText = lessonView.findViewById(R.id.quizScore);
                TextView quizAttemptsText = lessonView.findViewById(R.id.quizAttempts);
                TextView codeBuilderScoreText = lessonView.findViewById(R.id.codeBuilderScore);
                TextView codeBuilderAttemptsText = lessonView.findViewById(R.id.codeBuilderAttempts);
                TextView compilerScoreText = lessonView.findViewById(R.id.compilerScore);
                TextView compilerAttemptsText = lessonView.findViewById(R.id.compilerAttempts);

                lessonNameText.setText(lessonScore.lessonName);

                // Quiz scores
                if (lessonScore.quizScore > 0) {
                    quizScoreText.setText(String.valueOf(lessonScore.quizScore));
                    quizScoreText.setTextColor(0xFF4CAF50);
                } else {
                    quizScoreText.setText("--");
                    quizScoreText.setTextColor(0xFF999999);
                }
                quizAttemptsText.setText(lessonScore.quizAttempts + "/3");

                // Code Builder scores
                if (lessonScore.codeBuilderScore > 0) {
                    codeBuilderScoreText.setText(String.valueOf(lessonScore.codeBuilderScore));
                    codeBuilderScoreText.setTextColor(0xFF2196F3);
                } else {
                    codeBuilderScoreText.setText("--");
                    codeBuilderScoreText.setTextColor(0xFF999999);
                }
                codeBuilderAttemptsText.setText(lessonScore.codeBuilderAttempts + "/3");

                // Compiler scores
                if (lessonScore.compilerScore > 0) {
                    compilerScoreText.setText(String.valueOf(lessonScore.compilerScore));
                    compilerScoreText.setTextColor(0xFFFF9800);
                } else {
                    compilerScoreText.setText("--");
                    compilerScoreText.setTextColor(0xFF999999);
                }
                compilerAttemptsText.setText(lessonScore.compilerAttempts + "/1");

                holder.scoresContainer.addView(lessonView);
            }

            System.out.println("🔍 TeacherClassDetail: Displayed " + lessonScores.size() + " lesson scores for " + student.fullName);
}

class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView yearBlockText;
            ImageButton btnEdit;
            ImageButton btnKick;
            LinearLayout expandableContainer;
            LinearLayout scoresContainer;
            LinearLayout loadingContainer;
            LinearLayout noScoresContainer;

            public StudentViewHolder(@NonNull View itemView) {
        super(itemView); 
                nameText = itemView.findViewById(R.id.studentName);
                yearBlockText = itemView.findViewById(R.id.studentYearBlock);
                btnEdit = itemView.findViewById(R.id.btnEditStudent);
                btnKick = itemView.findViewById(R.id.btnKickStudent);
                expandableContainer = itemView.findViewById(R.id.expandableContainer);
                scoresContainer = itemView.findViewById(R.id.scoresContainer);
                loadingContainer = itemView.findViewById(R.id.loadingContainer);
                noScoresContainer = itemView.findViewById(R.id.noScoresContainer);
            }
        }
    }

    // Helper classes for detailed score display
    private static class LessonScoreResult {
        List<LessonScore> lessonScores = new ArrayList<>();
    }

    private static class LessonScore {
        String lessonName;
        int quizScore;
        int quizAttempts;
        int codeBuilderScore;
        int codeBuilderAttempts;
        int compilerScore;
        int compilerAttempts;
    }

    // Chart helper classes
    private static class ChartDataResult {
        List<LessonChartData> lessonData = new ArrayList<>();
    }

    private static class LessonChartData {
        String lessonName;
        int quizPassingPercent; // percentage 0-100
        int codeBuilderPassingPercent;
        int compilerPassingPercent;
    }

    // Legacy helper classes (kept for compatibility)
    private static class ScoreResult {
        int bestScore = 0;
        String bestLesson = "";
    }

    private static class AttemptResult {
        int latestAttempts = 0;
        String latestLesson = "";
    }
} 