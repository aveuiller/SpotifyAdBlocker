package com.cameron.spotifyadblocker.detection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.cameron.spotifyadblocker.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Load and use a track blocklist.
 */
public class Blocklist {
    private static final String TAG = "MuteAdsProcessor";

    private final HashSet<String> blocklist = new HashSet<>();
    private Context context;

    public Blocklist(Context context) {
        this.context = context;
        this.blocklist.addAll(fetchDefaults());
        this.blocklist.addAll(fetchUserDefined());
    }

    /**
     * Determine if the blocklist contains the given name.
     *
     * @param name The name to check against the blocklist.
     * @return True if the name is blocked, false otherwise.
     */
    public boolean isBlocked(String name) {
        return blocklist.contains(name);
    }

    /**
     * Fetch the tracks blocked by default via the application resources.
     *
     * @return A list of blocked names.
     */
    public List<String> fetchDefaults() {
        List<String> bl = new ArrayList<>();
        Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.blocklist);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                bl.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to read default blocklist", e);
        }
        return bl;
    }

    /**
     * Fetch the user defined tracks to block.
     *
     * @return A list of blocked names.
     */
    public List<String> fetchUserDefined() {
        SharedPreferences preferences = getBlocklistPreferences();
        return new ArrayList<>((Collection<? extends String>) preferences.getAll().values());
    }

    /**
     * Add a new filter to the list.
     *
     * @param name The filter to add.
     */
    public void addUserDefined(String name) {
        SharedPreferences.Editor preferencesEditor = getBlocklistPreferences().edit();
        if (!name.isEmpty()) {
            preferencesEditor.putString("filter_" + name, name);
            preferencesEditor.apply();
            blocklist.add(name);
        }
    }

    /**
     * Remove one of the filters from the list.
     *
     * @param name The filter to remove.
     */
    public void removeUserDefined(String name) {
        SharedPreferences.Editor preferencesEditor = getBlocklistPreferences().edit();
        preferencesEditor.remove("filter_" + name);
        preferencesEditor.apply();
        blocklist.remove(name);
    }

    private SharedPreferences getBlocklistPreferences() {
        String filtersPrefs = context.getString(R.string.saved_filters);
        return context.getSharedPreferences(filtersPrefs, MODE_PRIVATE);
    }

}
