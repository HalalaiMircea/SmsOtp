package com.example.smsotp.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.entity.User;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class AddUserFragment extends Fragment {
    private TextInputLayout userField;
    private TextInputLayout passField;
    private AppCompatActivity activity;

    public AddUserFragment() {
        super(R.layout.fragment_add_user);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        activity = (AppCompatActivity) requireActivity();
        Button createButton = view.findViewById(R.id.createButton);
        userField = view.findViewById(R.id.userField);
        passField = view.findViewById(R.id.passField);

        createButton.setOnClickListener(v -> {
            if (validateInputs()) {
                new Thread(() -> AppDatabase.getInstance(activity).userDao().insert(
                        new User(Objects.requireNonNull(userField.getEditText()).getText().toString(),
                                Objects.requireNonNull(passField.getEditText()).getText().toString()))
                ).start();
                activity.onBackPressed();
            }
        });
    }

    private boolean validateInputs() {
        String userText = Objects.requireNonNull(userField.getEditText()).getText().toString().trim();
        String passText = Objects.requireNonNull(passField.getEditText()).getText().toString().trim();

        boolean isUserValid = true;
        if (userText.isEmpty()) {
            userField.setError("Field can't be empty");
            isUserValid = false;
        }

        boolean isPassValid = true;
        if (passText.isEmpty()) {
            passField.setError("Field can't be empty");
            isPassValid = false;
        }

        if (!isUserValid || !isPassValid) return false;

        userField.setError(null);
        passField.setError(null);
        userField.setErrorEnabled(false);
        passField.setErrorEnabled(false);
        return true;
    }
}