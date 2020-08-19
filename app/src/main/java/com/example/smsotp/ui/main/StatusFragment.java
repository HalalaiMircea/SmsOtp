package com.example.smsotp.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.SmsOtpService;
import com.example.smsotp.WebServer;
import com.example.smsotp.databinding.FragmentStatusBinding;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

public class StatusFragment extends Fragment {
    private static final String TAG = "SMSOTP_StatusFragment";
    private FragmentStatusBinding binding;

    public StatusFragment() {
        super(R.layout.fragment_status);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatusBinding.bind(view);

        binding.serverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = requireContext();
            Intent serviceIntent = new Intent(context, SmsOtpService.class);
            if (isChecked) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        });
        binding.serverSwitch.setChecked(SmsOtpService.isRunning());

        binding.ipTextView.setText(getWifiIPAddress());
        binding.dbTextView.setText(AppDatabase.getInstance(getContext()).getOpenHelper().getDatabaseName());
        binding.portTextView.setText(String.valueOf(WebServer.port));
    }

    private String getWifiIPAddress() {
        WifiManager wifiManager =
                (WifiManager) requireContext().getApplicationContext().getSystemService(WIFI_SERVICE);
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