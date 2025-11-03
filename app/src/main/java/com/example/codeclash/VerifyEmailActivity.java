package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private TextView infoText;
    private Button btnResend;
    private Button btnRefresh;
    private Button btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();

        infoText = findViewById(R.id.verify_info_text);
        btnResend = findViewById(R.id.btn_resend_verification);
        btnRefresh = findViewById(R.id.btn_refresh_status);
        btnLogout = findViewById(R.id.btn_logout);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        String email = user.getEmail() != null ? user.getEmail() : "your email";
        infoText.setText("We sent a verification link to " + email + "\nPlease verify to continue.");

        btnResend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser current = mAuth.getCurrentUser();
                if (current == null) {
                    navigateToLogin();
                    return;
                }
                current.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(VerifyEmailActivity.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = task.getException() != null ? task.getException().getMessage() : "Failed to send";
                            Toast.makeText(VerifyEmailActivity.this, "Failed to send: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser current = mAuth.getCurrentUser();
                if (current == null) {
                    navigateToLogin();
                    return;
                }
                current.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseUser refreshed = mAuth.getCurrentUser();
                        if (refreshed != null && refreshed.isEmailVerified()) {
                            Toast.makeText(VerifyEmailActivity.this, "Email verified!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(VerifyEmailActivity.this, "Still not verified. Please check your inbox.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                navigateToLogin();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            navigateToLogin();
            return;
        }
        if (current.isEmailVerified()) {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(VerifyEmailActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(VerifyEmailActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


