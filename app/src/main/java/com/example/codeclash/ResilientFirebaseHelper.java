package com.example.codeclash;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Resilient Firebase operations that handle poor connections gracefully
 */
public class ResilientFirebaseHelper {
    private static final String TAG = "ResilientFirebase";
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private Context context;
    private ConnectionManager connectionManager;
    private FirebaseFirestore db;
    
    public ResilientFirebaseHelper(Context context) {
        this.context = context;
        this.connectionManager = ConnectionManager.getInstance(context);
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Execute a Firebase operation with retry and timeout handling
     */
    public <T> void executeWithResilience(Operation<T> operation, OperationCallback<T> callback) {
        executeWithResilience(operation, callback, MAX_RETRY_ATTEMPTS, DEFAULT_TIMEOUT_SECONDS);
    }
    
    public <T> void executeWithResilience(Operation<T> operation, OperationCallback<T> callback, 
                                        int maxRetries, int timeoutSeconds) {
        
        if (!connectionManager.isConnected()) {
            Log.d(TAG, "No connection, queuing operation");
            connectionManager.queueOperation(new ConnectionManager.QueuedOperation() {
                @Override
                public void execute() {
                    executeWithResilience(operation, callback, maxRetries, timeoutSeconds);
                }
                
                @Override
                public String getOperationName() {
                    return operation.getOperationName();
                }
                
                @Override
                public boolean isRetryable() {
                    return true;
                }
            });
            return;
        }
        
        // Execute operation with timeout
        Task<T> task = operation.execute();
        
        // Add timeout
        task.addOnCompleteListener(completedTask -> {
            if (completedTask.isSuccessful()) {
                Log.d(TAG, "Operation successful: " + operation.getOperationName());
                callback.onSuccess(completedTask.getResult());
            } else {
                Log.w(TAG, "Operation failed: " + operation.getOperationName() + 
                      " - " + (completedTask.getException() != null ? completedTask.getException().getMessage() : "Unknown error"));
                
                if (maxRetries > 0 && isRetryableError(completedTask.getException())) {
                    Log.d(TAG, "Retrying operation: " + operation.getOperationName() + 
                          " (attempts left: " + (maxRetries - 1) + ")");
                    
                    // Retry with exponential backoff
                    long delayMs = 1000 * (MAX_RETRY_ATTEMPTS - maxRetries + 1);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        executeWithResilience(operation, callback, maxRetries - 1, timeoutSeconds);
                    }, delayMs);
                } else {
                    callback.onFailure(completedTask.getException());
                }
            }
        });
        
        // Set timeout
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!task.isComplete()) {
                Log.w(TAG, "Operation timeout: " + operation.getOperationName());
                callback.onTimeout();
            }
        }, timeoutSeconds * 1000);
    }
    
    private boolean isRetryableError(Exception exception) {
        if (exception == null) return false;
        
        String message = exception.getMessage();
        if (message == null) return false;
        
        // Retry on network-related errors
        return message.contains("network") || 
               message.contains("timeout") || 
               message.contains("unavailable") ||
               message.contains("PERMISSION_DENIED") ||
               message.contains("UNAVAILABLE");
    }
    
    /**
     * Get document with resilience
     */
    public void getDocument(String collection, String documentId, DocumentCallback callback) {
        executeWithResilience(
            new Operation<DocumentSnapshot>() {
                @Override
                public Task<DocumentSnapshot> execute() {
                    return db.collection(collection).document(documentId).get();
                }
                
                @Override
                public String getOperationName() {
                    return "Get document: " + collection + "/" + documentId;
                }
            },
            new OperationCallback<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot result) {
                    callback.onSuccess(result);
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    /**
     * Set document with resilience
     */
    public void setDocument(String collection, String documentId, Map<String, Object> data, 
                           SimpleCallback callback) {
        executeWithResilience(
            new Operation<Void>() {
                @Override
                public Task<Void> execute() {
                    return db.collection(collection).document(documentId).set(data);
                }
                
                @Override
                public String getOperationName() {
                    return "Set document: " + collection + "/" + documentId;
                }
            },
            new OperationCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    /**
     * Update document with resilience
     */
    public void updateDocument(String collection, String documentId, Map<String, Object> data, 
                              SimpleCallback callback) {
        executeWithResilience(
            new Operation<Void>() {
                @Override
                public Task<Void> execute() {
                    return db.collection(collection).document(documentId).update(data);
                }
                
                @Override
                public String getOperationName() {
                    return "Update document: " + collection + "/" + documentId;
                }
            },
            new OperationCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    /**
     * Delete document with resilience
     */
    public void deleteDocument(String collection, String documentId, SimpleCallback callback) {
        executeWithResilience(
            new Operation<Void>() {
                @Override
                public Task<Void> execute() {
                    return db.collection(collection).document(documentId).delete();
                }
                
                @Override
                public String getOperationName() {
                    return "Delete document: " + collection + "/" + documentId;
                }
            },
            new OperationCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    /**
     * Query collection with resilience
     */
    public void queryCollection(Query query, QueryCallback callback) {
        executeWithResilience(
            new Operation<QuerySnapshot>() {
                @Override
                public Task<QuerySnapshot> execute() {
                    return query.get();
                }
                
                @Override
                public String getOperationName() {
                    return "Query collection";
                }
            },
            new OperationCallback<QuerySnapshot>() {
                @Override
                public void onSuccess(QuerySnapshot result) {
                    callback.onSuccess(result);
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    /**
     * Batch write with resilience
     */
    public void batchWrite(BatchOperation batchOperation, SimpleCallback callback) {
        executeWithResilience(
            new Operation<Void>() {
                @Override
                public Task<Void> execute() {
                    WriteBatch batch = db.batch();
                    batchOperation.execute(batch);
                    return batch.commit();
                }
                
                @Override
                public String getOperationName() {
                    return "Batch write";
                }
            },
            new OperationCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    callback.onSuccess();
                }
                
                @Override
                public void onFailure(Exception exception) {
                    callback.onFailure(exception);
                }
                
                @Override
                public void onTimeout() {
                    callback.onTimeout();
                }
            }
        );
    }
    
    // Interface definitions
    public interface Operation<T> {
        Task<T> execute();
        String getOperationName();
    }
    
    public interface OperationCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception exception);
        void onTimeout();
    }
    
    public interface DocumentCallback {
        void onSuccess(DocumentSnapshot document);
        void onFailure(Exception exception);
        void onTimeout();
    }
    
    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception exception);
        void onTimeout();
    }
    
    public interface QueryCallback {
        void onSuccess(QuerySnapshot querySnapshot);
        void onFailure(Exception exception);
        void onTimeout();
    }
    
    public interface BatchOperation {
        void execute(WriteBatch batch);
    }
    
    /**
     * Show user-friendly error messages
     */
    public void showErrorToast(Exception exception) {
        String message = "Operation failed";
        
        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("network") || errorMessage.contains("timeout")) {
                    message = "Network error. Please check your connection.";
                } else if (errorMessage.contains("PERMISSION_DENIED")) {
                    message = "Permission denied. Please try again.";
                } else if (errorMessage.contains("UNAVAILABLE")) {
                    message = "Service temporarily unavailable. Please try again.";
                } else {
                    message = "Error: " + errorMessage;
                }
            }
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    /**
     * Show connection status dialog
     */
    public void showConnectionDialog(String type, String title, String message, boolean showOfflineOption) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
            
            // Check if activity is in a valid state to show dialog
            if (activity.isFinishing() || activity.isDestroyed()) {
                // Activity is finishing or destroyed, use toast instead
                showErrorToast(new Exception(message));
                return;
            }
            
            // Check if we can safely show the dialog
            if (activity.getSupportFragmentManager().isStateSaved()) {
                // Fragment manager state is saved, use toast instead
                showErrorToast(new Exception(message));
                return;
            }
            
            try {
                ConnectionStatusDialog dialog = ConnectionStatusDialog.newInstance(
                    title, message, type, showOfflineOption
                );
                dialog.show(activity.getSupportFragmentManager(), "connection_dialog");
            } catch (IllegalStateException e) {
                // If we can't show the dialog, fall back to toast
                showErrorToast(new Exception(message));
            }
        } else {
            // Fallback to toast if not in FragmentActivity
            showErrorToast(new Exception(message));
        }
    }
    
    /**
     * Show connection lost dialog
     */
    public void showConnectionLostDialog() {
        showConnectionDialog(
            ConnectionStatusDialog.TYPE_CONNECTION_LOST,
            "No Internet Connection",
            "Please check your internet connection and try again.",
            true
        );
    }
    
    /**
     * Show timeout dialog
     */
    public void showTimeoutDialog() {
        showConnectionDialog(
            ConnectionStatusDialog.TYPE_TIMEOUT,
            "Request Timed Out",
            "The operation took too long to complete. This might be due to a poor connection.",
            true
        );
    }
    
    /**
     * Show poor connection dialog
     */
    public void showPoorConnectionDialog() {
        showConnectionDialog(
            ConnectionStatusDialog.TYPE_POOR_CONNECTION,
            "Poor Connection",
            "Your connection is slow. Some features may not work properly.",
            true
        );
    }
    
    /**
     * Show connection restored dialog
     */
    public void showConnectionRestoredDialog() {
        showConnectionDialog(
            ConnectionStatusDialog.TYPE_CONNECTION_RESTORED,
            "Connection Restored",
            "Your internet connection has been restored. Syncing data...",
            false
        );
    }
}
