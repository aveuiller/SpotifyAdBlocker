package com.cameron.spotifyadblocker.detection;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.cameron.spotifyadblocker.processing.MuteAdsProcessor;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Cameron on 6/7/2016.
 */
// Note: Rename this class during debugging (Refactor->Rename for ease). Android caching may prevent the service from binding on a previously-tested device.
public class CustomNotificationListener extends NotificationListenerService {
    private static String currentTitle;
    private static Timer timer;
    private static boolean running;


    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startID) {
        timer = new Timer();
        running = true;
        final Blocklist blocklist = new Blocklist(this);
        final MuteAdsProcessor adsProcessor = new MuteAdsProcessor(this, blocklist);

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                StatusBarNotification[] notifications = getActiveNotifications();
                if (notifications == null) {
                    Log.d("DEBUG", "No access to notifications.");
                    return;
                }
                Notification notification = getSpotifyNotification();
                if (notification == null) {
                    Log.d("DEBUG", "No spotify in notifications found.");
                    return;
                }

                // Retrieve title and handle it on the add processor.
                CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
                currentTitle = title == null ? "" : title.toString();
                Log.d("DEBUG", currentTitle);
                adsProcessor.handleTitle(currentTitle);
            }
        }, 10, 250);
        return START_NOT_STICKY;
    }

    public static void killService() {
        timer.cancel();
        running = false;
    }

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onDestroy() {
        Log.d("DEBUG", "Destroying Service");
        try {
            killService();
            Log.d("DEBUG", "Timer canceled.");
        } catch (NullPointerException ex) {
            Log.w("WARN", "NullPointer encountered while cancelling timer.");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
    }

    public static String getCurrentTitle() {
        return currentTitle == null ? "" : currentTitle;
    }

    private Notification getSpotifyNotification() {
        Notification spotifyNotification = null;
        StatusBarNotification[] notifications = getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            String name = notification.getPackageName();
            if (name.contains("spotify")) {
                Log.d("DEBUG", name);
                spotifyNotification = notification.getNotification();
                break;
            }
        }
        return spotifyNotification;
    }
}
