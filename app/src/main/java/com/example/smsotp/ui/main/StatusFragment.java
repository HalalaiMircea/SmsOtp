package com.example.smsotp.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentStatusBinding;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class StatusFragment extends Fragment {
    private static final String TAG = "SMSOTP_StatusFragment";
    private FragmentStatusBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);

        binding.serverSwitch.setChecked(WebService.isRunning());
        binding.serverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = requireContext();
            Intent serviceIntent = new Intent(context, WebService.class);
            if (isChecked) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        });

        binding.ipTextView.setText(getWifiIPAddress());
        binding.dbTextView.setText(AppDatabase.getInstance(getContext()).getOpenHelper().getDatabaseName());
        binding.portTextView.setText(String.valueOf(WebService.WebServer.port));
        return binding.getRoot();
    }

    private String getWifiIPAddress() {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext()
                .getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endian if needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Unable to get host address.");
            ipAddressString = "0.0.0.0";
        }

        return ipAddressString;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}