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
    private User mUser;
    private Integer userId;

    public AddUserFragment() {
        super(R.layout.fragment_add_user);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        if (!args.isEmpty()) {
            userId = args.getInt(UserFragment.ARG_ID);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddUserBinding.bind(view);
        activity = (AppCompatActivity) requireActivity();

        // If we got here from action_editUser (if userId arg was provided)
        if (userId != null) {
            Thread fetchThread = new Thread(() -> {
                mUser = AppDatabase.getInstance(getContext()).userDao().getById(userId);
                requireActivity().runOnUiThread(() -> {
                    binding.userField.getEditText().setText(mUser.username);
                    //TODO Should I put password there? IDK DOESN'T MATTER
                    binding.passField.getEditText().setText(mUser.password);
                });
            });
            fetchThread.start();
            // We change button's text to make sense when updating user's data
            binding.createButton.setText(R.string.save_changes);
        }

        if (userId == null)
            binding.createButton.setOnClickListener(this::onCreateClick);
        else
            binding.createButton.setOnClickListener(this::onEditClick);
    }

    private void onCreateClick(View view) {
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
    }

    private void onEditClick(View view) {
        String userText =
                Objects.requireNonNull(binding.userField.getEditText()).getText().toString().trim();
        String passText =
                Objects.requireNonNull(binding.passField.getEditText()).getText().toString().trim();
        if (validateInputs(userText, passText)) {
            // If input is valid, we update this user's data
            mUser.username = userText;
            mUser.password = passText;
            new Thread(() -> {
                try {
                    AppDatabase.getInstance(activity).userDao().update(mUser);
                    activity.onBackPressed();
                } catch (SQLiteConstraintException ex) {
                    Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                    activity.runOnUiThread(() -> binding.userField.setError("Username already taken!"));
                }
            }).start();
        }
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
        /*binding.userField.setErrorEnabled(false);
        binding.passField.setErrorEnabled(false);*/
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}