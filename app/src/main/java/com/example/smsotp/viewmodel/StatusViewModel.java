package com.example.smsotp.viewmodel;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
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
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class StatusViewModel extends AndroidViewModel {
    private static final String TAG = "StatusViewModel";
    // Data providers
    private final WifiStateReceiver receiver = new WifiStateReceiver();
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
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            assert networkInfo != null;
            wifiIpLiveData.setValue(networkInfo.isConnected() ? getWifiIPAddress() : "0.0.0.0");
        }

        private String getWifiIPAddress() {
            WifiManager wifiManager =
                    (WifiManager) getApplication().getApplicationContext().getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

            // Convert little-endian to big-endian if needed
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }
            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

            try {
                return InetAddress.getByAddress(ipByteArray).getHostAddress();
            } catch (UnknownHostException ex) {
                return "0.0.0.0";
            }
        }
    }
}
