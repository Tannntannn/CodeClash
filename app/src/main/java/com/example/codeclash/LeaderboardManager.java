package com.example.codeclash;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderboardManager {
    private static FirebaseFirestore db = null;
    
    private static FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }
    
    public static void cleanup() {
        if (db != null) {
            // Firebase Firestore doesn't need explicit cleanup, but we can null the reference
            db = null;
        }
    }
    
    // Activity types
    public static final String ACTIVITY_QUIZ = "quiz";
    public static final String ACTIVITY_CODE_BUILDER = "code_builder";
    public static final String ACTIVITY_COMPILER = "compiler";
    
    // Maximum attempts per activity
    public static final int MAX_ATTEMPTS = 3;
    
    /**
     * Record a student's score for an activity
     * Only keeps the highest score if multiple attempts exist
     */
    public static void recordScore(String classCode, String lessonName, String activityType, 
                                  String studentId, String studentName, int score, int attemptsUsed) {
        System.out.println("🏆 LeaderboardManager: recordScore() called");
        System.out.println("🏆 LeaderboardManager: Class: " + classCode + ", Lesson: " + lessonName + ", Activity: " + activityType);
        System.out.println("🏆 LeaderboardManager: Student: " + studentId + " (" + studentName + "), Score: " + score + ", Attempts: " + attemptsUsed);
        
        // Validate inputs
        if (classCode == null || lessonName == null || activityType == null || studentId == null || studentName == null) {
            System.out.println("❌ LeaderboardManager: Invalid input parameters");
            return;
        }
        
        // Skip scoring for compiler activity
        if (ACTIVITY_COMPILER.equals(activityType)) {
            System.out.println("🏆 LeaderboardManager: Skipping compiler activity (no leaderboard)");
            return;
        }
        
        // Check if student already has a score for this activity
        getDb().collection("Classes").document(classCode)
          .collection("Leaderboards").document(lessonName + "_" + activityType)
          .collection("Scores").document(studentId)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              
              if (documentSnapshot.exists()) {
                  // Student already has a score
                  int existingScore = documentSnapshot.getLong("score").intValue();
                  int existingAttempts = documentSnapshot.getLong("attemptsUsed").intValue();
                  
                  // Always update attemptsUsed to reflect current attempt count
                  // But only update score if it's better
                  if (score > existingScore) {
                      // Update with higher score and current attempts
                      updateScore(classCode, lessonName, activityType, studentId, studentName, score, attemptsUsed);
                  } else if (score == existingScore && attemptsUsed < existingAttempts) {
                      // Same score but fewer attempts used
                      updateScore(classCode, lessonName, activityType, studentId, studentName, score, attemptsUsed);
                  } else if (attemptsUsed != existingAttempts) {
                      // Score didn't improve, but attempts changed - update attempts only
                      // Using set() instead of update() ensures listeners are always triggered
                      updateAttemptsOnly(classCode, lessonName, activityType, studentId, attemptsUsed);
                  }
                  // If score and attempts are both the same, no update needed
              } else {
                  // First attempt, record the score
                  updateScore(classCode, lessonName, activityType, studentId, studentName, score, attemptsUsed);
              }
          });
    }
    
    private static void updateScore(String classCode, String lessonName, String activityType,
                                   String studentId, String studentName, int score, int attemptsUsed) {
        
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("studentId", studentId);
        scoreData.put("studentName", studentName);
        scoreData.put("score", score);
        scoreData.put("attemptsUsed", attemptsUsed);
        scoreData.put("timestamp", System.currentTimeMillis());
        
        String path = "Classes/" + classCode + "/Leaderboards/" + lessonName + "_" + activityType + "/Scores/" + studentId;
        System.out.println("🏆 LeaderboardManager: Writing score → " + path + " data=" + scoreData);
        getDb().collection("Classes").document(classCode)
          .collection("Leaderboards").document(lessonName + "_" + activityType)
          .collection("Scores").document(studentId)
          .set(scoreData)
          .addOnSuccessListener(v -> System.out.println("✅ LeaderboardManager: Score write success: " + path))
          .addOnFailureListener(e -> System.out.println("❌ LeaderboardManager: Score write FAILED: " + path + " error=" + e.getMessage()));
    }
    
    /**
     * Update only attemptsUsed field without changing score
     * Uses set with merge to ensure the update always succeeds and triggers listeners
     */
    private static void updateAttemptsOnly(String classCode, String lessonName, String activityType,
                                           String studentId, int attemptsUsed) {
        // First, get the existing document to preserve score and studentName
        getDb().collection("Classes").document(classCode)
          .collection("Leaderboards").document(lessonName + "_" + activityType)
          .collection("Scores").document(studentId)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot.exists()) {
                  // Preserve existing data and only update attempts and timestamp
                  Map<String, Object> updateData = new HashMap<>();
                  updateData.put("studentId", documentSnapshot.getString("studentId"));
                  updateData.put("studentName", documentSnapshot.getString("studentName"));
                  updateData.put("score", documentSnapshot.getLong("score"));
                  updateData.put("attemptsUsed", attemptsUsed);
                  updateData.put("timestamp", System.currentTimeMillis());
                  
                  String path = "Classes/" + classCode + "/Leaderboards/" + lessonName + "_" + activityType + "/Scores/" + studentId;
                  System.out.println("🏆 LeaderboardManager: Updating attempts only → " + path + " attemptsUsed=" + attemptsUsed);
                  getDb().collection("Classes").document(classCode)
                    .collection("Leaderboards").document(lessonName + "_" + activityType)
                    .collection("Scores").document(studentId)
                    .set(updateData)
                    .addOnSuccessListener(v -> System.out.println("✅ LeaderboardManager: Attempts update success: " + path))
                    .addOnFailureListener(e -> System.out.println("❌ LeaderboardManager: Attempts update FAILED: " + path + " error=" + e.getMessage()));
              } else {
                  System.out.println("❌ LeaderboardManager: Document does not exist for attempts update");
              }
          })
          .addOnFailureListener(e -> {
              String path = "Classes/" + classCode + "/Leaderboards/" + lessonName + "_" + activityType + "/Scores/" + studentId;
              System.out.println("❌ LeaderboardManager: Failed to get document for attempts update: " + path + " error=" + e.getMessage());
          });
    }
    
    /**
     * Internal helper to fetch leaderboard scores with optional limit.
     * If limit is null or <= 0, returns all scores ordered by score desc.
     */
    private static void getScoresInternal(String classCode, String lessonName, String activityType,
                                          Integer limit, OnLeaderboardCallback callback) {
        System.out.println("🏆 LeaderboardManager: getScoresInternal() called - Class: " + classCode + ", Lesson: " + lessonName + ", Activity: " + activityType + ", limit=" + limit);

        if (ACTIVITY_COMPILER.equals(activityType)) {
            System.out.println("🏆 LeaderboardManager: Skipping compiler activity");
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String documentPath = "Classes/" + classCode + "/Leaderboards/" + lessonName + "_" + activityType + "/Scores";
        System.out.println("🏆 LeaderboardManager: Querying path: " + documentPath);

        if (classCode == null || lessonName == null || activityType == null) {
            System.out.println("❌ LeaderboardManager: Invalid input parameters for getScoresInternal");
            callback.onSuccess(new ArrayList<>());
            return;
        }

        com.google.firebase.firestore.Query query = getDb().collection("Classes").document(classCode)
                .collection("Leaderboards").document(lessonName + "_" + activityType)
                .collection("Scores")
                .orderBy("score", Query.Direction.DESCENDING);

        if (limit != null && limit > 0) {
            query = query.limit(limit);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    System.out.println("🏆 LeaderboardManager: Query successful - Found " + querySnapshot.size() + " documents");
                    List<LeaderboardEntry> rawEntries = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        System.out.println("🏆 LeaderboardManager: Processing document: " + doc.getId() + " - Score: " + doc.getLong("score") + ", Student: " + doc.getString("studentName"));
                        LeaderboardEntry entry = new LeaderboardEntry(
                                doc.getString("studentId"),
                                doc.getString("studentName"),
                                doc.getLong("score").intValue(),
                                doc.getLong("attemptsUsed").intValue()
                        );
                        rawEntries.add(entry);
                    }

                    resolveStudentNames(rawEntries, new OnLeaderboardCallback() {
                        @Override
                        public void onSuccess(List<LeaderboardEntry> resolvedEntries) {
                            resolvedEntries.sort((a, b) -> {
                                int scoreCompare = Integer.compare(b.score, a.score);
                                if (scoreCompare != 0) return scoreCompare;
                                return Integer.compare(a.attemptsUsed, b.attemptsUsed);
                            });
                            System.out.println("🏆 LeaderboardManager: Returning " + resolvedEntries.size() + " entries with resolved names");
                            callback.onSuccess(resolvedEntries);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            System.out.println("❌ LeaderboardManager: Failed to resolve names: " + e.getMessage());
                            callback.onFailure(e);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ LeaderboardManager: Query failed for " + documentPath + ": " + e.getMessage());
                    callback.onFailure(e);
                });
    }

    /**
     * Get top 10 scores for an activity (student view)
     */
    public static void getTopScores(String classCode, String lessonName, String activityType,
                                    OnLeaderboardCallback callback) {
        getScoresInternal(classCode, lessonName, activityType, 10, callback);
    }

    /**
     * Get all scores for an activity (teacher view)
     */
    public static void getAllScores(String classCode, String lessonName, String activityType,
                                    OnLeaderboardCallback callback) {
        getScoresInternal(classCode, lessonName, activityType, null, callback);
    }
    
    /**
     * Add real-time listener for leaderboard scores
     * Returns ListenerRegistration that must be removed to stop listening
     */
    public static ListenerRegistration addRealtimeScoresListener(String classCode, String lessonName, String activityType,
                                                                 Integer limit, OnLeaderboardCallback callback) {
        System.out.println("🏆 LeaderboardManager: addRealtimeScoresListener() called - Class: " + classCode + ", Lesson: " + lessonName + ", Activity: " + activityType + ", limit=" + limit);

        if (ACTIVITY_COMPILER.equals(activityType)) {
            System.out.println("🏆 LeaderboardManager: Skipping compiler activity");
            callback.onSuccess(new ArrayList<>());
            return null;
        }

        if (classCode == null || lessonName == null || activityType == null) {
            System.out.println("❌ LeaderboardManager: Invalid input parameters for addRealtimeScoresListener");
            callback.onSuccess(new ArrayList<>());
            return null;
        }

        Query query = getDb().collection("Classes").document(classCode)
                .collection("Leaderboards").document(lessonName + "_" + activityType)
                .collection("Scores")
                .orderBy("score", Query.Direction.DESCENDING);

        if (limit != null && limit > 0) {
            query = query.limit(limit);
        }

        return query.addSnapshotListener((querySnapshot, error) -> {
            if (error != null) {
                System.out.println("❌ LeaderboardManager: Real-time listener error: " + error.getMessage());
                callback.onFailure(error);
                return;
            }

            if (querySnapshot == null) {
                System.out.println("🏆 LeaderboardManager: Real-time listener - querySnapshot is null");
                callback.onSuccess(new ArrayList<>());
                return;
            }

            System.out.println("🏆 LeaderboardManager: Real-time update - Found " + querySnapshot.size() + " documents");
            List<LeaderboardEntry> rawEntries = new ArrayList<>();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                if (doc.exists()) {
                    Long scoreLong = doc.getLong("score");
                    Long attemptsLong = doc.getLong("attemptsUsed");
                    
                    if (scoreLong != null && attemptsLong != null) {
                        LeaderboardEntry entry = new LeaderboardEntry(
                                doc.getString("studentId"),
                                doc.getString("studentName"),
                                scoreLong.intValue(),
                                attemptsLong.intValue()
                        );
                        rawEntries.add(entry);
                    }
                }
            }

            resolveStudentNames(rawEntries, new OnLeaderboardCallback() {
                @Override
                public void onSuccess(List<LeaderboardEntry> resolvedEntries) {
                    resolvedEntries.sort((a, b) -> {
                        int scoreCompare = Integer.compare(b.score, a.score);
                        if (scoreCompare != 0) return scoreCompare;
                        return Integer.compare(a.attemptsUsed, b.attemptsUsed);
                    });
                    System.out.println("🏆 LeaderboardManager: Real-time update - Returning " + resolvedEntries.size() + " entries");
                    callback.onSuccess(resolvedEntries);
                }

                @Override
                public void onFailure(Exception e) {
                    System.out.println("❌ LeaderboardManager: Failed to resolve names in real-time: " + e.getMessage());
                    callback.onFailure(e);
                }
            });
        });
    }
    
    /**
     * Resolve student names using signup names from users collection
     */
    private static void resolveStudentNames(List<LeaderboardEntry> entries, OnLeaderboardCallback callback) {
        if (entries.isEmpty()) {
            callback.onSuccess(entries);
            return;
        }
        
        final int totalEntries = entries.size();
        final int[] resolvedCount = {0};
        
        for (LeaderboardEntry entry : entries) {
            if (entry.studentId != null && !entry.studentId.isEmpty()) {
                UserNameManager.getUserName(entry.studentId, new UserNameManager.NameCallback() {
                    @Override
                    public void onSuccess(String name) {
                        entry.studentName = name;
                        resolvedCount[0]++;
                        if (resolvedCount[0] == totalEntries) {
                            callback.onSuccess(entries);
                        }
                    }
                    
                    @Override
                    public void onFailure(String fallbackName) {
                        entry.studentName = fallbackName;
                        resolvedCount[0]++;
                        if (resolvedCount[0] == totalEntries) {
                            callback.onSuccess(entries);
                        }
                    }
                });
            } else {
                // No studentId, keep original name
                resolvedCount[0]++;
                if (resolvedCount[0] == totalEntries) {
                    callback.onSuccess(entries);
                }
            }
        }
    }
    
    /**
     * Get student's current ranking (if in top 10)
     */
    public static void getStudentRanking(String classCode, String lessonName, String activityType,
                                        String studentId, OnRankingCallback callback) {
        // No ranking for compiler activity
        if (ACTIVITY_COMPILER.equals(activityType)) {
            callback.onSuccess(-1);
            return;
        }
        
        getTopScores(classCode, lessonName, activityType, new OnLeaderboardCallback() {
            @Override
            public void onSuccess(List<LeaderboardEntry> entries) {
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).studentId.equals(studentId)) {
                        callback.onSuccess(i + 1); // 1-based ranking
                        return;
                    }
                }
                callback.onSuccess(-1); // Not in top 10
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Check if student can attempt an activity (has attempts remaining)
     */
    public static void checkAttemptsRemaining(String classCode, String lessonName, String activityType,
                                             String studentId, OnAttemptsCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Attempts").document(lessonName + "_" + activityType)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              
              int attemptsUsed = 0;
              if (documentSnapshot.exists()) {
                  attemptsUsed = documentSnapshot.getLong("attemptsUsed").intValue();
              }
              
              boolean canAttempt = attemptsUsed < MAX_ATTEMPTS;
              callback.onSuccess(canAttempt, attemptsUsed, MAX_ATTEMPTS);
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Record an attempt (increment attempt counter)
     */
    public static void recordAttempt(String classCode, String lessonName, String activityType, String studentId) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Attempts").document(lessonName + "_" + activityType)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              
              int currentAttempts = 0;
              if (documentSnapshot.exists()) {
                  currentAttempts = documentSnapshot.getLong("attemptsUsed").intValue();
              }
              
              Map<String, Object> attemptData = new HashMap<>();
              attemptData.put("attemptsUsed", currentAttempts + 1);
              attemptData.put("lastAttempt", System.currentTimeMillis());
              
        String attemptPath = "Classes/" + classCode + "/Students/" + studentId + "/Attempts/" + lessonName + "_" + activityType;
        System.out.println("🎯 LeaderboardManager: Recording attempt → " + attemptPath + " current=" + currentAttempts);
        getDb().collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .collection("Attempts").document(lessonName + "_" + activityType)
          .set(attemptData)
          .addOnSuccessListener(v -> System.out.println("✅ LeaderboardManager: Attempt write success: " + attemptPath))
          .addOnFailureListener(e -> System.out.println("❌ LeaderboardManager: Attempt write FAILED: " + attemptPath + " error=" + e.getMessage()));
          });
    }
    
    /**
     * Teacher: Increase attempts for a student (direct control)
     */
    public static void teacherIncreaseAttempts(String classCode, String lessonName, String activityType,
                                             String studentId, String teacherId, AttemptUpdateCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Attempts").document(lessonName + "_" + activityType)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              
              int currentAttempts = 0;
              if (documentSnapshot.exists()) {
                  Long attemptsLong = documentSnapshot.getLong("attemptsUsed");
                  if (attemptsLong != null) {
                      currentAttempts = attemptsLong.intValue();
                  }
              }
              
              int newAttempts = currentAttempts + 1;
              
              Map<String, Object> attemptData = new HashMap<>();
              attemptData.put("attemptsUsed", newAttempts);
              attemptData.put("lastUpdated", System.currentTimeMillis());
              attemptData.put("updatedBy", "teacher");
              attemptData.put("teacherId", teacherId);
              
              getDb().collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .collection("Attempts").document(lessonName + "_" + activityType)
                .set(attemptData)
                .addOnSuccessListener(v -> {
                    System.out.println("✅ Teacher increased attempts: " + studentId + " " + newAttempts);
                    callback.onSuccess(newAttempts);
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Teacher attempt increase FAILED: " + e.getMessage());
                    callback.onFailure(e);
                });
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Teacher: Decrease attempts for a student (direct control)
     */
    public static void teacherDecreaseAttempts(String classCode, String lessonName, String activityType,
                                             String studentId, String teacherId, AttemptUpdateCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Attempts").document(lessonName + "_" + activityType)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              
              int currentAttempts = 0;
              if (documentSnapshot.exists()) {
                  Long attemptsLong = documentSnapshot.getLong("attemptsUsed");
                  if (attemptsLong != null) {
                      currentAttempts = attemptsLong.intValue();
                  }
              }
              
              int newAttempts = Math.max(0, currentAttempts - 1);
              
              Map<String, Object> attemptData = new HashMap<>();
              attemptData.put("attemptsUsed", newAttempts);
              attemptData.put("lastUpdated", System.currentTimeMillis());
              attemptData.put("updatedBy", "teacher");
              attemptData.put("teacherId", teacherId);
              
              getDb().collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .collection("Attempts").document(lessonName + "_" + activityType)
                .set(attemptData)
                .addOnSuccessListener(v -> {
                    System.out.println("✅ Teacher decreased attempts: " + studentId + " " + newAttempts);
                    callback.onSuccess(newAttempts);
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ Teacher attempt decrease FAILED: " + e.getMessage());
                    callback.onFailure(e);
                });
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Teacher: Reset attempts to 0 for a student
     */
    public static void teacherResetAttempts(String classCode, String lessonName, String activityType,
                                          String studentId, String teacherId, AttemptUpdateCallback callback) {
        
        Map<String, Object> attemptData = new HashMap<>();
        attemptData.put("attemptsUsed", 0);
        attemptData.put("lastUpdated", System.currentTimeMillis());
        attemptData.put("updatedBy", "teacher");
        attemptData.put("teacherId", teacherId);
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Attempts").document(lessonName + "_" + activityType)
          .set(attemptData)
          .addOnSuccessListener(v -> {
              System.out.println("✅ Teacher reset attempts: " + studentId + " to 0");
              callback.onSuccess(0);
          })
          .addOnFailureListener(e -> {
              System.out.println("❌ Teacher attempt reset FAILED: " + e.getMessage());
              callback.onFailure(e);
          });
    }
    
    /**
     * Request additional attempts (for teacher approval) - DEPRECATED, use direct controls instead
     */
    public static void requestAdditionalAttempts(String classCode, String lessonName, String activityType,
                                                String studentId, String studentName, String reason) {
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("studentId", studentId);
        requestData.put("studentName", studentName);
        requestData.put("lessonName", lessonName);
        requestData.put("activityType", activityType);
        requestData.put("reason", reason);
        requestData.put("status", "pending"); // pending, approved, denied
        requestData.put("requestedAt", System.currentTimeMillis());
        
        getDb().collection("Classes").document(classCode)
          .collection("AttemptRequests").document()
          .set(requestData);
    }
    
    // Callback interfaces
    public interface OnLeaderboardCallback {
        void onSuccess(List<LeaderboardEntry> entries);
        void onFailure(Exception e);
    }
    
    public interface OnRankingCallback {
        void onSuccess(int ranking); // -1 if not in top 10
        void onFailure(Exception e);
    }
    
    public interface OnAttemptsCallback {
        void onSuccess(boolean canAttempt, int attemptsUsed, int maxAttempts);
        void onFailure(Exception e);
    }
    
    public interface AttemptUpdateCallback {
        void onSuccess(int newAttemptCount);
        void onFailure(Exception e);
    }
    
    // Data class for leaderboard entries
    public static class LeaderboardEntry {
        public String studentId;
        public String studentName;
        public int score;
        public int attemptsUsed;
        public String classCode;
        public String lessonName;
        public String activityType;
        
        public LeaderboardEntry(String studentId, String studentName, int score, int attemptsUsed) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.score = score;
            this.attemptsUsed = attemptsUsed;
        }
    }
}


