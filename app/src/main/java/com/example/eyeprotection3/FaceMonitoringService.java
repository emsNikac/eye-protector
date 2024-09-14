package com.example.eyeprotection3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class FaceMonitoringService extends Service {

    private static final String CHANNEL_ID = "eye_protection_channel";
    private static final int NOTIFICATION_ID = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Eye Protection Service";
            String description = "Channel for eye protection service";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, CameraActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Eye Protection Service")
                .setContentText("Monitoring face distance to protect your eyes.")
                .setSmallIcon(R.drawable.ic_warning) // Ensure this icon exists in your drawable resources
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start camera monitoring or other background operations here
        return START_NOT_STICKY;
    }
}
