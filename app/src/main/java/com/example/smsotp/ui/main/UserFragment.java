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
    private Thread fetchDataThread;

    public UserFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            int userId = getArguments().getInt(ARG_ID);
            // BAD!!! VERY BAAAAAAAAAAAAD
            fetchDataThread = new Thread(() -> mUser =
                    AppDatabase.getInstance(getContext()).userDao().getById(userId));
            fetchDataThread.start();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserBinding.inflate(inflater, container, false);
        try {
            fetchDataThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        binding.idTextView.setText("Selected User ID: " + mUser.id);
        binding.nameTextView.setText("Username: " + mUser.username);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}