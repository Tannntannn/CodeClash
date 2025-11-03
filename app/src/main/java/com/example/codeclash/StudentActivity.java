package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StudentActivity extends AppCompatActivity implements ConnectionManager.ConnectionListener, ConnectionStatusDialog.ConnectionDialogListener {

    private ConnectionManager connectionManager;
    private ResilientFirebaseHelper resilientHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student);
        
        // Initialize connection monitoring
        connectionManager = ConnectionManager.getInstance(this);
        connectionManager.addConnectionListener(this);
        resilientHelper = new ResilientFirebaseHelper(this);

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
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Connection monitoring continues in background
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionManager != null) {
            connectionManager.removeConnectionListener(this);
        }
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
