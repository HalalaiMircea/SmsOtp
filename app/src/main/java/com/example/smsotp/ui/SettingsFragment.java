package com.example.smsotp.ui;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.smsotp.R;
import com.example.smsotp.WebService;

import java.util.List;
import java.util.Objects;

import static android.Manifest.permission.READ_PHONE_STATE;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static androidx.core.content.ContextCompat.getSystemService;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String KEY_PREF_PORT = "port";
    public static final String KEY_PREF_SIM = "sim";
    private static final String TAG = "SettingsFragment";
    private static final int PERMISSION_REQUEST = 12;
    private ListPreference subscriptionPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        EditTextPreference portPref = Objects.requireNonNull(findPreference(KEY_PREF_PORT));
        portPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        portPref.setOnPreferenceChangeListener(this::onServerPrefChanged);

        // This option is only available for API >= 22 as the Dual-SIM API was added in this version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            subscriptionPref = Objects.requireNonNull(findPreference(KEY_PREF_SIM));
            subscriptionPref.setOnPreferenceChangeListener(this::onServerPrefChanged);
            populateSubscriptionList();
        }
    }

    private boolean onServerPrefChanged(Preference preference, Object newValue) {
        Boolean isRunning = WebService.isRunning.getValue();
        if (isRunning != null && isRunning) {
            Toast.makeText(getContext(), R.string.pref_server_warning, Toast.LENGTH_LONG).show();
            return false;
        }
        if (preference.getKey().equals(KEY_PREF_PORT)) {
            int port = Integer.parseInt((String) newValue);
            if (port < 0 || port > 0xFFFF) {
                Toast.makeText(getContext(), R.string.pref_port_warning, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void populateSubscriptionList() {
        if (checkSelfPermission(requireContext(), READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            SubscriptionManager subManager = getSystemService(requireContext(), SubscriptionManager.class);
            assert subManager != null;
            List<SubscriptionInfo> subscriptionsInfo = subManager.getActiveSubscriptionInfoList();
            CharSequence[] entries = new CharSequence[subscriptionsInfo.size()];
            CharSequence[] entryValues = new CharSequence[subscriptionsInfo.size()];
            for (int i = 0; i < subscriptionsInfo.size(); i++) {
                SubscriptionInfo subInfo = subscriptionsInfo.get(i);
                entries[i] = subInfo.getDisplayName();
                entryValues[i] = Integer.toString(subInfo.getSubscriptionId());
            }
            subscriptionPref.setEntries(entries);
            subscriptionPref.setEntryValues(entryValues);
        } else if (shouldShowRequestPermissionRationale(READ_PHONE_STATE)) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.settings_rationale)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        requestPermissions(new String[]{READ_PHONE_STATE}, PERMISSION_REQUEST);
                    })
                    .show();
            subscriptionPref.setEnabled(false);
        } else {
            requestPermissions(new String[]{READ_PHONE_STATE}, PERMISSION_REQUEST);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission " + permissions[0] + " granted by user!");
                populateSubscriptionList();
                subscriptionPref.setEnabled(true);
            } else {
                Log.e(TAG, "Permission " + permissions[0] + " denied by user!");
                subscriptionPref.setEnabled(false);
            }
        }
    }
}