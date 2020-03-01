package com.cameron.spotifyadblocker.processing;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.cameron.spotifyadblocker.detection.Blocklist;

import java.util.Objects;

/**
 * Handle the phone reaction on ad detected.
 */
public class MuteAdsProcessor {
    private static final String TAG = "MuteAdsProcessor";
    private static final int ZERO_VOLUME = 0;

    private final AudioManager audioManager;
    private final Blocklist blocklist;

    private boolean muted = false;
    private int originalVolume;

    public MuteAdsProcessor(Context context, Blocklist blocklist) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        originalVolume = Objects.requireNonNull(audioManager).getStreamVolume(AudioManager.STREAM_MUSIC);
        this.blocklist = blocklist;
    }

    /**
     * Mute the phone sound if the current title is considered as an ad and we were playing a song
     * before,
     * restore the sound value to its precedent value ff the title is not an ad and we were playing
     * and ad before.
     *
     * @param title Name of the currently played track.
     */
    public void handleTitle(String title) {
        boolean isAdPlaying = blocklist.isBlocked(title);
        Log.i(TAG, String.format("Checking track %s (is ad: %s)", title, isAdPlaying));
        if (isAdPlaying) {
            onAdDetected();
        } else {
            onAdEnded();
        }
    }

    /**
     * Stores the current phone volume and mute the phone.
     */
    private void onAdDetected() {
        if (!muted) {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            Log.i(TAG, String.format("Setting sound from %s to %s", originalVolume, ZERO_VOLUME));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ZERO_VOLUME, AudioManager.FLAG_SHOW_UI);
            muted = true;
        }
    }

    /**
     * Restore the phone volume to its original one.
     */
    private void onAdEnded() {
        if (muted) {
            Log.i(TAG, String.format("Setting sound from %s to %s", ZERO_VOLUME, originalVolume));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, AudioManager.FLAG_SHOW_UI);
            muted = false;
        }
    }
}
