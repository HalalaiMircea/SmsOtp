package com.example.smsotp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentStatusBinding;
import com.example.smsotp.viewmodel.StatusViewModel;

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

        binding.serverSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = requireContext();
            Intent serviceIntent = new Intent(context, WebService.class);
            if (isChecked) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}