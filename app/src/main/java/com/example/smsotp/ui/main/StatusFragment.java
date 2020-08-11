package com.example.smsotp.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.SmsOtpService;
import com.example.smsotp.WebServer;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import static android.content.Context.WIFI_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatusFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "SMSOTP_StatusFragment";

    public StatusFragment() {
        // Required public no-args constructor
        super(R.layout.fragment_status);
    }

    public static StatusFragment newInstance() {
        StatusFragment fragment = new StatusFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView ipTextView = view.findViewById(R.id.ipTextView);
        TextView dbTextView = view.findViewById(R.id.dbTextView);
        TextView portTextView = view.findViewById(R.id.portTextView);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch serverSwitch = view.findViewById(R.id.serverSwitch);

        serverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requireContext().startService(new Intent(getContext(), SmsOtpService.class));
            } else {
                requireContext().stopService(new Intent(getContext(), SmsOtpService.class));
            }
        });

        ipTextView.setText(getWifiIPAddress());
        dbTextView.setText(AppDatabase.getInstance(getContext()).getOpenHelper().getDatabaseName());
        portTextView.setText(String.valueOf(WebServer.port));
    }

    private String getWifiIPAddress() {
        WifiManager wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
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
}