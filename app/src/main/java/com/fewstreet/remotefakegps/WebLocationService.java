package com.fewstreet.remotefakegps;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;

/**
 * Created by peter on 7/20/16.
 */
public class WebLocationService extends Service {
    private static final String TAG = "WebLocationService";
    private GPSHTTPD webserver;
    public final int PORT = 8080;
    public static final String CLOSE_ACTION = "fakegpsclose";
    private static final int NOTIFICATION = 1;
    private LocationManager mLocationManager;
    @Nullable
    private NotificationManager mNotificationManager = null;
    private final NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);

    @Override
    public void onCreate() {
        super.onCreate();
        webserver = new GPSHTTPD(PORT, getApplicationContext());
        Log.v(TAG, "WebLocationService Created");
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(GPSHTTPD.NEW_LOCATION_CHOSEN_ACTION));
        setupNotifications();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);
        webserver.stop();
        Log.v(TAG, "WebLocationService Destroyed");
        mNotificationManager.cancelAll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "WebLocationService Started");
        if(!webserver.isAlive()) {
            try {
                webserver.start();
                Log.v(TAG, "WebServer Launched");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.v(TAG, "WebServer Already Running");
        }
        showNotification();
        return START_STICKY;
    }

    public void onTaskRemoved(Intent rootIntent) {
        mNotificationManager.cancelAll();
        //stop service
        stopSelf();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupNotifications() { //called in onCreate()
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MapActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MapActivity.class)
                        .setAction(CLOSE_ACTION)
                , 0);
        mNotificationBuilder
                .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_exit), pendingCloseIntent)
                .setOngoing(true);
    }

    private void showNotification() {
        mNotificationBuilder
                .setTicker(getText(R.string.service_connected))
                .setContentText(getText(R.string.service_connected));
        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION, mNotificationBuilder.build());
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            // Get extra data included in the Intent
            float lat = intent.getFloatExtra("lat", 0);
            float lng = intent.getFloatExtra("lng", 0);
            Log.d(TAG, "Got message: " + lat + " " + lng);
        }
    };
}
