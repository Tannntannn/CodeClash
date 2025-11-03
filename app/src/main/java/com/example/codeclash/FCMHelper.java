package com.example.codeclash;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class FCMHelper {
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /**
     * Send notification via Firestore (which triggers FCM automatically if set up)
     * For now, we'll just create a notification document that the app can check
     */
    public static void sendNotificationToUser(String userId, String type, String title, String body, Map<String, String> extraData) {
        // First get the user's FCM token
        db.collection("Users").document(userId).get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    String fcmToken = userDoc.getString("fcmToken");
                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        // Send actual FCM notification
                        sendFCMPushNotification(fcmToken, type, title, body, extraData);
                    } else {
                        System.out.println("⚠️ FCM: No token found for user " + userId);
                    }
                }
                
                // Also create notification document for local checking
                createNotificationDocument(userId, type, title, body, extraData);
            })
            .addOnFailureListener(e -> {
                System.out.println("❌ FCM: Failed to get user token: " + e.getMessage());
                // Still create notification document as fallback
                createNotificationDocument(userId, type, title, body, extraData);
            });
    }
    
    private static void createNotificationDocument(String userId, String type, String title, String body, Map<String, String> extraData) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);
        
        // Add extra data
        if (extraData != null) {
            for (Map.Entry<String, String> entry : extraData.entrySet()) {
                notification.put(entry.getKey(), entry.getValue());
            }
        }
        
        db.collection("Users")
            .document(userId)
            .collection("Notifications")
            .document()
            .set(notification)
            .addOnSuccessListener(aVoid -> {
                System.out.println("📬 FCM: Notification document created for user " + userId);
            })
            .addOnFailureListener(e -> {
                System.out.println("❌ FCM: Failed to create notification document: " + e.getMessage());
            });
    }
    
    private static void sendFCMPushNotification(String fcmToken, String type, String title, String body, Map<String, String> extraData) {
        // For now, we'll use a simple HTTP request to FCM REST API
        // In production, this should be done server-side for security
        System.out.println("📱 FCM: Sending push notification to token " + fcmToken.substring(0, 10) + "...");
        System.out.println("📱 FCM: Title: " + title + ", Body: " + body);
        
        // Create notification data
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        if (extraData != null) {
            data.putAll(extraData);
        }
        
        Map<String, Object> message = new HashMap<>();
        message.put("token", fcmToken);
        message.put("notification", notification);
        message.put("data", data);
        
        // For now, just log the notification structure
        // In a real implementation, you would send this to FCM REST API
        System.out.println("📱 FCM: Notification structure: " + message.toString());
    }
    
    /**
     * Send a kick notification to a student
     */
    public static void sendKickNotification(String studentId, String classCode, String className) {
        Map<String, String> data = new HashMap<>();
        data.put("classCode", classCode);
        data.put("className", className);
        
        sendNotificationToUser(studentId, "kicked", "Removed from Class", 
            "You have been removed from class " + (className != null ? className : classCode), data);
    }
    
    /**
     * Send join request approval/rejection notifications
     */
    public static void sendJoinRequestNotification(String studentId, String classCode, boolean approved) {
        Map<String, String> data = new HashMap<>();
        data.put("classCode", classCode);
        
        String title = approved ? "Join Request Approved" : "Join Request Rejected";
        String body = approved 
            ? "Your request to join class " + classCode + " was approved!" 
            : "Your request to join class " + classCode + " was rejected";
        
        sendNotificationToUser(studentId, approved ? "join_approved" : "join_rejected", title, body, data);
    }
    
    /**
     * Send leave request approval/rejection notifications
     */
    public static void sendLeaveRequestNotification(String studentId, String classCode, String className, boolean approved) {
        Map<String, String> data = new HashMap<>();
        data.put("classCode", classCode);
        if (className != null) data.put("className", className);
        
        String title = approved ? "Leave Request Approved" : "Leave Request Rejected";
        String body = approved
            ? "You have been removed from class " + (className != null ? className : classCode)
            : "Your request to leave class " + (className != null ? className : classCode) + " was rejected";
        
        sendNotificationToUser(studentId, approved ? "leave_approved" : "leave_rejected", title, body, data);
    }
}

