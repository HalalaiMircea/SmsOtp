package com.example.smsotp.ui;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.InputType;
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
    private static final int PERMISSION_REQUEST = 12;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        EditTextPreference portPref = Objects.requireNonNull(findPreference(KEY_PREF_PORT));
        portPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        portPref.setOnPreferenceChangeListener(this::onServerPrefChanged);

        // This option is only available for API >= 22 as the Dual-SIM API was added in this version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            ListPreference subPref = Objects.requireNonNull(findPreference(KEY_PREF_SIM));
            subPref.setOnPreferenceClickListener(this::onSimPrefClicked);
            subPref.setOnPreferenceChangeListener(this::onServerPrefChanged);
        }
    }

    private boolean onServerPrefChanged(Preference preference, Object newValue) {
        boolean isRunning = Objects.requireNonNull(WebService.isRunning.getValue());
        if (isRunning) {
            Toast.makeText(getContext(), R.string.pref_server_warning, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private boolean onSimPrefClicked(Preference preference) {
        SubscriptionManager subManager = getSystemService(requireContext(), SubscriptionManager.class);
        if (checkSelfPermission(requireContext(), READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{READ_PHONE_STATE}, PERMISSION_REQUEST);
        } else {
            assert subManager != null;
            final List<SubscriptionInfo> activeSubInfoList = subManager.getActiveSubscriptionInfoList();
            CharSequence[] entries = new CharSequence[activeSubInfoList.size()];
            CharSequence[] entryValues = new CharSequence[activeSubInfoList.size()];
            for (int i = 0; i < activeSubInfoList.size(); i++) {
                SubscriptionInfo subInfo = activeSubInfoList.get(i);
                entries[i] = subInfo.getDisplayName();
                entryValues[i] = Integer.toString(subInfo.getSubscriptionId());
            }
            ((ListPreference) preference).setEntries(entries);
            ((ListPreference) preference).setEntryValues(entryValues);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            //TODO handle permission denial!
        }
    }
}