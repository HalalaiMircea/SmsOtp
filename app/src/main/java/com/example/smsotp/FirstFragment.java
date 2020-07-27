package com.example.smsotp;

import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        view.findViewById(R.id.button_first).setOnClickListener(view12 ->
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment));

        Button firstButton = view.findViewById(R.id.button_first);
        firstButton.setOnClickListener(v -> {
            String msg = "This is a test SMS.\nGreetings from APK n00b.";
            String[] phones = {"0773752028", "9999999999"};
//            sendSms("9999999999", msg);
            sendSmsToRecipients(phones, msg);
        });
    }

    public void sendSms(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getActivity().getApplicationContext(), "Message Sent!", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getActivity().getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    public void sendSmsToRecipients(String[] phones, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (String phoneNo : phones)
                smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getActivity().getApplicationContext(), "All Messages Sent!", Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Toast.makeText(getActivity().getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }
}