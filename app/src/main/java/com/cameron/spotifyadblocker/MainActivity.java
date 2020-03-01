package com.cameron.spotifyadblocker;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cameron.spotifyadblocker.detection.Blocklist;
import com.cameron.spotifyadblocker.detection.CustomNotificationListener;
import com.cameron.spotifyadblocker.detection.DetectionMethods;
import com.cameron.spotifyadblocker.detection.DeviceAccessException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ViewAdditionalFiltersDialogFragment.ViewAdditionalFiltersDialogListener {
    private static final String TAG = "MainActivity";
    private boolean enabled;
    private Blocklist blocklist;
    private DetectionMethods method;
    private Spinner methodSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        blocklist = new Blocklist(this);

        method = DetectionMethods.LISTENER;
        initSpinner();
    }

    private void initSpinner() {
        methodSpinner = findViewById(R.id.spinner_method_selection);

        final List<String> methodNames = new ArrayList<>();
        for (DetectionMethods available : DetectionMethods.values()) {
            methodNames.add(available.name());
        }

        methodSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, methodNames));
        methodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String name;
                try {
                    name = methodNames.get(i);
                } catch (IndexOutOfBoundsException e) {
                    Log.w(TAG, String.format("Unable to retrieve name for index %s", i), e);
                    return;
                }

                try {
                    method = DetectionMethods.valueOf(name);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, String.format("Unable to find detection method %s", name), e);
                }

                TextView notice = findViewById(R.id.requirement_notice);
                notice.setText(method.getNoticeId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restoreCheckboxState();
    }

    private void setState(boolean enabled) {
        this.enabled = enabled;
        SharedPreferences.Editor preferencesEditor = getEnablingPreferences().edit();
        preferencesEditor.putBoolean(getString(R.string.saved_enabled), enabled);
        preferencesEditor.apply();
    }

    private boolean getState() {
        SharedPreferences preferences = getEnablingPreferences();
        return preferences.getBoolean(getString(R.string.saved_enabled), false);
    }

    private SharedPreferences getEnablingPreferences() {
        return getSharedPreferences(getString(R.string.saved_enabled), MODE_PRIVATE);
    }

    private void restoreCheckboxState() {
        enabled = getState();
        CheckBox enabledCheckbox = (CheckBox) findViewById(R.id.checkBox);
        enabledCheckbox.setChecked(enabled);
        if (enabled) {
            runServiceWithAccess(method);
        }
    }

    public void onCheckboxClick(View view) {
        Log.d(TAG, String.format("Enabled? %s", enabled));
        if (enabled) {
            Log.d(TAG, "Stopping Service");
            method.stop(this);
            setState(false);
        } else {
            Log.d(TAG, "Starting Service");
            runServiceWithAccess(method);
        }
        // Disable spinner if service is started.
        methodSpinner.setEnabled(!getState());
    }

    /**
     * Try to start a service listening to the Spotify notifications, handling permission
     * accesses if necessary.
     *
     * @param method The {@link DetectionMethods} to use as service.
     */
    private void runServiceWithAccess(DetectionMethods method) {
        try {
            method.start(this);
            setState(true);
        } catch (DeviceAccessException e) {
            Toast.makeText(this, "Access denied", Toast.LENGTH_LONG).show();
            requestNotificationAccess(method);
        }
    }

    public void notificationAccess(View view) {
        requestNotificationAccess(method);
    }

    public void requestNotificationAccess(DetectionMethods method) {
        method.requestAccess(this);
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
