package com.example.codeclash;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import org.godotengine.godot.GodotFragment;

public class GodotHostActivity extends AppCompatActivity {

    private GodotFragment godotFragment;
    private boolean isFragmentAttached = false;
    private boolean isDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_godot_host);

        // Enable edge-to-edge fullscreen without shifting layout inside Godot
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } catch (Exception ignored) {}

        // Persist selection for Godot launcher
        writeSelectionFiles();

        // Attach Godot fragment
        attachGodotFragment();
    }

    private void writeSelectionFiles() {
        String lessonName = getIntent().getStringExtra("lessonName");
        String activityType = getIntent().getStringExtra("activityType"); // "quiz" or "code_builder"

        if (lessonName == null) lessonName = "Lesson 1";
        if (activityType == null) activityType = "quiz";

        writeTextFile("scene_name.txt", lessonName);
        writeTextFile("activity_type.txt", activityType);
    }

    private void writeTextFile(String filename, String contents) {
        try {
            java.io.File file = new java.io.File(getFilesDir(), filename);
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(contents);
            writer.close();
        } catch (Exception ignored) { }
    }

    private void attachGodotFragment() {
        if (isDestroyed || isFragmentAttached) {
            return;
        }
        
        try {
            godotFragment = new GodotFragment();
            FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.godot_fragment_container, godotFragment);
            tx.commit();
            isFragmentAttached = true;
            System.out.println("🎮 GodotHostActivity: Godot fragment attached successfully");
        } catch (Exception e) {
            System.out.println("❌ GodotHostActivity: Error attaching Godot fragment: " + e.getMessage());
        }
    }

    private void detachGodotFragment() {
        if (!isFragmentAttached || godotFragment == null) {
            return;
        }
        
        try {
            System.out.println("🎮 GodotHostActivity: Detaching Godot fragment");
            
            // First, try to stop the Godot fragment
            if (godotFragment.isAdded()) {
                getSupportFragmentManager().beginTransaction()
                    .remove(godotFragment)
                    .commitAllowingStateLoss();
            }
            
            godotFragment = null;
            isFragmentAttached = false;
            System.out.println("🎮 GodotHostActivity: Godot fragment detached successfully");
        } catch (Exception e) {
            System.out.println("❌ GodotHostActivity: Error detaching Godot fragment: " + e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        System.out.println("🔙 GodotHostActivity: onBackPressed() called - IMMEDIATE EXIT");
        
        // IMMEDIATE EXIT: Kill process to ensure clean exit
        System.out.println("🔙 GodotHostActivity: IMMEDIATE EXIT - Killing process");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onPause() {
        System.out.println("🔙 GodotHostActivity: onPause() called");
        super.onPause();
        
        // PROPER APPROACH: Forward lifecycle to Godot fragment
        if (godotFragment != null && godotFragment.isAdded()) {
            System.out.println("🔙 GodotHostActivity: Forwarding onPause to Godot fragment");
            // Godot fragment will handle its own pause logic
        }
        
        // Don't detach fragment on pause - let Godot handle it
    }

    @Override
    protected void onStop() {
        System.out.println("🔙 GodotHostActivity: onStop() called");
        super.onStop();
        
        // Ensure fragment is detached
        detachGodotFragment();
    }

    @Override
    protected void onDestroy() {
        System.out.println("🔙 GodotHostActivity: onDestroy() called");
        
        // Mark as destroyed to prevent further operations
        isDestroyed = true;
        
        // Final cleanup - ensure fragment is completely removed
        detachGodotFragment();
        
        // Clear any remaining references
        godotFragment = null;
        isFragmentAttached = false;
        
        // Clean up Firebase instances to prevent memory leaks
        LessonManager.cleanup();
        LeaderboardManager.cleanup();
        
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply immersive flags when regaining focus
        try {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } catch (Exception ignored) {}
        
        // PROPER APPROACH: Forward lifecycle to Godot fragment
        if (godotFragment != null && godotFragment.isAdded()) {
            System.out.println("🔙 GodotHostActivity: Forwarding onResume to Godot fragment");
            // Godot fragment will handle its own resume logic
        }
        
        // Reattach fragment if it was detached and activity is not destroyed
        if (!isDestroyed && !isFragmentAttached) {
            attachGodotFragment();
        }
    }
}






