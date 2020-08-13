package com.example.smsotp.ui.main;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentAddUserBinding;
import com.example.smsotp.entity.User;

import java.util.Objects;

public class AddUserFragment extends Fragment {
    private static final String TAG = "AddUserFragment";
    private FragmentAddUserBinding binding;
    private AppCompatActivity activity;

    public AddUserFragment() {
        super(R.layout.fragment_add_user);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddUserBinding.bind(view);
        activity = (AppCompatActivity) requireActivity();

        binding.createButton.setOnClickListener(v -> {
            String userText =
                    Objects.requireNonNull(binding.userField.getEditText()).getText().toString().trim();
            String passText =
                    Objects.requireNonNull(binding.passField.getEditText()).getText().toString().trim();
            if (validateInputs(userText, passText)) {
                new Thread(() -> {
                    try {
                        AppDatabase.getInstance(activity).userDao().insert(new User(userText, passText));
                        activity.onBackPressed();
                    } catch (SQLiteConstraintException ex) {
                        Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                        activity.runOnUiThread(() -> binding.userField.setError("Username already taken!"));
                    }
                }).start();
            }
        });
    }

    private boolean validateInputs(String userText, String passText) {
        boolean isUserValid = true;
        if (userText.isEmpty()) {
            binding.userField.setError("Field can't be empty");
            isUserValid = false;
        }

        boolean isPassValid = true;
        if (passText.isEmpty()) {
            binding.passField.setError("Field can't be empty");
            isPassValid = false;
        }

        if (!isUserValid || !isPassValid) return false;

        binding.userField.setError(null);
        binding.passField.setError(null);
        binding.userField.setErrorEnabled(false);
        binding.passField.setErrorEnabled(false);
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}