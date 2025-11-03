package com.example.codeclash;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to manage consistent user name lookups throughout the app
 * Uses signup names from users collection as the single source of truth
 */
public class UserNameManager {
    
    private static final Map<String, String> nameCache = new HashMap<>();
    
    public interface NameCallback {
        void onSuccess(String name);
        void onFailure(String fallbackName);
    }
    
    /**
     * Get user's signup name from users collection
     * Uses cache for performance
     */
    public static void getUserName(String userId, NameCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onFailure("Unknown Student");
            return;
        }
        
        // Check cache first
        if (nameCache.containsKey(userId)) {
            callback.onSuccess(nameCache.get(userId));
            return;
        }
        
        // Try capital 'Users' first (matches most code paths), then fallback to lowercase 'users'
        fetchNameFromCollection("Users", userId, new NameCallback() {
            @Override
            public void onSuccess(String name) {
                nameCache.put(userId, name);
                callback.onSuccess(name);
            }

            @Override
            public void onFailure(String fallbackName) {
                // Fallback to lowercase collection
                fetchNameFromCollection("users", userId, new NameCallback() {
                    @Override
                    public void onSuccess(String name) {
                        nameCache.put(userId, name);
                        callback.onSuccess(name);
                    }

                    @Override
                    public void onFailure(String fallbackName2) {
                        callback.onFailure("Unknown Student");
                    }
                });
            }
        });
    }

    private static void fetchNameFromCollection(String collection, String userId, NameCallback callback) {
        FirebaseFirestore.getInstance()
                .collection(collection)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("Unknown");
                        return;
                    }
                    // Support multiple possible name fields
                    String name = firstNonEmpty(
                            documentSnapshot.getString("fullName"),
                            documentSnapshot.getString("name"),
                            documentSnapshot.getString("displayName")
                    );
                    if (name != null && !name.isEmpty()) {
                        callback.onSuccess(name);
                    } else {
                        callback.onFailure("Unknown");
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("❌ UserNameManager: Failed to load name from " + collection + "/" + userId + ": " + e.getMessage());
                    callback.onFailure("Unknown");
                });
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v;
        }
        return null;
    }
    
    /**
     * Get teacher's name for class display
     */
    public static void getTeacherName(String teacherId, NameCallback callback) {
        getUserName(teacherId, new NameCallback() {
            @Override
            public void onSuccess(String name) {
                callback.onSuccess(name);
            }
            
            @Override
            public void onFailure(String fallbackName) {
                callback.onFailure("Unknown Teacher");
            }
        });
    }
    
    /**
     * Clear cache (useful for logout/login scenarios)
     */
    public static void clearCache() {
        nameCache.clear();
    }
    
    /**
     * Pre-cache multiple names for better performance
     */
    public static void preloadNames(String[] userIds) {
        for (String userId : userIds) {
            if (!nameCache.containsKey(userId)) {
                getUserName(userId, new NameCallback() {
                    @Override
                    public void onSuccess(String name) {
                        // Already cached in getUserName
                    }
                    
                    @Override
                    public void onFailure(String fallbackName) {
                        // Handle silently for preloading
                    }
                });
            }
        }
    }
}
