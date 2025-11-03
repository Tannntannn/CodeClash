package com.example.codeclash;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText loginUsername, loginPassword;
    private Button loginButton;
    private TextView loginRedirectText, forgotPasswordText;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private android.widget.CheckBox loginShowPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        loginShowPassword = findViewById(R.id.login_show_password);

        if (loginShowPassword != null) {
            loginShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int cursor = loginPassword.getSelectionStart();
                if (isChecked) {
                    loginPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    loginPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                loginPassword.setSelection(Math.max(0, cursor));
            });
        }
    }

    private void setClickListeners() {
        // Login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Redirect to signup click listener
        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Sign Up Activity
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Forgot password click listener
        forgotPasswordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void loginUser() {
        String username = loginUsername.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        // Check network connectivity first
        if (!NetworkManager.isNetworkAvailable(this)) {
            NetworkManager.showNetworkError(this);
            return;
        }

        // Validate input fields
        if (!validateInput(username, password)) {
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Check if input is email or username
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            // Login with email directly
            loginWithEmail(username, password);
        } else {
            // Find email by username first, then login
            findEmailByUsername(username, password);
        }
    }

    private boolean validateInput(String username, String password) {
        // Reset any previous error states
        loginUsername.setError(null);
        loginPassword.setError(null);

        boolean isValid = true;

        // Validate username/email
        if (TextUtils.isEmpty(username)) {
            loginUsername.setError("Username or email is required");
            loginUsername.requestFocus();
            isValid = false;
        } else if (username.length() < 3) {
            loginUsername.setError("Username must be at least 3 characters");
            loginUsername.requestFocus();
            isValid = false;
        } else if (username.contains(" ")) {
            loginUsername.setError("Username cannot contain spaces");
            loginUsername.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches() &&
                !username.matches("^[a-zA-Z0-9_]+$")) {
            loginUsername.setError("Enter a valid email or username (letters, numbers, underscore only)");
            loginUsername.requestFocus();
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Password is required");
            if (isValid) loginPassword.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            loginPassword.setError("Password must be at least 6 characters");
            if (isValid) loginPassword.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void findEmailByUsername(String username, String password) {
        // Add timeout handler to prevent indefinite loading
        android.os.Handler timeoutHandler = new android.os.Handler();
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, "Login timeout. Please try again.",
                        Toast.LENGTH_LONG).show();
                resetButton();
            }
        };

        // Set 15 second timeout
        timeoutHandler.postDelayed(timeoutRunnable, 15000);

        // Query Firebase database to find user by username
        Query query = mDatabase.child("users").orderByChild("username").equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Remove timeout since we got a response
                timeoutHandler.removeCallbacks(timeoutRunnable);

                if (snapshot.exists()) {
                    // Username found, get the email
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null && !email.isEmpty()) {
                            // Login with the found email
                            loginWithEmail(email, password);
                            return;
                        }
                    }
                    // If we reach here, email wasn't found (shouldn't happen)
                    Toast.makeText(LoginActivity.this, "User data error. Please try again.",
                            Toast.LENGTH_LONG).show();
                    resetButton();
                } else {
                    // Username not found
                    Toast.makeText(LoginActivity.this, "Invalid username or password. Please check your credentials.",
                            Toast.LENGTH_LONG).show();
                    resetButton();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Remove timeout since we got a response (even if it's an error)
                timeoutHandler.removeCallbacks(timeoutRunnable);

                String errorMessage = "Login failed";
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("Permission denied")) {
                        errorMessage = "Database access denied. Please contact support.";
                    } else if (error.getMessage().contains("network")) {
                        errorMessage = "Network error. Please check your internet connection.";
                    } else {
                        errorMessage = "Database error: " + error.getMessage();
                    }
                }

                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                resetButton();
            }
        });
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Login success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (!user.isEmailVerified()) {
                                    Toast.makeText(LoginActivity.this, "Please verify your email to continue.",
                                            Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(LoginActivity.this, "Login successful!",
                                            Toast.LENGTH_SHORT).show();

                                    // Navigate to main activity or role selection
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        } else {
                            // Login failed - Enhanced error handling
                            String errorMessage = "Login failed";

                            if (task.getException() != null) {
                                Exception exception = task.getException();

                                if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                    // Wrong password or email doesn't exist - show generic message
                                    loginPassword.setError("Invalid email or password");
                                    loginPassword.requestFocus();
                                    Toast.makeText(LoginActivity.this,
                                        "Invalid email or password. Please try again.",
                                        Toast.LENGTH_LONG).show();
                                    resetButton();
                                    return;
                                } else if (exception instanceof FirebaseAuthInvalidUserException) {
                                    FirebaseAuthInvalidUserException invalidUserException = (FirebaseAuthInvalidUserException) exception;
                                    String errorCode = invalidUserException.getErrorCode();

                                    if ("USER_NOT_FOUND".equals(errorCode)) {
                                        loginPassword.setError("Invalid email or password");
                                        loginPassword.requestFocus();
                                        Toast.makeText(LoginActivity.this,
                                            "Invalid email or password. Please try again.",
                                            Toast.LENGTH_LONG).show();
                                        resetButton();
                                        return;
                                    } else if ("USER_DISABLED".equals(errorCode)) {
                                        errorMessage = "This account has been disabled";
                                    } else {
                                        errorMessage = "Account error: " + errorCode;
                                    }
                                } else {
                                    // Check for specific error messages in the exception
                                    String exceptionMessage = exception.getMessage();
                                    if (exceptionMessage != null) {
                                        if (exceptionMessage.contains("password is invalid") ||
                                                exceptionMessage.contains("INVALID_PASSWORD") ||
                                                exceptionMessage.contains("no user record") ||
                                                exceptionMessage.contains("USER_NOT_FOUND")) {
                                            loginPassword.setError("Invalid email or password");
                                            loginPassword.requestFocus();
                                            Toast.makeText(LoginActivity.this,
                                                "Invalid email or password. Please try again.",
                                                Toast.LENGTH_LONG).show();
                                            resetButton();
                                            return;
                                        } else if (exceptionMessage.contains("too many requests") ||
                                                exceptionMessage.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
                                            errorMessage = "Too many failed attempts. Try again later";
                                        } else if (exceptionMessage.contains("network error") ||
                                                exceptionMessage.contains("NETWORK_ERROR")) {
                                            errorMessage = "Network error. Please check your internet connection";
                                        } else {
                                            errorMessage = "Login failed: " + exceptionMessage;
                                        }
                                    }
                                }
                            }

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            resetButton();
                        }
                    }
                });
    }

    private void resetButton() {
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            if (currentUser.isEmailVerified()) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    // Helper method to clear all input fields
    private void clearFields() {
        loginUsername.setText("");
        loginPassword.setText("");
    }

    @Override
    public void onBackPressed() {
        // Exit app instead of navigating to MainActivity
        super.onBackPressed();
        finishAffinity(); // This will close all activities and exit the app
    }

    // Enhanced forgot password functionality with dialog
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Enter your email address to receive a password reset link:");

        // Create EditText for email input
        final EditText emailInput = new EditText(this);
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setHint("Enter your email");

        // Set some padding
        emailInput.setPadding(50, 30, 50, 30);
        builder.setView(emailInput);

        builder.setPositiveButton("Send Reset Email", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = emailInput.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginActivity.this, "Please enter your email address",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(LoginActivity.this, "Please enter a valid email address",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Send password reset email
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this,
                                            "Password reset email sent to " + email,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    String errorMessage = "Failed to send reset email";
                                    if (task.getException() != null) {
                                        String exception = task.getException().getMessage();
                                        if (exception != null) {
                                            if (exception.contains("no user record")) {
                                                errorMessage = "No account found with this email";
                                            } else {
                                                errorMessage += ": " + exception;
                                            }
                                        }
                                    }
                                    Toast.makeText(LoginActivity.this, errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    // Dialog to show when email doesn't exist
    private void showEmailNotFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Account Not Found")
                .setMessage("No account exists with this email address.\n\n" +
                           "Would you like to create a new account?")
                .setPositiveButton("Sign Up", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Navigate to Sign Up Activity
                        Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }

    // Check if email exists in Firestore
    private void checkEmailExistence(String email, OnEmailCheckCallback callback) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        firestore.collection("Users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    boolean foundInUsers = task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty();
                    if (foundInUsers) {
                        callback.onEmailExists(true);
                    } else {
                        firestore.collection("users")
                                .whereEqualTo("email", email)
                                .limit(1)
                                .get()
                                .addOnCompleteListener(lowerTask -> {
                                    boolean foundInLowercase = lowerTask.isSuccessful() && lowerTask.getResult() != null && !lowerTask.getResult().isEmpty();
                                    callback.onEmailExists(foundInLowercase);
                                });
                    }
                });
    }

    // Callback interface for email existence check
    interface OnEmailCheckCallback {
        void onEmailExists(boolean exists);
    }
}