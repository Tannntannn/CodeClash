package com.example.codeclash.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
// Removed unused imports
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.codeclash.LeaderboardManager;
import com.example.codeclash.NetworkManager;
import com.example.codeclash.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class Leaderboards extends Fragment {

    View view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LeaderboardAdapter adapter;
    private List<LeaderboardManager.LeaderboardEntry> leaderboardEntries = new ArrayList<>();
    private TabLayout activityTabLayout;
    private TabLayout lessonTabLayout;
    private TextView emptyStateText;
    private View emptyStateContainer;
    private String currentClassCode;
    private String currentLesson;
    private String currentActivityType = LeaderboardManager.ACTIVITY_QUIZ;
    private String[] lessons = {
            "INTRODUCTION TO JAVA",
            "VARIABLES and DATA",
            "OPERATORS and EXPRESSIONS",
            "CONDITIONAL STATEMENTS",
            "CONDITIONAL LOOPS",
            "ARRAYS"
    };
    private ListenerRegistration currentLeaderboardListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragments_leaderboards, container, false);

        initViews();
        setupTabs();
        setupLessonTabs();
        setupRefresh();
        loadLeaderboard();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listener to prevent memory leaks
        if (currentLeaderboardListener != null) {
            currentLeaderboardListener.remove();
            currentLeaderboardListener = null;
            System.out.println("🏆 Leaderboards: Removed listener in onDestroyView");
        }
    }

    private void initViews() {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        activityTabLayout = view.findViewById(R.id.activityTabs);
        lessonTabLayout = view.findViewById(R.id.lessonTabs);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);

        System.out.println("🏆 Leaderboards: initViews - emptyStateText found? " + (emptyStateText != null));
        System.out.println("🏆 Leaderboards: initViews - recyclerView found? " + (recyclerView != null));

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new LeaderboardAdapter(leaderboardEntries);
        recyclerView.setAdapter(adapter);

        // Set initial empty state
        if (emptyStateText != null && emptyStateContainer != null) {
            emptyStateText.setText("Loading...");
            emptyStateContainer.setVisibility(View.VISIBLE);
            System.out.println("🏆 Leaderboards: Set initial loading state");
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        // Obtain class code and lesson from fragment args or hosting activity intent
        Bundle args = getArguments();
        if (args != null) {
            currentClassCode = args.getString("classCode", null);
            currentLesson = args.getString("lessonName", null);
            System.out.println("🏆 Leaderboards: From args - Class: " + currentClassCode + ", Lesson: " + currentLesson);
        } else {
            System.out.println("❌ Leaderboards: No arguments provided");
        }

        if (currentClassCode == null || currentLesson == null) {
            if (getActivity() != null && getActivity().getIntent() != null) {
                currentClassCode = getActivity().getIntent().getStringExtra("classCode");
                currentLesson = getActivity().getIntent().getStringExtra("lessonName");
                System.out.println("🏆 Leaderboards: From activity intent - Class: " + currentClassCode + ", Lesson: " + currentLesson);
            } else {
                System.out.println("❌ Leaderboards: No activity or intent available");
            }
        }

        // Set default lesson if none provided
        if (currentLesson == null) {
            currentLesson = lessons[0]; // Default to first lesson
            System.out.println("🏆 Leaderboards: Set default lesson: " + currentLesson);
        }

        System.out.println("🏆 Leaderboards: Final values - Class: " + currentClassCode + ", Lesson: " + currentLesson);
    }

    private void setupRefresh() {
        if (swipeRefreshLayout == null) return;
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadLeaderboard();
        });
    }

    private void setupTabs() {
        activityTabLayout.addTab(activityTabLayout.newTab().setText("Quiz"));
        activityTabLayout.addTab(activityTabLayout.newTab().setText("Code Builder"));
        activityTabLayout.addTab(activityTabLayout.newTab().setText("Compiler"));

        activityTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentActivityType = LeaderboardManager.ACTIVITY_QUIZ;
                        break;
                    case 1:
                        currentActivityType = LeaderboardManager.ACTIVITY_CODE_BUILDER;
                        break;
                    case 2:
                        currentActivityType = LeaderboardManager.ACTIVITY_COMPILER;
                        break;
                }
                loadLeaderboard();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupLessonTabs() {
        // Add lesson tabs with shorter names
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 1"));
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 2"));
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 3"));
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 4"));
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 5"));
        lessonTabLayout.addTab(lessonTabLayout.newTab().setText("Lesson 6"));

        // Set default lesson if none specified
        if (currentLesson == null || currentLesson.isEmpty()) {
            currentLesson = lessons[0]; // Default to first lesson
            lessonTabLayout.selectTab(lessonTabLayout.getTabAt(0));
        } else {
            // Select the tab matching current lesson
            for (int i = 0; i < lessons.length; i++) {
                if (lessons[i].equals(currentLesson)) {
                    lessonTabLayout.selectTab(lessonTabLayout.getTabAt(i));
                    break;
                }
            }
        }

        lessonTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentLesson = lessons[tab.getPosition()];
                System.out.println("🏆 Leaderboards: Selected lesson: " + currentLesson + " (tab position: " + tab.getPosition() + ")");
                System.out.println("🏆 Leaderboards: Current activity type: " + currentActivityType);
                loadLeaderboard();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLeaderboard() {
        System.out.println("🏆 Leaderboards: loadLeaderboard() called");
        System.out.println("🏆 Leaderboards: currentClassCode = " + currentClassCode);
        System.out.println("🏆 Leaderboards: currentLesson = " + currentLesson);
        System.out.println("🏆 Leaderboards: currentActivityType = " + currentActivityType);

        // Remove old listener if exists
        if (currentLeaderboardListener != null) {
            currentLeaderboardListener.remove();
            currentLeaderboardListener = null;
            System.out.println("🏆 Leaderboards: Removed old listener");
        }

        // Check network connectivity first
        if (getContext() != null && !NetworkManager.isNetworkAvailable(getContext())) {
            NetworkManager.showOfflineMessage(getContext());
            // Still try to load from cache (offline persistence)
        }

        if (currentClassCode == null) {
            System.out.println("🏆 Leaderboards: Missing classCode, loading general leaderboard");
            // Try to load a general leaderboard from all classes
            loadGeneralLeaderboard();
            return;
        }
        
        // Clear existing data first
        leaderboardEntries.clear();
        adapter.notifyDataSetChanged();

        // Show loading state
        emptyStateText.setText("Loading...");
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        // Determine if user is teacher (check if they created classes)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isTeacher = false;
        if (currentUser != null) {
            // Quick check - if they're viewing a specific class, assume student view
            // For real-time, we'll use top 10 for now (can be enhanced later)
            isTeacher = false; // Simplified for now
        }

        // Add real-time listener - limit to top 10 for students
        currentLeaderboardListener = LeaderboardManager.addRealtimeScoresListener(
            currentClassCode, currentLesson, currentActivityType, 
            10, // Limit to top 10
            new LeaderboardManager.OnLeaderboardCallback() {
                @Override
                public void onSuccess(List<LeaderboardManager.LeaderboardEntry> entries) {
                        if (!isAdded() || getActivity() == null) return;

                    leaderboardEntries.clear();
                    leaderboardEntries.addAll(entries);
                    adapter.notifyDataSetChanged();
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                    if (entries.isEmpty()) {
                            System.out.println("🏆 Leaderboards: No entries found, showing empty state");
                            if (emptyStateText != null && emptyStateContainer != null) {
                                if (currentActivityType.equals(LeaderboardManager.ACTIVITY_QUIZ)) {
                                    emptyStateText.setText("No quiz scores yet");
                                } else if (currentActivityType.equals(LeaderboardManager.ACTIVITY_CODE_BUILDER)) {
                                    emptyStateText.setText("No code builder scores yet");
                                } else if (currentActivityType.equals(LeaderboardManager.ACTIVITY_COMPILER)) {
                                    emptyStateText.setText("No compiler scores yet");
                                } else {
                                    emptyStateText.setText("No scores yet");
                                }
                                emptyStateContainer.setVisibility(View.VISIBLE);
                            }
                            if (recyclerView != null) {
                        recyclerView.setVisibility(View.GONE);
                            }
                    } else {
                        System.out.println("🏆 Leaderboards: Real-time update - Showing " + entries.size() + " entries");
                            if (emptyStateContainer != null) {
                                emptyStateContainer.setVisibility(View.GONE);
                            }
                            if (recyclerView != null) {
                        recyclerView.setVisibility(View.VISIBLE);
                            }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                        if (!isAdded() || getActivity() == null) return;

                    emptyStateText.setText("Failed to load leaderboard");
                        emptyStateContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                }
            });
    }

    // Adapter for leaderboard entries
    private static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardViewHolder> {
        private List<LeaderboardManager.LeaderboardEntry> entries;

        public LeaderboardAdapter(List<LeaderboardManager.LeaderboardEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard_entry, parent, false);
            return new LeaderboardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
            LeaderboardManager.LeaderboardEntry entry = entries.get(position);
            
            holder.rankText.setText("#" + (position + 1));

            // Show student name with class and lesson info
            String displayName = entry.studentName;
            if (entry.classCode != null && entry.lessonName != null && entry.activityType != null) {
                String activityDisplay;
                if (entry.activityType.equals(LeaderboardManager.ACTIVITY_QUIZ)) {
                    activityDisplay = "Quiz";
                } else if (entry.activityType.equals(LeaderboardManager.ACTIVITY_CODE_BUILDER)) {
                    activityDisplay = "Code Builder";
                } else if (entry.activityType.equals(LeaderboardManager.ACTIVITY_COMPILER)) {
                    activityDisplay = "Compiler";
                } else {
                    activityDisplay = "Unknown";
                }
                displayName += "\n" + entry.classCode + " • " + entry.lessonName + " • " + activityDisplay;
            }
            holder.studentNameText.setText(displayName);

            holder.scoreText.setText(String.valueOf(entry.score));
            holder.attemptsText.setText(entry.attemptsUsed + "/" + LeaderboardManager.MAX_ATTEMPTS + " attempts");

            // Highlight top 3 positions
            if (position == 0) {
                holder.rankText.setTextColor(0xFFFFD700); // Gold
            } else if (position == 1) {
                holder.rankText.setTextColor(0xFFC0C0C0); // Silver
            } else if (position == 2) {
                holder.rankText.setTextColor(0xFFCD7F32); // Bronze
            } else {
                holder.rankText.setTextColor(0xFF666666); // Gray
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, studentNameText, scoreText, attemptsText;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            scoreText = itemView.findViewById(R.id.scoreText);
            attemptsText = itemView.findViewById(R.id.attemptsText);
        }
    }

    private void loadGeneralLeaderboard() {
        System.out.println("🏆 Leaderboards: Loading general leaderboard from all classes...");

        // Clear existing data first
        leaderboardEntries.clear();
        adapter.notifyDataSetChanged();

        emptyStateText.setText("Loading...");
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            emptyStateText.setText("Please log in to view leaderboards");
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }
        String userId = currentUser.getUid();

        // Try to load as teacher first
        FirebaseFirestore.getInstance()
                .collection("Classes")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(teacherClassesSnapshot -> {
                    if (!teacherClassesSnapshot.isEmpty()) {
                        System.out.println("🏆 Leaderboards: User is a teacher, loading from " + teacherClassesSnapshot.size() + " classes");
                        loadAllClassLeaderboards(teacherClassesSnapshot);
                    } else {
                        // Try to load as student
                        loadStudentClassesAsFallback();
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Leaderboards: Failed to load teacher classes: " + e.getMessage());
                    loadStudentClassesAsFallback();
                });
    }

    private void loadStudentClasses() {
        System.out.println("🏆 Leaderboards: Loading user's classes...");
        emptyStateText.setText("Loading your classes...");
        emptyStateContainer.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Please log in again", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
            return;
        }
        String userId = currentUser.getUid();
        System.out.println("🏆 Leaderboards: Current user ID: " + userId);

        // First, try to load as teacher (check if user has created classes)
        System.out.println("🏆 Leaderboards: Querying for teacher classes with createdBy: " + userId);
        FirebaseFirestore.getInstance()
                .collection("Classes")
                .whereEqualTo("createdBy", userId)
                .get()
                .addOnSuccessListener(teacherClassesSnapshot -> {
                    if (!teacherClassesSnapshot.isEmpty()) {
                        System.out.println("🏆 Leaderboards: User is a teacher with " + teacherClassesSnapshot.size() + " classes");
                        loadAllClassLeaderboards(teacherClassesSnapshot);
                    } else {
                        // Teacher with no classes - try to load as student
                        System.out.println("🏆 Leaderboards: No classes found with createdBy: " + userId);
                        System.out.println("🏆 Leaderboards: User is not a teacher, checking student classes...");
                        loadStudentClassesAsFallback();
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Leaderboards: Failed to check teacher classes: " + e.getMessage());
                    // If teacher check fails, try to load as student
                    loadStudentClassesAsFallback();
                });
    }

    private void loadStudentClassesAsFallback() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Please log in again", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
            return;
        }
        String userId = currentUser.getUid();
        System.out.println("🏆 Leaderboards: User is not a teacher, checking student classes...");
        System.out.println("🏆 Leaderboards: Querying MyJoinedClasses for user: " + userId);
        FirebaseFirestore.getInstance()
                .collection("Users").document(userId)
                .collection("MyJoinedClasses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    System.out.println("🏆 Leaderboards: Student classes query result: " + querySnapshot.size() + " classes");
                    if (querySnapshot.isEmpty()) {
                        System.out.println("🏆 Leaderboards: Student has no joined classes");
                        emptyStateText.setText("You haven't joined any classes yet.\n\nJoin a class to see leaderboards!");
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        return;
                    }

                    System.out.println("🏆 Leaderboards: User is a student with " + querySnapshot.size() + " joined classes");
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        System.out.println("🏆 Leaderboards: Student joined class: " + doc.getId() + " - " + doc.getString("classCode"));
                    }
                    // Load leaderboards from all classes - convert student classes to class documents
                    loadStudentClassLeaderboards(querySnapshot);
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Leaderboards: Failed to load student classes: " + e.getMessage());
                    emptyStateText.setText("Failed to load your classes");
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadStudentClassLeaderboards(com.google.firebase.firestore.QuerySnapshot studentClassesSnapshot) {
        System.out.println("🏆 Leaderboards: Loading leaderboards from " + studentClassesSnapshot.size() + " student classes");

        leaderboardEntries.clear();
        java.util.concurrent.atomic.AtomicInteger completedQueries = new java.util.concurrent.atomic.AtomicInteger(0);
        int totalQueries = studentClassesSnapshot.size(); // Only 1 lesson per class now

        String lessonToLoad = currentLesson != null ? currentLesson : lessons[0];
        System.out.println("🏆 Leaderboards: Loading student class leaderboards for lesson: " + lessonToLoad + ", activity: " + currentActivityType);

        for (com.google.firebase.firestore.DocumentSnapshot studentClassDoc : studentClassesSnapshot) {
            String classCode = studentClassDoc.getString("classCode");
            if (classCode == null) {
                classCode = studentClassDoc.getId(); // fallback to document ID
            }
            final String finalClassCode = classCode; // Make it final for lambda
            System.out.println("🏆 Leaderboards: Loading leaderboard for student class: " + finalClassCode);

            // Load leaderboard for selected lesson and activity type only (student: top 10)
            loadClassLeaderboardStudent(finalClassCode, lessonToLoad, currentActivityType, completedQueries, totalQueries);
        }
    }

    private void loadAllClassLeaderboards(com.google.firebase.firestore.QuerySnapshot classSnapshot) {
        System.out.println("🏆 Leaderboards: Loading leaderboards from " + classSnapshot.size() + " classes");

        leaderboardEntries.clear();
        java.util.concurrent.atomic.AtomicInteger completedQueries = new java.util.concurrent.atomic.AtomicInteger(0);
        int totalQueries = classSnapshot.size(); // Only 1 lesson per class now

        String lessonToLoad = currentLesson != null ? currentLesson : lessons[0];
        System.out.println("🏆 Leaderboards: Loading leaderboards for lesson: " + lessonToLoad + ", activity: " + currentActivityType);

        for (com.google.firebase.firestore.DocumentSnapshot classDoc : classSnapshot) {
            String classCode = classDoc.getId();
            System.out.println("🏆 Leaderboards: Loading leaderboard for class: " + classCode);

            // Load leaderboard for selected lesson and activity type only (teacher: all)
            loadClassLeaderboardTeacher(classCode, lessonToLoad, currentActivityType, completedQueries, totalQueries);
        }
    }

    private void loadClassLeaderboardStudent(String classCode, String lessonName, String activityType,
                                      java.util.concurrent.atomic.AtomicInteger completedQueries, int totalQueries) {
        System.out.println("🏆 Leaderboards (Student): Loading " + classCode + " " + lessonName + " " + activityType);

        LeaderboardManager.getTopScores(classCode, lessonName, activityType,
                new LeaderboardManager.OnLeaderboardCallback() {
                    @Override
                    public void onSuccess(List<LeaderboardManager.LeaderboardEntry> entries) {
                        System.out.println("🏆 Leaderboards: Found " + entries.size() + " entries for " + classCode + " " + lessonName + " " + activityType);

                        // Add class and lesson info to entries
                        for (LeaderboardManager.LeaderboardEntry entry : entries) {
                            entry.classCode = classCode;
                            entry.lessonName = lessonName;
                            entry.activityType = activityType;
                        }
                        leaderboardEntries.addAll(entries);

                        int currentCompleted = completedQueries.incrementAndGet();
                        System.out.println("🏆 Leaderboards: Completed " + currentCompleted + "/" + totalQueries + " queries");
                        if (currentCompleted >= totalQueries) {
                            // All queries completed, update UI
                            System.out.println("🏆 Leaderboards: All queries completed, updating UI with " + leaderboardEntries.size() + " total entries");
                            updateLeaderboardUI();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        System.out.println("❌ Leaderboards: Failed to load " + classCode + " " + lessonName + " " + activityType + ": " + e.getMessage());

                        int currentCompleted = completedQueries.incrementAndGet();
                        System.out.println("🏆 Leaderboards: Completed " + currentCompleted + "/" + totalQueries + " queries (failed)");
                        if (currentCompleted >= totalQueries) {
                            // All queries completed, update UI
                            System.out.println("🏆 Leaderboards: All queries completed, updating UI with " + leaderboardEntries.size() + " total entries");
                            updateLeaderboardUI();
                        }
                    }
                });
    }

    private void loadClassLeaderboardTeacher(String classCode, String lessonName, String activityType,
                                      java.util.concurrent.atomic.AtomicInteger completedQueries, int totalQueries) {
        System.out.println("🏆 Leaderboards (Teacher): Loading " + classCode + " " + lessonName + " " + activityType);

        LeaderboardManager.getAllScores(classCode, lessonName, activityType,
                new LeaderboardManager.OnLeaderboardCallback() {
                    @Override
                    public void onSuccess(List<LeaderboardManager.LeaderboardEntry> entries) {
                        System.out.println("🏆 Leaderboards: Found " + entries.size() + " entries for " + classCode + " " + lessonName + " " + activityType);

                        // Add class and lesson info to entries
                        for (LeaderboardManager.LeaderboardEntry entry : entries) {
                            entry.classCode = classCode;
                            entry.lessonName = lessonName;
                            entry.activityType = activityType;
                        }
                        leaderboardEntries.addAll(entries);

                        int currentCompleted = completedQueries.incrementAndGet();
                        System.out.println("🏆 Leaderboards: Completed " + currentCompleted + "/" + totalQueries + " queries");
                        if (currentCompleted >= totalQueries) {
                            // All queries completed, update UI
                            System.out.println("🏆 Leaderboards: All queries completed, updating UI with " + leaderboardEntries.size() + " total entries");
                            updateLeaderboardUI();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        System.out.println("❌ Leaderboards: Failed to load " + classCode + " " + lessonName + " " + activityType + ": " + e.getMessage());

                        int currentCompleted = completedQueries.incrementAndGet();
                        System.out.println("🏆 Leaderboards: Completed " + currentCompleted + "/" + totalQueries + " queries (failed)");
                        if (currentCompleted >= totalQueries) {
                            // All queries completed, update UI
                            System.out.println("🏆 Leaderboards: All queries completed, updating UI with " + leaderboardEntries.size() + " total entries");
                            updateLeaderboardUI();
                        }
                    }
                });
    }

    private void updateLeaderboardUI() {
        if (!isAdded() || getActivity() == null) return;

        System.out.println("🏆 Leaderboards: updateLeaderboardUI() called with " + leaderboardEntries.size() + " entries");

        // Sort by score (highest first)
        leaderboardEntries.sort((a, b) -> Integer.compare(b.score, a.score));

        // Keep only top 20 entries
        if (leaderboardEntries.size() > 20) {
            leaderboardEntries = leaderboardEntries.subList(0, 20);
            System.out.println("🏆 Leaderboards: Trimmed to top 20 entries");
        }

        adapter.notifyDataSetChanged();

        // CRITICAL: Stop the swipe refresh animation
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
            System.out.println("🏆 Leaderboards: Stopped swipe refresh animation");
        }

        if (leaderboardEntries.isEmpty()) {
            System.out.println("🏆 Leaderboards: No entries found, showing empty state");
            if (currentActivityType.equals(LeaderboardManager.ACTIVITY_QUIZ)) {
                emptyStateText.setText("No quiz scores yet");
            } else if (currentActivityType.equals(LeaderboardManager.ACTIVITY_CODE_BUILDER)) {
                emptyStateText.setText("No code builder scores yet");
            } else if (currentActivityType.equals(LeaderboardManager.ACTIVITY_COMPILER)) {
                emptyStateText.setText("No compiler scores yet");
            } else {
                emptyStateText.setText("No scores yet");
            }
            emptyStateContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            System.out.println("🏆 Leaderboards: Showing " + leaderboardEntries.size() + " entries");
            emptyStateContainer.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
