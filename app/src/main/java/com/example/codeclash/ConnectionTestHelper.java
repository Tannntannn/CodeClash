package com.example.codeclash;

import android.content.Context;
import android.widget.Toast;

/**
 * Helper class to test connection handling features
 * This can be used to simulate poor connections for testing
 */
public class ConnectionTestHelper {
    
    /**
     * Simulate a poor connection scenario for testing
     */
    public static void simulatePoorConnection(Context context) {
        Toast.makeText(context, "🧪 Testing: Simulating poor connection...", Toast.LENGTH_SHORT).show();
        
        // Get the connection manager and simulate poor connection
        ConnectionManager connectionManager = ConnectionManager.getInstance(context);
        ResilientFirebaseHelper resilientHelper = new ResilientFirebaseHelper(context);
        
        // Show poor connection dialog
        resilientHelper.showPoorConnectionDialog();
    }
    
    /**
     * Simulate a connection timeout for testing
     */
    public static void simulateTimeout(Context context) {
        Toast.makeText(context, "🧪 Testing: Simulating timeout...", Toast.LENGTH_SHORT).show();
        
        ResilientFirebaseHelper resilientHelper = new ResilientFirebaseHelper(context);
        resilientHelper.showTimeoutDialog();
    }
    
    /**
     * Simulate connection lost for testing
     */
    public static void simulateConnectionLost(Context context) {
        Toast.makeText(context, "🧪 Testing: Simulating connection lost...", Toast.LENGTH_SHORT).show();
        
        ResilientFirebaseHelper resilientHelper = new ResilientFirebaseHelper(context);
        resilientHelper.showConnectionLostDialog();
    }
    
    /**
     * Simulate connection restored for testing
     */
    public static void simulateConnectionRestored(Context context) {
        Toast.makeText(context, "🧪 Testing: Simulating connection restored...", Toast.LENGTH_SHORT).show();
        
        ResilientFirebaseHelper resilientHelper = new ResilientFirebaseHelper(context);
        resilientHelper.showConnectionRestoredDialog();
    }
}

