package com.cameron.spotifyadblocker.detection;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.cameron.spotifyadblocker.R;

import java.util.logging.Logger;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

public enum DetectionMethods {
    LISTENER(R.string.notification_requirement_notice) {
        @Override
        public void start(Activity activity) throws DeviceAccessException {
            String currentPackage = activity.getApplicationContext().getPackageName();
            if (!NotificationManagerCompat.getEnabledListenerPackages(activity).contains(currentPackage)) {
                throw new DeviceAccessException();
            }
            if (!CustomNotificationListener.isRunning()) {
                activity.startService(serviceIntent(activity));
            }
        }

        @Override
        public void stop(Activity activity) {
            CustomNotificationListener.killService();
            activity.stopService(serviceIntent(activity));
        }

        @Override
        public void requestAccess(Activity activity) {
            activity.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }

        /**
         * Singleton Intent used for initializing the {@link CustomNotificationListener}.
         *
         * @param activity The calling {@link Activity}.
         * @return An intent to {@link CustomNotificationListener}.
         */
        private Intent serviceIntent(Activity activity) {
            return new Intent(activity, CustomNotificationListener.class);
        }
    },
    BROADCAST_RECEIVER(R.string.broadcast_requirement_notice) {
        @Override
        public void start(Activity activity) {
            ComponentName componentName = getComponentName(activity);
            activity.getPackageManager().setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_ENABLED, 0);
        }

        @Override
        public void stop(Activity activity) {
            ComponentName componentName = getComponentName(activity);
            activity.getPackageManager().setComponentEnabledSetting(componentName, COMPONENT_ENABLED_STATE_DISABLED, 0);
        }

        @Override
        public void requestAccess(Activity activity) {
            Toast.makeText(activity,
                    "Please open the Spotify application settings.", Toast.LENGTH_SHORT).show();
        }

        private ComponentName getComponentName(Activity activity) {
            String receiverPackage = activity.getApplicationContext().getPackageName();
            String receiverClass = MediaBroadcastReceiver.class.getCanonicalName();
            ComponentName componentName = new ComponentName(receiverPackage, receiverClass);
            Log.d("BROADCAST_RECEIVER", String.format("Using component package: %s - class: %s", receiverPackage, receiverClass));
            Log.d("BROADCAST_RECEIVER", String.format("Generated component: %s", componentName));
            return componentName;
        }

    };

    private final int notice_id;

    public int getNoticeId() {
        return notice_id;
    }

    /**
     * Initialize a new service to monitor the Spotify activity from an {@link Activity}.
     *
     * @param activity The calling {@link Activity}.
     * @throws DeviceAccessException If the application is missing accesses on the device.
     */
    public abstract void start(Activity activity) throws DeviceAccessException;

    /**
     * Stop the currently running service from an {@link Activity}.
     *
     * @param activity The calling {@link Activity}.
     */
    public abstract void stop(Activity activity);

    /**
     * Request the mandatory access to start the detection mechanism.
     *
     * @param activity The calling {@link Activity}.
     */
    public abstract void requestAccess(Activity activity);

    DetectionMethods(int notice_id) {
        this.notice_id = notice_id;
    }
}
