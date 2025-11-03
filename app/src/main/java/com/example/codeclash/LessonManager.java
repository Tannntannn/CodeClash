package com.example.codeclash;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class LessonManager {
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
    
    // Lesson status constants
    public static final String STATUS_LOCKED = "locked";
    public static final String STATUS_UNLOCKED = "unlocked";
    public static final String STATUS_COMPLETED = "completed";
    
    // Activity completion status
    public static final String ACTIVITY_NOT_STARTED = "not_started";
    public static final String ACTIVITY_IN_PROGRESS = "in_progress";
    public static final String ACTIVITY_COMPLETED = "completed";
    public static final String ACTIVITY_LOCKED = "locked";
    
    /**
     * Lock/unlock a lesson (teacher only)
     */
    public static void setLessonStatus(String classCode, String lessonName, String status) {
        Map<String, Object> lessonData = new HashMap<>();
        lessonData.put("status", status);
        lessonData.put("updatedAt", System.currentTimeMillis());
        
        getDb().collection("Classes").document(classCode)
          .collection("Lessons").document(lessonName)
          .set(lessonData);
    }
    
    /**
     * Get lesson status for a student
     */
    public static void getLessonStatus(String classCode, String lessonName, String studentId, 
                                     OnLessonStatusCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Lessons").document(lessonName)
          .get()
          .addOnSuccessListener(lessonDoc -> {
              
              String lessonStatus = STATUS_LOCKED; // Default locked
              if (lessonDoc.exists()) {
                  lessonStatus = lessonDoc.getString("status");
                  if (lessonStatus == null) lessonStatus = STATUS_LOCKED;
              }
              
              // Check if student has completed this lesson
              checkStudentProgress(classCode, lessonName, studentId, lessonStatus, callback);
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    private static void checkStudentProgress(String classCode, String lessonName, String studentId, 
                                           String lessonStatus, OnLessonStatusCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Progress").document(lessonName)
          .get()
          .addOnSuccessListener(progressDoc -> {
              
              Map<String, String> activityStatus = new HashMap<>();
              activityStatus.put("quiz", ACTIVITY_NOT_STARTED);
              activityStatus.put("code_builder", ACTIVITY_NOT_STARTED);
              activityStatus.put("compiler", ACTIVITY_NOT_STARTED);
              
              if (progressDoc.exists()) {
                  Map<String, Object> data = progressDoc.getData();
                  if (data != null) {
                      activityStatus.put("quiz", (String) data.getOrDefault("quiz", ACTIVITY_NOT_STARTED));
                      activityStatus.put("code_builder", (String) data.getOrDefault("code_builder", ACTIVITY_NOT_STARTED));
                      activityStatus.put("compiler", (String) data.getOrDefault("compiler", ACTIVITY_NOT_STARTED));
                  }
              }
              
              boolean isCompleted = activityStatus.get("quiz").equals(ACTIVITY_COMPLETED) &&
                                   activityStatus.get("code_builder").equals(ACTIVITY_COMPLETED) &&
                                   activityStatus.get("compiler").equals(ACTIVITY_COMPLETED);
              
              String finalStatus = lessonStatus;
              if (isCompleted) {
                  finalStatus = STATUS_COMPLETED;
              } else if (lessonStatus.equals(STATUS_LOCKED)) {
                  finalStatus = STATUS_LOCKED;
              }
              
              callback.onSuccess(finalStatus, activityStatus, isCompleted);
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Mark an activity as completed for a student
     */
    public static void markActivityCompleted(String classCode, String lessonName, String activityType, 
                                           String studentId, int score, int attemptsUsed) {
        
        System.out.println("📊 LessonManager: markActivityCompleted() called");
        System.out.println("📊 LessonManager: Class: " + classCode + ", Lesson: " + lessonName + ", Activity: " + activityType);
        System.out.println("📊 LessonManager: Student: " + studentId + ", Score: " + score + ", Attempts: " + attemptsUsed);
        
        // Record the score in leaderboard
        System.out.println("📊 LessonManager: Getting student info from Firebase...");
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .get()
          .addOnSuccessListener(studentDoc -> {
              System.out.println("📊 LessonManager: Student document retrieved successfully");
              
              String studentName = "Unknown Student";
              if (studentDoc.exists()) {
                  studentName = studentDoc.getString("fullName");
                  if (studentName == null) studentName = "Unknown Student";
                  System.out.println("📊 LessonManager: Student name: " + studentName);
              } else {
                  System.out.println("❌ LessonManager: Student document does not exist");
              }
              
              // Record score in leaderboard
              System.out.println("📊 LessonManager: Calling LeaderboardManager.recordScore()");
              LeaderboardManager.recordScore(classCode, lessonName, activityType, 
                                           studentId, studentName, score, attemptsUsed);
              
              // Update student progress
              updateStudentProgress(classCode, lessonName, activityType, studentId, ACTIVITY_COMPLETED);
          });
    }
    
    /**
     * Update student progress for an activity
     */
    public static void updateStudentProgress(String classCode, String lessonName, String activityType, 
                                           String studentId, String status) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Progress").document(lessonName)
          .get()
          .addOnSuccessListener(progressDoc -> {
              
              Map<String, Object> progressData = new HashMap<>();
              progressData.put("lessonName", lessonName);
              progressData.put("lastUpdated", System.currentTimeMillis());
              
              if (progressDoc.exists()) {
                  Map<String, Object> existingData = progressDoc.getData();
                  if (existingData != null) {
                      progressData.putAll(existingData);
                  }
              }
              
              progressData.put(activityType, status);
              
              getDb().collection("Classes").document(classCode)
                .collection("Students").document(studentId)
                .collection("Progress").document(lessonName)
                .set(progressData);
          });
    }
    
    /**
     * Get all pending attempt requests for a teacher
     */
    public static void getPendingAttemptRequests(String classCode, OnAttemptRequestsCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("AttemptRequests")
          .whereEqualTo("status", "pending")
          .get()
          .addOnSuccessListener(querySnapshot -> {
              
              callback.onSuccess(querySnapshot);
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Approve or deny an attempt request (teacher only)
     */
    public static void respondToAttemptRequest(String classCode, String requestId, String response, 
                                             String teacherNote) {
        
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("status", response); // "approved" or "denied"
        updateData.put("teacherNote", teacherNote);
        updateData.put("respondedAt", System.currentTimeMillis());
        
        getDb().collection("Classes").document(classCode)
          .collection("AttemptRequests").document(requestId)
          .update(updateData);
    }
    
    /**
     * Get student's overall progress for a class
     */
    public static void getStudentClassProgress(String classCode, String studentId, 
                                             OnClassProgressCallback callback) {
        
        getDb().collection("Classes").document(classCode)
          .collection("Students").document(studentId)
          .collection("Progress")
          .get()
          .addOnSuccessListener(querySnapshot -> {
              
              Map<String, Map<String, String>> lessonProgress = new HashMap<>();
              int totalLessons = 0;
              int completedLessons = 0;
              
              for (DocumentSnapshot doc : querySnapshot) {
                  String lessonName = doc.getId();
                  Map<String, Object> data = doc.getData();
                  
                  if (data != null) {
                      Map<String, String> activityStatus = new HashMap<>();
                      activityStatus.put("quiz", (String) data.getOrDefault("quiz", ACTIVITY_NOT_STARTED));
                      activityStatus.put("code_builder", (String) data.getOrDefault("code_builder", ACTIVITY_NOT_STARTED));
                      activityStatus.put("compiler", (String) data.getOrDefault("compiler", ACTIVITY_NOT_STARTED));
                      
                      lessonProgress.put(lessonName, activityStatus);
                      
                      totalLessons++;
                      if (activityStatus.get("quiz").equals(ACTIVITY_COMPLETED) &&
                          activityStatus.get("code_builder").equals(ACTIVITY_COMPLETED) &&
                          activityStatus.get("compiler").equals(ACTIVITY_COMPLETED)) {
                          completedLessons++;
                      }
                  }
              }
              
              callback.onSuccess(lessonProgress, completedLessons, totalLessons);
          })
          .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Check if student can proceed to next lesson
     */
    public static void canProceedToNextLesson(String classCode, String currentLesson, String studentId,
                                            OnProceedCallback callback) {
        
        // Check if current lesson is completed
        getLessonStatus(classCode, currentLesson, studentId, new OnLessonStatusCallback() {
            @Override
            public void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted) {
                
                if (isCompleted) {
                    // Current lesson completed, check if next lesson is unlocked
                    getNextLessonName(classCode, currentLesson, nextLessonName -> {
                        if (nextLessonName != null) {
                            getLessonStatus(classCode, nextLessonName, studentId, new OnLessonStatusCallback() {
                                @Override
                                public void onSuccess(String nextLessonStatus, Map<String, String> nextActivityStatus, boolean nextIsCompleted) {
                                    boolean canProceed = !nextLessonStatus.equals(STATUS_LOCKED);
                                    callback.onSuccess(canProceed, nextLessonName, nextLessonStatus);
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    callback.onFailure(e);
                                }
                            });
                        } else {
                            // No next lesson (completed all lessons)
                            callback.onSuccess(false, null, null);
                        }
                    });
                } else {
                    // Current lesson not completed
                    callback.onSuccess(false, null, null);
                }
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    private static void getNextLessonName(String classCode, String currentLesson, OnNextLessonCallback callback) {
        // This would need to be implemented based on your lesson ordering
        // For now, return null (no next lesson)
        callback.onSuccess(null);
    }
    
    // Callback interfaces
    public interface OnLessonStatusCallback {
        void onSuccess(String lessonStatus, Map<String, String> activityStatus, boolean isCompleted);
        void onFailure(Exception e);
    }
    
    public interface OnAttemptRequestsCallback {
        void onSuccess(QuerySnapshot requests);
        void onFailure(Exception e);
    }
    
    public interface OnClassProgressCallback {
        void onSuccess(Map<String, Map<String, String>> lessonProgress, int completedLessons, int totalLessons);
        void onFailure(Exception e);
    }
    
    public interface OnProceedCallback {
        void onSuccess(boolean canProceed, String nextLessonName, String nextLessonStatus);
        void onFailure(Exception e);
    }
    
    public interface OnNextLessonCallback {
        void onSuccess(String nextLessonName);
    }
    
    /**
     * Initialize lessons for a class with default locked status
     */
    public static void initializeLessons(String classCode, String[] lessonNames) {
        for (String lessonName : lessonNames) {
            Map<String, Object> lessonData = new HashMap<>();
            lessonData.put("status", STATUS_LOCKED);
            lessonData.put("createdAt", System.currentTimeMillis());
            
            getDb().collection("Classes").document(classCode)
              .collection("Lessons").document(lessonName)
              .set(lessonData);
        }
    }
}


