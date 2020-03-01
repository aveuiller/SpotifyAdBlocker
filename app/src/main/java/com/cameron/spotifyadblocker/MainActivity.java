package com.cameron.spotifyadblocker;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.cameron.spotifyadblocker.detection.Blocklist;
import com.cameron.spotifyadblocker.detection.CustomNotificationListener;

import java.util.List;

public class MainActivity extends AppCompatActivity implements ViewAdditionalFiltersDialogFragment.ViewAdditionalFiltersDialogListener {
    private static final String TAG = "MainActivity";
    private static final String OWN_PACKAGE = "com.cameron.spotifyadblocker";
    private boolean enabled;
    private Intent serviceIntent;
    private Blocklist blocklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        serviceIntent = new Intent(this, CustomNotificationListener.class);
        blocklist = new Blocklist(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreCheckboxState();
    }

    private void restoreCheckboxState() {
        enabled = false;
        SharedPreferences preferences = getSharedPreferences(getString(R.string.saved_enabled), MODE_PRIVATE);
        enabled = preferences.getBoolean(getString(R.string.saved_enabled), enabled);
        CheckBox enabledCheckbox = (CheckBox) findViewById(R.id.checkBox);
        enabledCheckbox.setChecked(enabled);
        if (enabled && !CustomNotificationListener.isRunning())
            startService(serviceIntent);
    }

    public void onCheckboxClick(View view) {
        if (!NotificationManagerCompat.getEnabledListenerPackages(this).contains(OWN_PACKAGE)) {
            Toast.makeText(this, "Notification access denied", Toast.LENGTH_LONG).show();
            requestNotificationAccess();
            return;
        }

        if (enabled) {
            Log.d(TAG, "Stopping Service");
            CustomNotificationListener.killService();
            stopService(serviceIntent);
            enabled = false;
        } else if (!CustomNotificationListener.isRunning()) {
            startService(serviceIntent);
            enabled = true;
        }
        SharedPreferences.Editor preferencesEditor = getSharedPreferences(getString(R.string.saved_enabled), MODE_PRIVATE).edit();
        preferencesEditor.putBoolean(getString(R.string.saved_enabled), enabled);
        preferencesEditor.apply();
    }

    public void notificationAccess(View view) {
        requestNotificationAccess();
    }

    public void requestNotificationAccess() {
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    public void addAdditionalFilter(View view) {
        EditText et = view.getRootView().findViewById(R.id.editTextAddFilter);
        String newFilter = et.getText().toString();
        et.setText("");
        blocklist.addUserDefined(newFilter);
        // Only restart if a non-empty filter is added.
        if (!newFilter.isEmpty()) {
            Toast.makeText(this, "Added filter: " + newFilter, Toast.LENGTH_SHORT).show();
            // reload list by restarting service
            if (enabled) {
                onCheckboxClick(view);
                onCheckboxClick(view);
            }
        }
    }

    public void addCurrentAdToFilter(View view) {
        EditText et = findViewById(R.id.editTextAddFilter);
        et.setText(CustomNotificationListener.getCurrentTitle());
        this.addAdditionalFilter(findViewById(R.id.buttonAddFilter));
    }

    public void openAdditionalFilterListDialog(View view) {
        List<String> additionalFilters = blocklist.fetchUserDefined();
        ViewAdditionalFiltersDialogFragment viewAdditionalFiltersDialogFragment = ViewAdditionalFiltersDialogFragment.newInstance(additionalFilters.toArray(new String[additionalFilters.size()]));
        viewAdditionalFiltersDialogFragment.show(getFragmentManager(), "additionalFiltersDialog");
    }

    @Override
    public void onFilterClick(DialogInterface dialogInterface, int i) {
        List<String> additionalFilters = blocklist.fetchUserDefined();
        String filterToRemove = additionalFilters.get(i);
        blocklist.removeUserDefined(filterToRemove);
        Toast.makeText(this, "Deleted filter: " + filterToRemove, Toast.LENGTH_SHORT).show();
    }
}
