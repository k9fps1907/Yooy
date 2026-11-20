package com.ds4.light;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

public class KeepAlive extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel ch = new NotificationChannel("ds4", "Luz dos controles", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        Notification n = new Notification.Builder(this, "ds4")
                .setContentTitle("Luz dos controles ativa")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
