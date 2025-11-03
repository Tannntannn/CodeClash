package com.example.codeclash;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NotificationListenerService extends Service {
    private static final String TAG = "NotificationService";
    private static final String CHANNEL_ID = "realtime_notifications";
    private static final int NOTIFICATION_ID = 1;
    
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration notificationListener;
    
    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (currentUserId != null) {
            startNotificationListener();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep service running
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void startNotificationListener() {
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        // Listen for new notifications in real-time
        notificationListener = db.collection("Users")
            .document(currentUserId)
            .collection("Notifications")
            .whereEqualTo("read", false)
            .addSnapshotListener((querySnapshot, error) -> {
                if (error != null) {
                    Log.e(TAG, "Notification listener error: " + error.getMessage());
                    return;
                }
                
                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String title = doc.getString("title");
                        String body = doc.getString("body");
                        String classCode = doc.getString("classCode");
                        
                        if (type != null && title != null && body != null) {
                            showRealtimeNotification(type, title, body, classCode);
                            
                            // Mark as read
                            doc.getReference().update("read", true);
                        }
                    }
                }
            });
    }
    
    private void showRealtimeNotification(String type, String title, String body, String classCode) {
        Intent intent;
        
        // Determine which activity to open based on notification type
        if ("join_request".equals(type) || "leave_request".equals(type)) {
            // For teachers - open approval activity
            intent = new Intent(this, TeacherApprovalActivity.class);
            if (classCode != null) {
                intent.putExtra("classCode", classCode);
            }
        } else if ("kicked".equals(type) || "join_approved".equals(type) || "join_rejected".equals(type) || 
                   "leave_approved".equals(type) || "leave_rejected".equals(type)) {
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
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent);
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
            Log.d(TAG, "Realtime notification shown: " + title);
        }
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Real-time Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for real-time class activities");
            channel.enableVibration(true);
            channel.enableLights(true);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
}



