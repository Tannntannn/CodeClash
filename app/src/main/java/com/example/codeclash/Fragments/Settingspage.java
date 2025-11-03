package com.example.codeclash.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.codeclash.R;
import com.example.codeclash.LoginActivity; // Changed to LoginActivity
import com.google.firebase.auth.FirebaseAuth; // Added Firebase Auth

public class Settingspage extends Fragment {

    View view;
    private FirebaseAuth mAuth; // Added Firebase Auth instance

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize logout button
        Button logoutBtn = view.findViewById(R.id.btn_logout);
        logoutBtn.setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Perform logout
                    performLogout();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Dismiss dialog
                    dialog.dismiss();
                })
                .show();
    }

    private void performLogout() {
        // Sign out from Firebase
        mAuth.signOut();

        // Clear any stored user data/preferences here
        if (getActivity() != null) {
            // Clear SharedPreferences if you're using them
            getActivity().getSharedPreferences("user_prefs", getActivity().MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
        }

        // Show logout success message
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate back to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class); // Changed to LoginActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);

        // Finish current activity
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}