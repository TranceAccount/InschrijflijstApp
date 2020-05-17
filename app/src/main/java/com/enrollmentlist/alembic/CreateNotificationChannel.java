package com.enrollmentlist.alembic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import static com.enrollmentlist.alembic.MainActivity.CHANNEL_ID;

class CreateNotificationChannel {

    private Context mContext;

    CreateNotificationChannel(Context mContext) {
        this.mContext = mContext;
    }

    /*
     * Create notification channel for higher versions of Android
     */
    void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Inschrijflijst Alembic",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("New Event Available");
            channel.setVibrationPattern(new long[]{0, 500, 500, 500}); // Not sure if necessary

            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}