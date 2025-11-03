package com.example.codeclash;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MyClassesActivity extends AppCompatActivity implements ConnectionManager.ConnectionListener, ConnectionStatusDialog.ConnectionDialogListener {

    private RecyclerView recyclerView;
    private View emptyStateContainer;
    private TextView tvNoClasses;
    private List<JoinedClass> classList;
    private JoinedClassAdapter adapter;

    private FirebaseFirestore db;
    private String studentUID;
    private ListenerRegistration classesListener;
    private ConnectionManager connectionManager;
    private ResilientFirebaseHelper resilientHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_classes);

        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerViewClasses);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        tvNoClasses = findViewById(R.id.tvNoClasses);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        classList = new ArrayList<>();
        adapter = new JoinedClassAdapter(classList);
        recyclerView.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        studentUID = currentUser.getUid();
        db = FirebaseFirestore.getInstance();
        
        // Initialize connection monitoring
        connectionManager = ConnectionManager.getInstance(this);
        connectionManager.addConnectionListener(this);
        resilientHelper = new ResilientFirebaseHelper(this);

        loadJoinedClasses();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh student UID in case authentication state changed
        if (adapter != null) {
            adapter.refreshStudentUID();
        }
        // Check for notifications
        checkForNotifications();
        // Start real-time listener for classes
        startClassesListener();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop real-time listener to save resources
        stopClassesListener();
    }
    
    private void checkForNotifications() {
        db.collection("Users")
            .document(studentUID)
            .collection("Notifications")
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot.getDocuments().isEmpty()) {
                    System.out.println("🔔 No unread notifications found");
                    return;
                }
                
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String type = doc.getString("type");
                    String title = doc.getString("title");
                    String body = doc.getString("body");
                    String classCode = doc.getString("classCode");
                    String className = doc.getString("className");
                    
                    System.out.println("🔔 Found unread notification: type=" + type + ", title=" + title);
                    
                    // Show Toast as fallback when notifications are blocked
                    String message = body != null ? body : title;
                    if (message != null) {
                        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
                        System.out.println("🔔 Showed Toast notification: " + message);
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
                System.out.println("🔔 Failed to check notifications: " + e.getMessage());
            });
    }

    private void loadJoinedClasses() {
        db.collection("Users")
                .document(studentUID)
                .collection("MyJoinedClasses")
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    classList.clear();
                    for (DocumentSnapshot doc : querySnapshots) {
                        JoinedClass joinedClass = doc.toObject(JoinedClass.class);
                        classList.add(joinedClass);
                    }

                    if (classList.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateContainer.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvNoClasses.setText("Failed to load classes");
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Refresh the classes list (called after leaving a class)
     */
    public void refreshClasses() {
        System.out.println("🔄 MyClassesActivity: Refreshing classes list");
        loadJoinedClasses();
    }
    
    private void startClassesListener() {
        if (classesListener != null) {
            classesListener.remove();
        }
        
        classesListener = db.collection("Users").document(studentUID)
            .collection("MyJoinedClasses")
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    System.out.println("❌ Classes listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null) {
                    classList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        JoinedClass joinedClass = doc.toObject(JoinedClass.class);
                        if (joinedClass != null) {
                            classList.add(joinedClass);
                        }
                    }
                    
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    
                    // Update empty state
                    if (classList.isEmpty()) {
                        emptyStateContainer.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateContainer.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    System.out.println("🔄 Classes list auto-reloaded: " + classList.size() + " classes");
                }
            });
    }
    
    private void stopClassesListener() {
        if (classesListener != null) {
            classesListener.remove();
            classesListener = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClassesListener();
        if (connectionManager != null) {
            connectionManager.removeConnectionListener(this);
        }
    }
    
    // ConnectionManager.ConnectionListener implementation
    @Override
    public void onConnectionChanged(boolean isConnected) {
        // Connection status changed
        System.out.println("🔗 MyClassesActivity: Connection changed - " + (isConnected ? "Connected" : "Disconnected"));
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
        // User chose to retry - refresh the classes list
        loadJoinedClasses();
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
