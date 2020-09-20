package com.example.smsotp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.BuildConfig;
import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentStatusBinding;
import com.example.smsotp.viewmodel.StatusViewModel;

import static android.Manifest.permission.SEND_SMS;

public class StatusFragment extends Fragment {
    private static final String TAG = "SMSOTP_StatusFragment";
    private FragmentStatusBinding binding;
    private StatusViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatusViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);

        viewModel.getServerState().observe(getViewLifecycleOwner(), binding.serverSwitch::setChecked);
        viewModel.getIpLiveData().observe(getViewLifecycleOwner(), binding.ipTextView::setText);
        binding.dbTextView.setText(viewModel.getDatabaseName());
        binding.portTextView.setText(viewModel.getServerPort());

        binding.serverSwitch.setOnCheckedChangeListener(this::onCheckedChanged);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Server started automatically in debug build variant!");
            binding.serverSwitch.setChecked(true);
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "Switch state changed to " + (isChecked ? "on" : "off"));
        Context context = requireContext();
        Intent serviceIntent = new Intent(context, WebService.class);
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    context.checkSelfPermission(SEND_SMS) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{SEND_SMS}, 10);
            } else {// If sdk < M or permission is granted
                context.startService(serviceIntent);
            }
        } else {
            context.stopService(serviceIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 10) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length == 0)
                return;
            if (permissions[0].equals(SEND_SMS) && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Permission " + permissions[0] + " denied by user!");
                Process.killProcess(Process.myPid());
            } else {
                Log.i(TAG, "Permission " + permissions[0] + " granted by user!");
                requireContext().startService(new Intent(getContext(), WebService.class));
            }
        }
    }
}