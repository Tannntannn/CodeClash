package com.example.codeclash;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "class_requests_channel";
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage);
        }
        
        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message notification body: " + remoteMessage.getNotification().getBody());
            showNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }
    
    private void handleDataMessage(RemoteMessage remoteMessage) {
        String type = remoteMessage.getData().get("type");
        
        if (type != null) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            
            showNotification(title, body, remoteMessage.getData());
        }
    }
    
    private void showNotification(String title, String messageBody, java.util.Map<String, String> data) {
        Intent intent;
        
        // Determine which activity to open based on notification type
        String type = data.get("type");
        if ("join_request".equals(type) || "leave_request".equals(type)) {
            // For teachers - open approval activity
            intent = new Intent(this, TeacherApprovalActivity.class);
            String classCode = data.get("classCode");
            if (classCode != null) {
                intent.putExtra("classCode", classCode);
            }
        } else if ("kicked".equals(type) || "approved".equals(type) || "rejected".equals(type)) {
            // For students - open my classes
            intent = new Intent(this, MyClassesActivity.class);
        } else {
            // Default to main activity
            intent = new Intent(this, MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        createNotificationChannel();
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.newlogo)
            .setContentTitle(title != null ? title : "CodeClash")
            .setContentText(messageBody != null ? messageBody : "New notification")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
            Log.d(TAG, "Notification shown");
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Class Requests",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for class activities");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Send token to Firestore for this user
        saveTokenToFirestore(token);
    }
    
    private void saveTokenToFirestore(String token) {
        // Get current user ID
        com.google.firebase.auth.FirebaseUser currentUser = 
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            String userId = currentUser.getUid();
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save token", e));
        }
    }
}



