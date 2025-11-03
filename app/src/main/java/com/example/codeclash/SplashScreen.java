package com.example.codeclash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashScreen extends AppCompatActivity {

    private Handler handler;
    private Runnable splashRunnable;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Get the root view (whatever the root element is)
        View rootView = findViewById(android.R.id.content);

        // Apply window insets to the root view
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize handler and runnable
        handler = new Handler(Looper.getMainLooper());
        splashRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    checkUserAndNavigate();
                }
            }
        };

        // Navigate after 4 seconds
        handler.postDelayed(splashRunnable, 4000);
    }

    private void checkUserAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Intent intent;

        if (currentUser != null) {
            // User is logged in, check if email is verified
            if (currentUser.isEmailVerified()) {
                // User is logged in and verified, go to MainActivity
                intent = new Intent(SplashScreen.this, MainActivity.class);
            } else {
                // User is logged in but not verified, go to VerifyEmailActivity
                intent = new Intent(SplashScreen.this, VerifyEmailActivity.class);
            }
        } else {
            // User is not logged in, go to SignupActivity
            intent = new Intent(SplashScreen.this, SignupActivity.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove callbacks to prevent memory leaks
        if (handler != null && splashRunnable != null) {
            handler.removeCallbacks(splashRunnable);
        }
    }
}