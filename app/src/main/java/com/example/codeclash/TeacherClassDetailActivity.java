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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

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
import java.util.List;
import java.util.Map;

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
            android.widget.TextView btn = manageActionView.findViewById(R.id.btnManageAttempts);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    showAttemptManagementOptions();
                });
            }
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
        }

        return super.onOptionsItemSelected(item);
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

        String[] lessons = {"INTRODUCTION TO JAVA", "VARIABLES and DATA", "OPERATORS and EXPRESSIONS",
                "CONDITIONAL STATEMENTS", "CONDITIONAL LOOPS", "ARRAYS"};

        builder.setItems(lessons, (dialog, which) -> {
            String selectedLesson = lessons[which];
            showActivityTypeSelection(selectedLesson);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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

        String[] lessons = {"INTRODUCTION TO JAVA", "VARIABLES and DATA", "OPERATORS and EXPRESSIONS",
                "CONDITIONAL STATEMENTS", "CONDITIONAL LOOPS", "ARRAYS"};

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
        LineChart lineChart = dialogView.findViewById(R.id.lineChart);
        LinearLayout loadingContainer = dialogView.findViewById(R.id.loadingContainer);
        LinearLayout emptyStateContainer = dialogView.findViewById(R.id.emptyStateContainer);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnRefresh = dialogView.findViewById(R.id.btnRefresh);
        
        // Show loading state
        loadingContainer.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        lineChart.setVisibility(View.GONE);
        
        // Setup buttons
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnRefresh.setOnClickListener(v -> {
            // Show loading and refresh data
            loadingContainer.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
            loadChartData(lineChart, loadingContainer, emptyStateContainer);
        });
        
        // Load chart data
        loadChartData(lineChart, loadingContainer, emptyStateContainer);
    }
    
    private void loadChartData(LineChart lineChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer) {
        System.out.println("🔍 TeacherClassDetail: Loading chart data for class: " + classCode);
        
        String[] lessons = {"INTRODUCTION TO JAVA", "VARIABLES and DATA", "OPERATORS and EXPRESSIONS",
                "CONDITIONAL STATEMENTS", "CONDITIONAL LOOPS", "ARRAYS"};
        
        ChartDataResult result = new ChartDataResult();
        loadAllLessonChartData(result, lessons, 0, lineChart, loadingContainer, emptyStateContainer);
    }
    
    private void loadAllLessonChartData(ChartDataResult result, String[] lessons, int lessonIndex,
                                       LineChart lineChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer) {
        
        if (lessonIndex >= lessons.length) {
            // All lessons processed, render chart
            renderChart(lineChart, loadingContainer, emptyStateContainer, result);
            return;
        }
        
        String currentLesson = lessons[lessonIndex];
        System.out.println("🔍 TeacherClassDetail: Loading chart data for " + currentLesson);
        
        LessonChartData lessonData = new LessonChartData();
        lessonData.lessonName = currentLesson;
        lessonData.quizPassingCount = 0;
        lessonData.codeBuilderPassingCount = 0;
        
        // Load quiz passing count
        FirebaseFirestore.getInstance()
                .collection("Classes").document(classCode)
                .collection("Leaderboards").document(currentLesson + "_quiz")
                .collection("Scores")
                .get()
                .addOnSuccessListener(quizSnapshot -> {
                    int passingCount = 0;
                    for (DocumentSnapshot doc : quizSnapshot) {
                        Long score = doc.getLong("score");
                        if (score != null && score >= 8) { // Quiz passing threshold: 8/15
                            passingCount++;
                        }
                    }
                    lessonData.quizPassingCount = passingCount;
                    
                    // Load code builder passing count
                    FirebaseFirestore.getInstance()
                            .collection("Classes").document(classCode)
                            .collection("Leaderboards").document(currentLesson + "_code_builder")
                            .collection("Scores")
                            .get()
                            .addOnSuccessListener(codeSnapshot -> {
                                int codePassingCount = 0;
                                for (DocumentSnapshot doc : codeSnapshot) {
                                    Long score = doc.getLong("score");
                                    if (score != null && score >= 15) { // Code Builder passing threshold: 15/25
                                        codePassingCount++;
                                    }
                                }
                                lessonData.codeBuilderPassingCount = codePassingCount;
                                
                                result.lessonData.add(lessonData);
                                loadAllLessonChartData(result, lessons, lessonIndex + 1, lineChart, loadingContainer, emptyStateContainer);
                            })
                            .addOnFailureListener(e -> {
                                System.out.println("🔍 TeacherClassDetail: Error loading code builder chart data: " + e.getMessage());
                                result.lessonData.add(lessonData);
                                loadAllLessonChartData(result, lessons, lessonIndex + 1, lineChart, loadingContainer, emptyStateContainer);
                            });
                })
                .addOnFailureListener(e -> {
                    System.out.println("🔍 TeacherClassDetail: Error loading quiz chart data: " + e.getMessage());
                    result.lessonData.add(lessonData);
                    loadAllLessonChartData(result, lessons, lessonIndex + 1, lineChart, loadingContainer, emptyStateContainer);
                });
    }
    
    private void renderChart(LineChart lineChart, LinearLayout loadingContainer, LinearLayout emptyStateContainer, ChartDataResult result) {
        loadingContainer.setVisibility(View.GONE);
        
        if (result.lessonData.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            lineChart.setVisibility(View.GONE);
            return;
        }
        
        lineChart.setVisibility(View.VISIBLE);
        emptyStateContainer.setVisibility(View.GONE);
        
        // Prepare data entries
        List<Entry> quizEntries = new ArrayList<>();
        List<Entry> codeBuilderEntries = new ArrayList<>();
        
        for (int i = 0; i < result.lessonData.size(); i++) {
            LessonChartData data = result.lessonData.get(i);
            quizEntries.add(new Entry(i, data.quizPassingCount));
            codeBuilderEntries.add(new Entry(i, data.codeBuilderPassingCount));
        }
        
        // Create datasets
        LineDataSet quizDataSet = new LineDataSet(quizEntries, "Quiz");
        quizDataSet.setColor(android.graphics.Color.BLUE);
        quizDataSet.setCircleColor(android.graphics.Color.BLUE);
        quizDataSet.setLineWidth(3f);
        quizDataSet.setCircleRadius(6f);
        quizDataSet.setValueTextSize(14f);
        quizDataSet.setValueTextColor(android.graphics.Color.BLACK);
        quizDataSet.setDrawValues(true);
        quizDataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Show as integer, not decimal
            }
        });
        
        LineDataSet codeBuilderDataSet = new LineDataSet(codeBuilderEntries, "Code Builder");
        codeBuilderDataSet.setColor(android.graphics.Color.parseColor("#FFA500")); // Orange
        codeBuilderDataSet.setCircleColor(android.graphics.Color.parseColor("#FFA500"));
        codeBuilderDataSet.setLineWidth(3f);
        codeBuilderDataSet.setCircleRadius(6f);
        codeBuilderDataSet.setValueTextSize(14f);
        codeBuilderDataSet.setValueTextColor(android.graphics.Color.BLACK);
        codeBuilderDataSet.setDrawValues(true);
        codeBuilderDataSet.setValueFormatter(new ValueFormatter() {
    @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // Show as integer, not decimal
            }
        });
        
        // Combine datasets
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(quizDataSet);
        dataSets.add(codeBuilderDataSet);
        
        LineData lineData = new LineData(dataSets);
        lineChart.setData(lineData);
        
        // Configure chart appearance
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(android.graphics.Color.WHITE);
        
        // Configure X-axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(android.graphics.Color.BLACK);
        xAxis.setTextSize(12f);
        xAxis.setLabelCount(6, true); // Show all 6 lessons
        xAxis.setGranularity(1f); // No decimal values
        xAxis.setValueFormatter(new ValueFormatter() {
    @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < result.lessonData.size()) {
                    return "L" + (index + 1); // Shorter labels: L1, L2, L3, etc.
                }
                return "";
            }
        });
        
        // Configure Y-axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(android.graphics.Color.BLACK);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f); // Only show integer values
        // Calculate max value dynamically based on actual data
        float maxValue = 0f;
        for (LessonChartData data : result.lessonData) {
            maxValue = Math.max(maxValue, Math.max(data.quizPassingCount, data.codeBuilderPassingCount));
        }
        leftAxis.setAxisMaximum(Math.max(maxValue + 3, 10f)); // Add more padding
        leftAxis.setLabelCount(6, true); // Show 6 labels on Y-axis
        
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        
        // Configure legend
        lineChart.getLegend().setTextColor(android.graphics.Color.BLACK);
        lineChart.getLegend().setTextSize(10f);
        
        // Refresh chart
        lineChart.invalidate();
        
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
            // Display year and block (should now have actual values)
            String yearLevel = student.yearLevel != null ? student.yearLevel : "N/A";
            String block = student.block != null ? student.block : "N/A";
            holder.yearBlockText.setText(yearLevel + " - " + block);

            System.out.println("🔍 TeacherClassDetail: Binding student " + position + ": " + student.fullName);

            // Initially hide expandable container
            holder.expandableContainer.setVisibility(View.GONE);
            
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
                    holder.expandableContainer.setVisibility(View.VISIBLE);
                    holder.loadingContainer.setVisibility(View.VISIBLE);
                    holder.scoresContainer.setVisibility(View.GONE);
                    holder.noScoresContainer.setVisibility(View.GONE);
                    
                    // Load detailed student scores
                    loadDetailedStudentScores(student, holder);
                } else {
                    // Collapse
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

            // Clear existing scores
            holder.scoresContainer.removeAllViews();

            // Get all lessons from the class document
            String[] lessons = {"INTRODUCTION TO JAVA", "VARIABLES and DATA", "OPERATORS and EXPRESSIONS",
                    "CONDITIONAL STATEMENTS", "CONDITIONAL LOOPS", "ARRAYS"};

            LessonScoreResult result = new LessonScoreResult();
            loadAllLessonScores(student, holder, result, lessons, 0);
        }

        private void loadAllLessonScores(Student student, StudentViewHolder holder, LessonScoreResult result,
                                         String[] lessons, int lessonIndex) {

            if (lessonIndex >= lessons.length) {
                // All lessons checked, display results
                displayLessonScores(student, holder, result);
                return;
            }

            String currentLesson = lessons[lessonIndex];
            System.out.println("🔍 TeacherClassDetail: Loading scores for " + currentLesson + " for " + student.fullName);

            // Create lesson score entry
            LessonScore lessonScore = new LessonScore();
            lessonScore.lessonName = currentLesson;
            lessonScore.quizScore = 0;
            lessonScore.quizAttempts = 0;
            lessonScore.codeBuilderScore = 0;
            lessonScore.codeBuilderAttempts = 0;

            // Load quiz score
            FirebaseFirestore.getInstance()
                    .collection("Classes").document(classCode)
                    .collection("Leaderboards").document(currentLesson + "_quiz")
                    .collection("Scores").document(student.studentId)
                    .get()
                    .addOnSuccessListener(quizDoc -> {
                        if (quizDoc.exists()) {
                            Long score = quizDoc.getLong("score");
                            Long attempts = quizDoc.getLong("attemptsUsed");
                            if (score != null) lessonScore.quizScore = score.intValue();
                            if (attempts != null) lessonScore.quizAttempts = attempts.intValue();
                            System.out.println("🔍 TeacherClassDetail: Quiz score for " + currentLesson + ": " + lessonScore.quizScore);
                        }

                        // Load code builder score
                        FirebaseFirestore.getInstance()
                                .collection("Classes").document(classCode)
                                .collection("Leaderboards").document(currentLesson + "_code_builder")
                                .collection("Scores").document(student.studentId)
                                .get()
                                .addOnSuccessListener(codeDoc -> {
                                    if (codeDoc.exists()) {
                                        Long score = codeDoc.getLong("score");
                                        Long attempts = codeDoc.getLong("attemptsUsed");
                                        if (score != null) lessonScore.codeBuilderScore = score.intValue();
                                        if (attempts != null) lessonScore.codeBuilderAttempts = attempts.intValue();
                                        System.out.println("🔍 TeacherClassDetail: Code Builder score for " + currentLesson + ": " + lessonScore.codeBuilderScore);
                                    }

                                    // Add to results
                                    result.lessonScores.add(lessonScore);

                                    // Load next lesson
                                    loadAllLessonScores(student, holder, result, lessons, lessonIndex + 1);
                                })
                                .addOnFailureListener(e -> {
                                    System.out.println("🔍 TeacherClassDetail: Error loading code builder score for " + currentLesson + ": " + e.getMessage());
                                    result.lessonScores.add(lessonScore);
                                    loadAllLessonScores(student, holder, result, lessons, lessonIndex + 1);
                                });
                    })
                    .addOnFailureListener(e -> {
                        System.out.println("🔍 TeacherClassDetail: Error loading quiz score for " + currentLesson + ": " + e.getMessage());
                        result.lessonScores.add(lessonScore);
                        loadAllLessonScores(student, holder, result, lessons, lessonIndex + 1);
                    });
        }

        private void displayLessonScores(Student student, StudentViewHolder holder, LessonScoreResult result) {
            // Hide loading
            holder.loadingContainer.setVisibility(View.GONE);

            if (result.lessonScores.isEmpty()) {
                holder.noScoresContainer.setVisibility(View.VISIBLE);
                holder.scoresContainer.setVisibility(View.GONE);
                return;
            }

            holder.scoresContainer.setVisibility(View.VISIBLE);
            holder.noScoresContainer.setVisibility(View.GONE);

            // Add lesson score views
            for (LessonScore lessonScore : result.lessonScores) {
                View lessonView = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_lesson_score, holder.scoresContainer, false);

                TextView lessonNameText = lessonView.findViewById(R.id.lessonName);
                TextView quizScoreText = lessonView.findViewById(R.id.quizScore);
                TextView quizAttemptsText = lessonView.findViewById(R.id.quizAttempts);
                TextView codeBuilderScoreText = lessonView.findViewById(R.id.codeBuilderScore);
                TextView codeBuilderAttemptsText = lessonView.findViewById(R.id.codeBuilderAttempts);

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

                holder.scoresContainer.addView(lessonView);
            }

            System.out.println("🔍 TeacherClassDetail: Displayed " + result.lessonScores.size() + " lesson scores for " + student.fullName);
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
    }

    // Chart helper classes
    private static class ChartDataResult {
        List<LessonChartData> lessonData = new ArrayList<>();
    }

    private static class LessonChartData {
        String lessonName;
        int quizPassingCount;
        int codeBuilderPassingCount;
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