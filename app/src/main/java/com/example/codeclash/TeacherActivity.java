package com.example.codeclash;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.codeclash.Fragments.Addpage;
import com.example.codeclash.Fragments.Settingspage;
import com.example.codeclash.Fragments.Homepage;
import com.example.codeclash.Fragments.Leaderboards;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherActivity extends AppCompatActivity implements ConnectionManager.ConnectionListener, ConnectionStatusDialog.ConnectionDialogListener {

    private ConnectionManager connectionManager;
    private ResilientFirebaseHelper resilientHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher);
        
        // Initialize connection monitoring
        connectionManager = ConnectionManager.getInstance(this);
        connectionManager.addConnectionListener(this);
        resilientHelper = new ResilientFirebaseHelper(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Change this line - use OnItemSelectedListener instead of OnItemReselectedListener
        bottomNav.setOnItemSelectedListener(navListener);

        // Load default fragment
        Fragment selectedFragment = new Homepage();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
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
        System.out.println("🔗 TeacherActivity: Connection changed - " + (isConnected ? "Connected" : "Disconnected"));
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

    // Change the listener type here too
    private BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        int itemId = item.getItemId();
        Fragment selectedFragment = null;

        if (itemId == R.id.nav_home) {
            selectedFragment = new Homepage();
        } else if (itemId == R.id.nav_addpage) {
            selectedFragment = new Addpage();
        } else if (itemId == R.id.nav_leaderboards) {
            selectedFragment = new Leaderboards();
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new Settingspage();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }

        return true; // Important: return true to indicate the item selection was handled
    };
}