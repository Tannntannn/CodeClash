package com.example.codeclash;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Advanced connection manager for handling poor network conditions
 */
public class ConnectionManager {
    private static final String TAG = "ConnectionManager";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    private static final long OPERATION_TIMEOUT_MS = 10000; // 10 seconds
    
    private static ConnectionManager instance;
    private Context context;
    private ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    private boolean isConnected = false;
    private List<ConnectionListener> listeners = new ArrayList<>();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    // Offline queue for operations
    private List<QueuedOperation> offlineQueue = new ArrayList<>();

    // Debounce handling
    private static final long DEBOUNCE_MS = 800L;
    private Boolean pendingState = null;
    private Runnable pendingRunnable = null;
    
    public interface ConnectionListener {
        void onConnectionChanged(boolean isConnected);
        void onConnectionRestored();
        void onConnectionLost();
    }
    
    public interface QueuedOperation {
        void execute();
        String getOperationName();
        boolean isRetryable();
    }
    
    private ConnectionManager(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        setupNetworkCallback();
        checkInitialConnection();
    }
    
    public static synchronized ConnectionManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectionManager(context);
        }
        return instance;
    }
    
    private void setupNetworkCallback() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            networkCallback = new NetworkCallback();
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        }
    }
    
    private void checkInitialConnection() {
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                isConnected = capabilities != null && 
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
    }
    
    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(Network network) {
            Log.d(TAG, "Network available");
            updateConnectionStatus(true);
        }
        
        @Override
        public void onLost(Network network) {
            Log.d(TAG, "Network lost");
            updateConnectionStatus(false);
        }
        
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            updateConnectionStatus(hasInternet);
        }
    }
    
    private void updateConnectionStatus(boolean connected) {
        boolean previousRequested = (pendingState != null) ? pendingState : isConnected;
        if (pendingRunnable != null) {
            mainHandler.removeCallbacks(pendingRunnable);
            pendingRunnable = null;
        }
        pendingState = connected;
        pendingRunnable = () -> {
            boolean wasConnected = isConnected;
            isConnected = pendingState;
            pendingState = null;
            if (isConnected && !wasConnected) {
                Log.d(TAG, "Connection restored - processing offline queue");
                processOfflineQueue();
                notifyConnectionRestored();
            } else if (!isConnected && wasConnected) {
                Log.d(TAG, "Connection lost");
                notifyConnectionLost();
            }
            notifyConnectionChanged(isConnected);
        };
        mainHandler.postDelayed(pendingRunnable, DEBOUNCE_MS);
    }
    
    private void processOfflineQueue() {
        if (offlineQueue.isEmpty()) return;
        
        Log.d(TAG, "Processing " + offlineQueue.size() + " queued operations");
        List<QueuedOperation> operationsToProcess = new ArrayList<>(offlineQueue);
        offlineQueue.clear();
        
        for (QueuedOperation operation : operationsToProcess) {
            if (operation.isRetryable()) {
                operation.execute();
            }
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void addConnectionListener(ConnectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyConnectionChanged(boolean connected) {
        for (ConnectionListener listener : listeners) {
            listener.onConnectionChanged(connected);
        }
    }
    
    private void notifyConnectionRestored() {
        // Use main thread to ensure UI operations are safe
        mainHandler.post(() -> {
            for (ConnectionListener listener : listeners) {
                try {
                    listener.onConnectionRestored();
                } catch (Exception e) {
                    Log.e(TAG, "notifying connection restored: " + e.getMessage());
                }
            }
        });
    }
    
    private void notifyConnectionLost() {
        // Use main thread to ensure UI operations are safe
        mainHandler.post(() -> {
            for (ConnectionListener listener : listeners) {
                try {
                    listener.onConnectionLost();
                } catch (Exception e) {
                    Log.e(TAG, "notifying connection lost: " + e.getMessage());
                }
            }
        });
    }
    
    public void queueOperation(QueuedOperation operation) {
        if (isConnected) {
            operation.execute();
        } else {
            offlineQueue.add(operation);
            Log.d(TAG, "Queued operation: " + operation.getOperationName());
        }
    }
    
    public void executeWithRetry(Runnable operation, String operationName, int maxRetries) {
        executeWithRetry(operation, operationName, maxRetries, 0);
    }
    
    private void executeWithRetry(Runnable operation, String operationName, int maxRetries, int currentAttempt) {
        if (currentAttempt >= maxRetries) {
            Log.e(TAG, "Max retries reached for: " + operationName);
            return;
        }
        
        if (!isConnected) {
            Log.d(TAG, "No connection, queuing operation: " + operationName);
            queueOperation(new QueuedOperation() {
                @Override
                public void execute() {
                    executeWithRetry(operation, operationName, maxRetries, currentAttempt);
                }
                
                @Override
                public String getOperationName() {
                    return operationName;
                }
                
                @Override
                public boolean isRetryable() {
                    return true;
                }
            });
            return;
        }
        
        operation.run();
        
        // Schedule retry if operation fails
        mainHandler.postDelayed(() -> {
            if (!isConnected) {
                executeWithRetry(operation, operationName, maxRetries, currentAttempt + 1);
            }
        }, RETRY_DELAY_MS * (currentAttempt + 1));
    }
    
    public void executeWithTimeout(Runnable operation, String operationName) {
        if (!isConnected) {
            queueOperation(new QueuedOperation() {
                @Override
                public void execute() {
                    executeWithTimeout(operation, operationName);
                }
                
                @Override
                public String getOperationName() {
                    return operationName;
                }
                
                @Override
                public boolean isRetryable() {
                    return true;
                }
            });
            return;
        }
        
        // Execute operation
        operation.run();
        
        // Set timeout
        mainHandler.postDelayed(() -> {
            Log.w(TAG, "Operation timeout: " + operationName);
        }, OPERATION_TIMEOUT_MS);
    }
    
    public void cleanup() {
        if (networkCallback != null && connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        listeners.clear();
        offlineQueue.clear();
    }
    
    /**
     * Get connection quality based on available networks
     */
    public ConnectionQuality getConnectionQuality() {
        if (!isConnected) {
            return ConnectionQuality.OFFLINE;
        }
        
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return ConnectionQuality.EXCELLENT;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        // For cellular networks, assume moderate quality
                        // In a real implementation, you could check signal strength or other metrics
                        return ConnectionQuality.POOR;
                    }
                }
            }
        }
        
        return ConnectionQuality.UNKNOWN;
    }
    
    public enum ConnectionQuality {
        OFFLINE,
        POOR,
        GOOD,
        EXCELLENT,
        UNKNOWN
    }
}
