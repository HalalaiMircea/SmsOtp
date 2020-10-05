package com.example.smsotp.viewmodel;

import android.app.Application;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.WebService;
import com.example.smsotp.ui.SettingsFragment;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class StatusViewModel extends AndroidViewModel {
    private static final String TAG = "StatusViewModel";
    // Data providers
    private final BroadcastReceiver receiver = new WifiStateReceiver();
    private final OnSharedPreferenceChangeListener prefsChangeListener;
    private final SharedPreferences sharedPrefs;
    // LiveData
    private MutableLiveData<String> wifiIpLiveData = new MutableLiveData<>();
    private LiveData<Boolean> serverStateLiveData = WebService.isRunning;
    private MutableLiveData<String> serverPort;
    private String databasePath;

    public StatusViewModel(@NonNull Application application) {
        super(application);
        getApplication().registerReceiver(receiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
        prefsChangeListener = (sharedPreferences, key) -> {
            if (key.equals(SettingsFragment.KEY_PREF_PORT)) {
                serverPort.setValue(sharedPreferences.getString(key, "8080"));
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefsChangeListener);
        serverPort = new MutableLiveData<>(sharedPrefs.getString(SettingsFragment.KEY_PREF_PORT, "8080"));
        databasePath = AppDatabase.getInstance(application).getOpenHelper().getWritableDatabase().getPath();
    }

    public LiveData<String> getIpLiveData() {
        return wifiIpLiveData;
    }

    public LiveData<Boolean> getServerState() {
        return serverStateLiveData;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public LiveData<String> getServerPort() {
        return serverPort;
    }

    @Override
    protected void onCleared() {
        getApplication().unregisterReceiver(receiver);
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefsChangeListener);
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        WifiManager wifiManager = ContextCompat.getSystemService(getApplication(), WifiManager.class);

        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = Objects.requireNonNull(
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
            );
            String ipAddress = "0.0.0.0";
            if (networkInfo.isConnected()) {
                int ipAddressInt = Integer.reverseBytes(wifiManager.getConnectionInfo().getIpAddress());
                byte[] ipByteArray = BigInteger.valueOf(ipAddressInt).toByteArray();
                try {
                    ipAddress = InetAddress.getByAddress(ipByteArray).getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
            wifiIpLiveData.setValue(ipAddress);
        }
    }
}
