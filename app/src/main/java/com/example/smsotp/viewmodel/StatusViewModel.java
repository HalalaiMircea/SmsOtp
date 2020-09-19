package com.example.smsotp.viewmodel;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.WebService;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class StatusViewModel extends AndroidViewModel {
    private static final String TAG = "StatusViewModel";
    private final WifiStateReceiver receiver = new WifiStateReceiver();
    private MutableLiveData<String> wifiIpLiveData = new MutableLiveData<>();
    private LiveData<Boolean> serverStateLiveData = WebService.getIsRunning();
    private String databaseName, serverPort;

    public StatusViewModel(@NonNull Application application) {
        super(application);
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getApplication().registerReceiver(receiver, filter);
        databaseName = AppDatabase.getInstance(application).getOpenHelper().getDatabaseName();
        serverPort = String.valueOf(WebService.WebServer.port);
    }

    public LiveData<String> getIpLiveData() {
        return wifiIpLiveData;
    }

    public LiveData<Boolean> getServerState() {
        return serverStateLiveData;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getServerPort() {
        return serverPort;
    }

    @Override
    protected void onCleared() {
        getApplication().unregisterReceiver(receiver);
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
