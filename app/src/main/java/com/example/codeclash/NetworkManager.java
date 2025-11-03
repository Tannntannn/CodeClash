package com.example.codeclash;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Network utility class to handle connectivity checks and Firebase offline persistence
 * Enhanced with poor connection handling
 */
public class NetworkManager {
    
    private static boolean isOfflinePersistenceEnabled = false;
    private static ConnectionManager connectionManager;
    
    /**
     * Enable Firebase offline persistence (call this once in MainActivity)
     */
    public static void enableOfflinePersistence() {
        if (isOfflinePersistenceEnabled) {
            return; // Already enabled
        }
        
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            isOfflinePersistenceEnabled = true;
            System.out.println("✅ NetworkManager: Firebase offline persistence enabled");
        } catch (Exception e) {
            System.out.println("❌ NetworkManager: Failed to enable offline persistence: " + e.getMessage());
            // Note: Cannot show Toast here as we don't have context in this static method
            // The error is logged and will be handled by the calling activity
        }
    }
    
    /**
     * Initialize connection manager for advanced connection handling
     */
    public static void initializeConnectionManager(Context context) {
        if (connectionManager == null) {
            connectionManager = ConnectionManager.getInstance(context);
            System.out.println("✅ NetworkManager: Connection manager initialized");
        }
    }
    
    /**
     * Get connection manager instance
     */
    public static ConnectionManager getConnectionManager(Context context) {
        if (connectionManager == null) {
            initializeConnectionManager(context);
        }
        return connectionManager;
    }
    
    /**
     * Check if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            System.out.println("❌ NetworkManager: Error checking network: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Show network error message
     */
    public static void showNetworkError(Context context) {
        if (context != null) {
            Toast.makeText(context, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show offline message
     */
    public static void showOfflineMessage(Context context) {
        if (context != null) {
            Toast.makeText(context, "You're offline. Some features may be limited.", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show retry message
     */
    public static void showRetryMessage(Context context) {
        if (context != null) {
            Toast.makeText(context, "Connection failed. Retrying...", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check if network is available with detailed error info
     */
    public static NetworkStatus getNetworkStatus(Context context) {
        if (context == null) {
            return new NetworkStatus(false, "Context is null");
        }
        
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                return new NetworkStatus(false, "Connectivity service unavailable");
            }
            
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null) {
                return new NetworkStatus(false, "No active network");
            }
            
            if (!activeNetworkInfo.isConnected()) {
                return new NetworkStatus(false, "Network not connected");
            }
            
            if (!activeNetworkInfo.isAvailable()) {
                return new NetworkStatus(false, "Network not available");
            }
            
            return new NetworkStatus(true, "Network available");
            
        } catch (Exception e) {
            return new NetworkStatus(false, "Error checking network: " + e.getMessage());
        }
    }
    
    /**
     * Network status result class
     */
    public static class NetworkStatus {
        public final boolean isAvailable;
        public final String message;
        
        public NetworkStatus(boolean isAvailable, String message) {
            this.isAvailable = isAvailable;
            this.message = message;
        }
    }
}
