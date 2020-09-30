package com.example.smsotp.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.smsotp.R;
import com.example.smsotp.WebService;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String KEY_PREF_PORT = "port";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        EditTextPreference portPref = Objects.requireNonNull(findPreference(KEY_PREF_PORT));
        portPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        portPref.setOnPreferenceChangeListener((preference, newValue) -> {
            final boolean isRunning = Objects.requireNonNull(WebService.isRunning.getValue());
            if (isRunning) {
                Toast.makeText(SettingsFragment.this.getContext(), R.string.pref_port_warning,
                        Toast.LENGTH_LONG).show();
            } else return true;

            return false;
        });
    }
}