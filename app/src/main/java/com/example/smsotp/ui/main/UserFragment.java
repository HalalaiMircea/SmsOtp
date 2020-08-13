package com.example.smsotp.ui.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.databinding.FragmentUserBinding;
import com.example.smsotp.entity.User;

public class UserFragment extends Fragment {
    public static final String ARG_ID = "userId";
    private FragmentUserBinding binding;
    private User mUser;
    private int userId;

    public UserFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getInt(ARG_ID);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserBinding.inflate(inflater, container, false);

        Thread fetchDataThread = new Thread(() -> {
            mUser = AppDatabase.getInstance(getContext()).userDao().getById(userId);
            requireActivity().runOnUiThread(() -> {
                binding.idTextView.setText("Selected User ID: " + mUser.id);
                binding.nameTextView.setText("Username: " + mUser.username);
            });
        });
        fetchDataThread.start();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}