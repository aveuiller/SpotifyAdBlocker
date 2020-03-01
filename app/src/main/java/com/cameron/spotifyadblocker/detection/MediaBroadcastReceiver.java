package com.cameron.spotifyadblocker.detection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cameron.spotifyadblocker.processing.MuteAdsProcessor;

public class MediaBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaBroadcastReceiver";

    static final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    /**
     * Holding a static {@link MuteAdsProcessor} instance to keep track of the system volume
     * configuration through track changes.
     */
    private static MuteAdsProcessor adsProcessor;
    private static long lastEvent = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Initialize adsProcessor is needed.
        if (adsProcessor == null) {
            adsProcessor = new MuteAdsProcessor(context, new Blocklist(context));
        }

        // Check event consistency
        long timeSentInMs = intent.getLongExtra("timeSent", 0L);
        if (timeSentInMs < lastEvent) {
            Log.w(TAG, String.format("Received late event (event: %s, previous %s)", timeSentInMs, lastEvent));
            return;
        }
        lastEvent = timeSentInMs;

        String action = intent.getAction();
        Log.d(TAG, String.format("Got Spotify intent %s", action));
        if (BroadcastTypes.METADATA_CHANGED.equals(action)) {
            String trackName = intent.getStringExtra("track");
            adsProcessor.handleTitle(trackName);
        } else {
            Log.d(TAG, String.format("Unused intent action %s", action));
        }
    }
}
