package com.example.smsotp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.R;
import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentStatusBinding;
import com.example.smsotp.viewmodel.StatusViewModel;
import com.google.android.material.snackbar.Snackbar;

import static android.Manifest.permission.SEND_SMS;
import static androidx.core.content.ContextCompat.checkSelfPermission;

public class StatusFragment extends Fragment {
    private static final String TAG = "SMSOTP_StatusFragment";
    private static final int PERMISSION_REQUEST = 10;
    private static final int APP_SETTINGS_REQUEST = 11;
    private FragmentStatusBinding binding;
    private StatusViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentStatusBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(StatusViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        viewModel.getServerState().observe(getViewLifecycleOwner(), binding.serverSwitch::setChecked);
        viewModel.getIpLiveData().observe(getViewLifecycleOwner(), binding.ipTextView::setText);
        viewModel.getServerPort().observe(getViewLifecycleOwner(), binding.portTextView::setText);

        binding.dbTextView.setText(viewModel.getDatabaseName());
        binding.serverSwitch.setOnCheckedChangeListener(this::onCheckedChanged);
        if (checkSelfPermission(requireContext(), SEND_SMS) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{SEND_SMS}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Context context = requireContext();
        Intent intent = new Intent(context, WebService.class);
        if (isChecked) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission " + permissions[0] + " granted by user!");
            } else {
                Log.e(TAG, "Permission " + permissions[0] + " denied by user!");
                binding.serverSwitch.setEnabled(false);
                showSnackbarRationale(requireParentFragment().requireView());
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == APP_SETTINGS_REQUEST) {
            if (checkSelfPermission(requireContext(), SEND_SMS) == PackageManager.PERMISSION_GRANTED)
                binding.serverSwitch.setEnabled(true);
            else
                requestPermissions(new String[]{SEND_SMS}, PERMISSION_REQUEST);
        }
    }

    private void showSnackbarRationale(View view) {
        Snackbar.make(view, R.string.sms_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.action_settings, v -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + requireContext().getPackageName()));
                    startActivityForResult(intent, APP_SETTINGS_REQUEST);
                })
                .show();
    }
}