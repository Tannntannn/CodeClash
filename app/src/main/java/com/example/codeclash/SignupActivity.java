package com.example.codeclash;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText signupName, signupEmail, signupPassword;
    private Button signupButton;
    private TextView signupRedirectText;
    private CheckBox privacyCheckbox;
    private TextView privacyPolicyLink, termsLink;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final int TIMEOUT_DURATION = 10000; // 10 seconds timeout

    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_USER_ROLE = "user_role";
    private boolean isCreatingAccount = false;
    private android.widget.CheckBox signupShowPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();
        setClickListeners();
    }

    private void initializeViews() {
        signupName = findViewById(R.id.signup_name);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupButton = findViewById(R.id.signup_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        privacyCheckbox = findViewById(R.id.privacy_checkbox);
        privacyPolicyLink = findViewById(R.id.privacy_policy_link);
        termsLink = findViewById(R.id.terms_link);
        signupShowPassword = findViewById(R.id.signup_show_password);

        if (signupShowPassword != null) {
            signupShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int cursor = signupPassword.getSelectionStart();
                if (isChecked) {
                    signupPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    signupPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                signupPassword.setSelection(Math.max(0, cursor));
            });
        }
    }

    private void setClickListeners() {
        signupButton.setOnClickListener(v -> createAccount());

        signupRedirectText.setOnClickListener(v -> {
            if (isCreatingAccount) {
                Toast.makeText(SignupActivity.this, "Please wait, account creation in progress...", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });

        privacyPolicyLink.setOnClickListener(v -> showPrivacyPolicy());
        termsLink.setOnClickListener(v -> showTermsOfService());
    }

    private void createAccount() {
        if (isCreatingAccount) {
            Toast.makeText(this, "Account creation in progress, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check internet connectivity first
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        String name = signupName.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();

        if (!validateInput(name, email, password)) {
            return;
        }

        setLoadingState(true);

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send verification email first
                            user.sendEmailVerification().addOnCompleteListener(sendTask -> {
                                // Proceed to save minimal user data regardless of email send success
                                saveUserToFirestore(user.getUid(), name, email);
                            });
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        Toast.makeText(SignupActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        setLoadingState(false);
                    }
                });
    }

    private boolean validateInput(String name, String email, String password) {
        clearErrors();
        boolean isValid = true;

        if (TextUtils.isEmpty(name) || name.length() < 2) {
            signupName.setError(TextUtils.isEmpty(name) ? "Name is required" : "Name must be at least 2 characters");
            if (isValid) signupName.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            signupEmail.setError("Email is required");
            if (isValid) signupEmail.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Please enter a valid email address");
            if (isValid) signupEmail.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            signupPassword.setError("Password is required");
            if (isValid) signupPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            signupPassword.setError("Password must be at least 6 characters");
            if (isValid) signupPassword.requestFocus();
            isValid = false;
        }

        if (!privacyCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept the Privacy Policy and Terms of Service", Toast.LENGTH_LONG).show();
            privacyCheckbox.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        signupName.setError(null);
        signupEmail.setError(null);
        signupPassword.setError(null);
    }

    private void saveUserToFirestore(String userId, String name, String email) {
        signupButton.setText("Saving Account...");

        // Create minimal user data - only essential information
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("userId", userId);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("role", ""); // Will be set in MainActivity

        // Create a timeout handler
        android.os.Handler timeoutHandler = new android.os.Handler();
        boolean[] isCompleted = {false};

        Runnable timeoutRunnable = () -> {
            if (!isCompleted[0]) {
                isCompleted[0] = true;
                // If Firestore is taking too long, proceed anyway since auth user is already created
                Toast.makeText(SignupActivity.this, "Account created! (Data will sync in background)", Toast.LENGTH_SHORT).show();

                // Save data in background
                saveUserDataInBackground(userId, userData);

                // Navigate to MainActivity
                clearPreviousRoleSelection();
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        };

        // Start timeout
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);

        // Save to Firestore users collection
        db.collection("users").document(userId).set(userData)
                .addOnCompleteListener(task -> {
                    if (!isCompleted[0]) {
                        isCompleted[0] = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);

                        if (task.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                            // Clear any previous role selection
                            clearPreviousRoleSelection();

                            // Navigate to VerifyEmailActivity
                            Intent intent = new Intent(SignupActivity.this, VerifyEmailActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMessage = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Failed to save user data";
                            Toast.makeText(SignupActivity.this, "Failed to save user data: " + errorMessage, Toast.LENGTH_LONG).show();

                            // Clean up - delete the created auth user
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.delete().addOnCompleteListener(deleteTask -> {
                                    if (!deleteTask.isSuccessful()) {
                                        System.err.println("Failed to delete user after Firestore save failure");
                                    }
                                });
                            }
                            setLoadingState(false);
                        }
                    }
                });
    }

    private void saveUserDataInBackground(String userId, Map<String, Object> userData) {
        // Retry saving user data in background
        db.collection("users").document(userId).set(userData)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        System.err.println("Background save failed: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void clearPreviousRoleSelection() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ROLE);
        editor.apply();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void setLoadingState(boolean isLoading) {
        isCreatingAccount = isLoading;

        // Disable/enable all components
        signupName.setEnabled(!isLoading);
        signupEmail.setEnabled(!isLoading);
        signupPassword.setEnabled(!isLoading);
        signupButton.setEnabled(!isLoading);
        signupRedirectText.setEnabled(!isLoading);
        privacyCheckbox.setEnabled(!isLoading);
        privacyPolicyLink.setEnabled(!isLoading);
        termsLink.setEnabled(!isLoading);

        // Update button appearance
        if (isLoading) {
            signupButton.setText("Creating Account...");
            signupButton.setAlpha(0.6f);
            signupRedirectText.setAlpha(0.6f);
        } else {
            signupButton.setText("Sign Up");
            signupButton.setAlpha(1.0f);
            signupRedirectText.setAlpha(1.0f);
        }
    }

    private void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy")
                .setMessage("CodeClash Privacy Policy\n\n" +
                        "1. Information We Collect:\n" +
                        "• Name and email address for account creation\n" +
                        "• Class participation and progress data\n" +
                        "• Usage analytics to improve the app\n\n" +
                        "2. How We Use Your Information:\n" +
                        "• To provide educational services\n" +
                        "• To track learning progress\n" +
                        "• To communicate with teachers and students\n\n" +
                        "3. Data Security:\n" +
                        "• We use Firebase for secure data storage\n" +
                        "• All data is encrypted in transit\n" +
                        "• We never share personal information with third parties\n\n" +
                        "4. Your Rights:\n" +
                        "• You can request deletion of your data\n" +
                        "• You can opt out of analytics\n" +
                        "• Contact us for any privacy concerns\n\n" +
                        "Last updated: " + java.time.LocalDate.now())
                .setPositiveButton("I Understand", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void showTermsOfService() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms of Service")
                .setMessage("CodeClash Terms of Service\n\n" +
                        "1. Acceptance of Terms:\n" +
                        "By using CodeClash, you agree to these terms.\n\n" +
                        "2. Educational Use:\n" +
                        "• This app is designed for educational purposes\n" +
                        "• Users must be students or teachers\n" +
                        "• Appropriate behavior is required\n\n" +
                        "3. Code of Conduct:\n" +
                        "• Be respectful to other users\n" +
                        "• Do not share inappropriate content\n" +
                        "• Follow teacher instructions\n\n" +
                        "4. Intellectual Property:\n" +
                        "• CodeClash retains rights to the app\n" +
                        "• Users retain rights to their code\n" +
                        "• Educational content is provided as-is\n\n" +
                        "5. Limitation of Liability:\n" +
                        "• We provide the service as-is\n" +
                        "• We're not liable for any damages\n" +
                        "• Use at your own risk\n\n" +
                        "6. Termination:\n" +
                        "• We can terminate accounts for violations\n" +
                        "• Users can delete their accounts anytime\n\n" +
                        "Last updated: " + java.time.LocalDate.now())
                .setPositiveButton("I Agree", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                clearPreviousRoleSelection();
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(SignupActivity.this, VerifyEmailActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (isCreatingAccount) {
            Toast.makeText(this, "Please wait, account creation in progress...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
        finishAffinity();
    }
}