package com.example.codeclash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton studentButton;
    private ImageButton teacherButton;
    private ProgressBar loadingProgressBar;
    private TextView loadingText;  // Add this field
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_ROLE = "user_role";
    private boolean isRoleBeingSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Background music disabled

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Enable offline persistence for better network handling
        NetworkManager.enableOfflinePersistence();
        
        // Initialize connection manager for poor connection handling
        NetworkManager.initializeConnectionManager(this);
        
        // Start notification service for real-time notifications
        startNotificationService();
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this);
        
        // Get FCM token for push notifications
        getFCMToken();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Initialize views
        initViews();

        // Check if user is authenticated
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not authenticated, redirect to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Load user role from Firestore
        loadUserRoleFromFirestore(currentUser.getUid());
    }

    private void initViews() {
        studentButton = findViewById(R.id.studentButton);
        teacherButton = findViewById(R.id.teacherButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingText = findViewById(R.id.loadingText);  // Initialize the TextView

        // Initially hide the buttons and show loading
        showLoading(true);
    }

    private void showLoading(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (loadingText != null) {
            loadingText.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        studentButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        teacherButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);

        // Disable buttons during loading
        studentButton.setEnabled(!isLoading);
        teacherButton.setEnabled(!isLoading);
    }

    private void loadUserRoleFromFirestore(String userId) {
        showLoading(true);

        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");

                            if (role != null && !role.isEmpty()) {
                                // User has a role set, save to local storage and navigate
                                saveRoleLocally(role);
                                Toast.makeText(this, "Welcome back, " + role + "!", Toast.LENGTH_SHORT).show();
                                navigateToRoleActivity(role);
                                return;
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error loading user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    // No role found or error occurred, show role selection
                    setupRoleSelection();
                });
    }

    private void setupRoleSelection() {
        // Set up click listeners for role selection
        studentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRoleBeingSet) {
                    showRoleConfirmationDialog("student");
                }
            }
        });

        teacherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRoleBeingSet) {
                    showRoleConfirmationDialog("teacher");
                }
            }
        });
    }

    private void showRoleConfirmationDialog(String role) {
        String roleDisplay = role.substring(0, 1).toUpperCase() + role.substring(1);
        new AlertDialog.Builder(this)
            .setTitle("Confirm Role")
            .setMessage("Are you sure you want to continue as a " + roleDisplay + "? This will be your role for future logins.")
            .setPositiveButton("Yes", (dialog, which) -> selectRole(role))
            .setNegativeButton("No", null)
            .show();
    }

    private void selectRole(String role) {
        if (isRoleBeingSet) {
            Toast.makeText(this, "Please wait, setting your role...", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        isRoleBeingSet = true;
        showLoading(true);

        // Save role to Firestore
        saveRoleToFirestore(currentUser.getUid(), role);
    }

    private void saveRoleToFirestore(String userId, String role) {
        // Update the user document with the selected role
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role);
        updates.put("roleSetAt", System.currentTimeMillis());

        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(task -> {
                    isRoleBeingSet = false;
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Save to local storage as backup
                        saveRoleLocally(role);

                        // Show confirmation toast
                        String message = "Role set as: " + role.substring(0, 1).toUpperCase() + role.substring(1) +
                                "\nThis will be your role for future logins.";
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

                        // Navigate to appropriate activity
                        navigateToRoleActivity(role);
                    } else {
                        Toast.makeText(this, "Failed to save role: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                Toast.LENGTH_LONG).show();

                        // Re-enable buttons for retry
                        setupRoleSelection();
                    }
                });
    }

    private void saveRoleLocally(String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    private void navigateToRoleActivity(String role) {
        Intent intent;

        if (role.equals("student")) {
            intent = new Intent(this, StudentActivity.class);
        } else if (role.equals("teacher")) {
            intent = new Intent(this, TeacherActivity.class);
        } else {
            Toast.makeText(this, "Invalid role selected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass role as extra data
        intent.putExtra("user_role", role);
        startActivity(intent);
        finish();
    }

    // Method to get saved user role from local storage
    public String getSavedUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "");
    }

    // Method to clear user role from both local and Firestore
    public void clearUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Clear from Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("role", "");

            db.collection("users").document(currentUser.getUid()).update(updates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Role cleared successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Clear from local storage
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ROLE);
        editor.apply();
    }

    // Static method to clear role from other activities
    public static void clearUserRoleStatic(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_ROLE);
        editor.apply();

        // Also clear from Firestore
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            Map<String, Object> updates = new HashMap<>();
            updates.put("role", "");

            firestore.collection("users").document(currentUser.getUid()).update(updates);
        }
    }

    // Method to check if user has a role set locally
    public boolean hasRoleSet() {
        return !getSavedUserRole().isEmpty();
    }

    // Method to reset user role (with confirmation)
    public void resetUserRole() {
        // You can add a confirmation dialog here
        clearUserRole();

        // Restart the activity to show role selection again
        recreate();
    }

    // Method to sync role from Firestore to local storage
    public void syncRoleFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadUserRoleFromFirestore(currentUser.getUid());
        }
    }

    private void getFCMToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    System.out.println("FCM: Failed to get token: " + task.getException());
                    return;
                }
                
                String token = task.getResult();
                System.out.println("FCM: Token received: " + token);
                saveFCMToken(token);
            });
    }
    
    private void saveFCMToken(String token) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("Users").document(currentUser.getUid())
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> System.out.println("FCM: Token saved successfully"))
                .addOnFailureListener(e -> System.out.println("FCM: Failed to save token: " + e.getMessage()));
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (isRoleBeingSet) {
            Toast.makeText(this, "Please wait, setting your role...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
    
    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationListenerService.class);
        startService(serviceIntent);
    }
}