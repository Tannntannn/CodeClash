package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class StudentActivity extends AppCompatActivity implements ConnectionManager.ConnectionListener, ConnectionStatusDialog.ConnectionDialogListener {

    private ConnectionManager connectionManager;
    private ResilientFirebaseHelper resilientHelper;
    private FirebaseFirestore db;
    private String studentUID;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student);
        
        // Initialize connection monitoring
        connectionManager = ConnectionManager.getInstance(this);
        connectionManager.addConnectionListener(this);
        resilientHelper = new ResilientFirebaseHelper(this);
        
        // Initialize Firestore and get student UID
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Initialize buttons
        ImageButton joinClassButton = findViewById(R.id.joinClassButton);
        ImageButton settingsButton = findViewById(R.id.settingsButton);
        ImageButton myClassesButton = findViewById(R.id.myClassesButton); // ✅ Added this
        ImageButton leaderboardsButton = findViewById(R.id.leaboardsButton);

        // Set click listener for JOIN CLASS button
        joinClassButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, StudentClasscode.class);
            startActivity(intent);
        });

        // Set click listener for SETTINGS button
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, StudentSettings.class);
            startActivity(intent);
        });

        // ✅ Set click listener for MY CLASSES button
        myClassesButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, MyClassesActivity.class);
            startActivity(intent);
        });

        leaderboardsButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudentActivity.this, StudentLeaderboardsActivity.class);
            // If you have current class/lesson context, attach here
            // intent.putExtra("classCode", currentClassCode);
            // intent.putExtra("lessonName", currentLessonName);
            startActivity(intent);
        });

        // Apply window insets for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.centerContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check connection status when activity resumes
        if (connectionManager != null && !connectionManager.isConnected()) {
            resilientHelper.showConnectionLostDialog();
        }
        // Check for notifications automatically
        checkForNotifications();
        // Start real-time notification listener
        startNotificationListener();
    }
    
    private void startNotificationListener() {
        if (studentUID == null) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                studentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                return;
            }
        }
        
        // Remove old listener if exists
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        // Listen for new notifications in real-time
        notificationListener = db.collection("Users")
            .document(studentUID)
            .collection("Notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    System.out.println("🔔 StudentActivity: Notification listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String title = doc.getString("title");
                        String body = doc.getString("body");
                        String classCode = doc.getString("classCode");
                        String className = doc.getString("className");
                        
                        System.out.println("🔔 StudentActivity: Real-time notification received: type=" + type + ", title=" + title);
                        
                        // Show Toast notification
                        String message = body != null ? body : title;
                        if (message != null) {
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        }
                        
                        // Show local notification based on type
                        switch (type) {
                            case "kicked":
                                NotificationHelper.showStudentKickedNotification(this, classCode, className, "Student");
                                break;
                            case "join_approved":
                                NotificationHelper.showJoinApprovedNotification(this, classCode);
                                break;
                            case "join_rejected":
                                NotificationHelper.showJoinRejectedNotification(this, classCode);
                                break;
                            case "leave_approved":
                                if (className != null) {
                                    NotificationHelper.showLeaveApprovedNotification(this, classCode, className);
                                } else {
                                    NotificationHelper.showLeaveApprovedNotification(this, classCode, null);
                                }
                                break;
                            case "leave_rejected":
                                NotificationHelper.showLeaveRejectedNotification(this, classCode);
                                break;
                            default:
                                // Generic notification
                                if (title != null && body != null) {
                                    NotificationHelper.showGenericNotification(this, title, body);
                                }
                        }
                        
                        // Mark as read
                        doc.getReference().update("read", true);
                    }
                }
            });
        
        System.out.println("🔔 StudentActivity: Started real-time notification listener");
    }
    
    private void stopNotificationListener() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
            System.out.println("🔔 StudentActivity: Stopped real-time notification listener");
        }
    }
    
    private void checkForNotifications() {
        if (studentUID == null) {
            // Refresh student UID in case authentication state changed
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                studentUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            } else {
                return;
            }
        }
        
        db.collection("Users")
            .document(studentUID)
            .collection("Notifications")
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.getDocuments().isEmpty()) {
                    System.out.println("🔔 StudentActivity: No unread notifications found");
                    return;
                }
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String type = doc.getString("type");
                    String title = doc.getString("title");
                    String body = doc.getString("body");
                    String classCode = doc.getString("classCode");
                    String className = doc.getString("className");
                    
                    System.out.println("🔔 StudentActivity: Found unread notification: type=" + type + ", title=" + title);
                    
                    // Show Toast as fallback when notifications are blocked
                    String message = body != null ? body : title;
                    if (message != null) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        System.out.println("🔔 StudentActivity: Showed Toast notification: " + message);
                    }
                    
                    // Show local notification based on type
                    switch (type) {
                        case "kicked":
                            NotificationHelper.showStudentKickedNotification(this, classCode, className, "Student");
                            break;
                        case "join_approved":
                            NotificationHelper.showJoinApprovedNotification(this, classCode);
                            break;
                        case "join_rejected":
                            NotificationHelper.showJoinRejectedNotification(this, classCode);
                            break;
                        case "leave_approved":
                            if (className != null) {
                                NotificationHelper.showLeaveApprovedNotification(this, classCode, className);
                            } else {
                                NotificationHelper.showLeaveApprovedNotification(this, classCode, null);
                            }
                            break;
                        case "leave_rejected":
                            NotificationHelper.showLeaveRejectedNotification(this, classCode);
                            break;
                        default:
                            // Generic notification
                            if (title != null && body != null) {
                                NotificationHelper.showGenericNotification(this, title, body);
                            }
                    }
                    
                    // Mark as read
                    doc.getReference().update("read", true);
                }
            })
            .addOnFailureListener(e -> {
                System.out.println("🔔 StudentActivity: Failed to check notifications: " + e.getMessage());
            });
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop notification listener to save resources
        stopNotificationListener();
        // Connection monitoring continues in background
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionManager != null) {
            connectionManager.removeConnectionListener(this);
        }
        // Clean up notification listener
        stopNotificationListener();
    }
    
    // ConnectionManager.ConnectionListener implementation
    @Override
    public void onConnectionChanged(boolean isConnected) {
        // Connection status changed
        System.out.println("🔗 StudentActivity: Connection changed - " + (isConnected ? "Connected" : "Disconnected"));
    }
    
    @Override
    public void onConnectionRestored() {
        // Connection was restored - only show dialog if activity is in valid state
        if (!isFinishing() && !isDestroyed()) {
            resilientHelper.showConnectionRestoredDialog();
        }
    }
    
    @Override
    public void onConnectionLost() {
        // Connection was lost - only show dialog if activity is in valid state
        if (!isFinishing() && !isDestroyed()) {
            resilientHelper.showConnectionLostDialog();
        }
    }
    
    // ConnectionStatusDialog.ConnectionDialogListener implementation
    @Override
    public void onRetry() {
        // User chose to retry - refresh the activity
        recreate();
    }
    
    @Override
    public void onCancel() {
        // User chose to cancel - do nothing
    }
    
    @Override
    public void onGoOffline() {
        // User chose to work offline - continue with cached data
        // The app will continue working with Firebase offline persistence
    }
}
