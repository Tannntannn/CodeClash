package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import androidx.cardview.widget.CardView;

public class StudentClasses extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private LinearLayout classListContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_classes);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        classListContainer = findViewById(R.id.classListContainer); // declared in layout
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        
        // Setup swipe refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            classListContainer.removeAllViews(); // Clear existing classes
            loadStudentClasses();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadStudentClasses();
    }

    private void loadStudentClasses() {
        // Check network connectivity first
        if (!NetworkManager.isNetworkAvailable(this)) {
            NetworkManager.showOfflineMessage(this);
            // Still try to load from cache (offline persistence)
        }

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Load classes from the student's membership fan-out for reliable access under rules
        firestore.collection("Users").document(currentUserId)
                .collection("MyJoinedClasses")
                .get()
                .addOnSuccessListener(joinedSnapshot -> {
                    if (joinedSnapshot.isEmpty()) {
                        Toast.makeText(this, "No joined classes", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }

                    // Track completion of all class loading
                    int totalClasses = joinedSnapshot.size();
                    final int[] completedClasses = {0};
                    
                    for (DocumentSnapshot joinedDoc : joinedSnapshot.getDocuments()) {
                        String classCode = joinedDoc.getId();
                        // Fetch the class document (rules allow GET for enrolled students)
                        firestore.collection("Classes").document(classCode)
                                .get()
                                .addOnSuccessListener(classDoc -> {
                                    if (!classDoc.exists()) {
                                        completedClasses[0]++;
                                        if (completedClasses[0] >= totalClasses) {
                                            swipeRefreshLayout.setRefreshing(false);
                                        }
                                        return;
                                    }
                String yearLevel = classDoc.getString("yearLevel");
                String block = classDoc.getString("block");
                                    String teacherId = classDoc.getString("createdBy");
                                    addClassCard(classCode, yearLevel, block, teacherId);
                                    
                                    completedClasses[0]++;
                                    if (completedClasses[0] >= totalClasses) {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("StudentClasses", "Failed to load class " + classCode + ": " + e.getMessage());
                                    completedClasses[0]++;
                                    if (completedClasses[0] >= totalClasses) {
                                        swipeRefreshLayout.setRefreshing(false);
                            }
                        });
            }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load student classes", Toast.LENGTH_SHORT).show();
                    Log.e("StudentClasses", "Failed to load MyJoinedClasses: " + e.getMessage());
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void addClassCard(String classCode, String yearLevel, String block) {
        addClassCard(classCode, yearLevel, block, null);
    }
    
    private void addClassCard(String classCode, String yearLevel, String block, String teacherId) {
        CardView cardView = new CardView(this);
        cardView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        cardView.setCardElevation(12f);
        cardView.setRadius(24f);
        cardView.setUseCompatPadding(true);
        cardView.setContentPadding(0, 0, 0, 0);
        cardView.setClickable(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // WHITE HEADER BAR WITH CLASS CODE
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setBackgroundColor(getResources().getColor(R.color.game_red_primary));
        headerLayout.setPadding(16, 16, 16, 16);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView headerTitle = new TextView(this);
        headerTitle.setText("🎓 CLASS");
        headerTitle.setTextColor(getResources().getColor(R.color.white));
        headerTitle.setTextSize(16);
        headerTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        headerTitle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        headerLayout.addView(headerTitle);

        // CONTENT AREA
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.HORIZONTAL);
        contentLayout.setPadding(20, 20, 20, 20);
        contentLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // CLASS ICON
        LinearLayout iconLayout = new LinearLayout(this);
        iconLayout.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        iconLayout.setBackgroundColor(getResources().getColor(R.color.game_red_primary));
        iconLayout.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(64, 64);
        iconParams.setMargins(0, 0, 20, 0);
        iconLayout.setLayoutParams(iconParams);

        TextView iconText = new TextView(this);
        iconText.setText("🎮");
        iconText.setTextSize(28);
        iconLayout.addView(iconText);

        // CLASS INFO
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoLayout.setLayoutParams(infoParams);

        TextView yearBlockText = new TextView(this);
        yearBlockText.setText("Year " + yearLevel + " • Block " + block);
        yearBlockText.setTextSize(18);
        yearBlockText.setTypeface(null, android.graphics.Typeface.BOLD);
        yearBlockText.setTextColor(getResources().getColor(R.color.game_text_primary));
        LinearLayout.LayoutParams yearParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        yearParams.setMargins(0, 0, 0, 4);
        yearBlockText.setLayoutParams(yearParams);

        TextView teacherText = new TextView(this);
        teacherText.setText("👨‍🏫 Loading teacher...");
        teacherText.setTextSize(14);
        teacherText.setTextColor(getResources().getColor(R.color.game_text_secondary));

        // Add class code display
        TextView classCodeDisplay = new TextView(this);
        classCodeDisplay.setText("Class Code: " + classCode);
        classCodeDisplay.setTextSize(14);
        classCodeDisplay.setTextColor(getResources().getColor(R.color.game_text_primary));
        classCodeDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams classCodeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        classCodeParams.setMargins(0, 4, 0, 0);
        classCodeDisplay.setLayoutParams(classCodeParams);

        infoLayout.addView(yearBlockText);
        infoLayout.addView(teacherText);
        infoLayout.addView(classCodeDisplay);

        contentLayout.addView(iconLayout);
        contentLayout.addView(infoLayout);

        mainLayout.addView(headerLayout);
        mainLayout.addView(contentLayout);
        cardView.addView(mainLayout);

        // Load teacher name asynchronously
        if (teacherId != null && !teacherId.isEmpty()) {
            UserNameManager.getTeacherName(teacherId, new UserNameManager.NameCallback() {
                @Override
                public void onSuccess(String teacherName) {
                    teacherText.setText("👨‍🏫 " + teacherName);
                }
                
                @Override
                public void onFailure(String fallbackName) {
                    teacherText.setText("👨‍🏫 Teacher not found");
                }
            });
        } else {
            teacherText.setText("👨‍🏫 Teacher Unknown");
        }

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(this, LessonsActivity.class);
            intent.putExtra("classCode", classCode);
            intent.putExtra("yearLevel", yearLevel);
            intent.putExtra("block", block);
            intent.putExtra("isTeacher", false); // Student role
            startActivity(intent);
        });

        classListContainer.addView(cardView);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
