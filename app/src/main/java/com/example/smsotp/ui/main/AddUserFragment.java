package com.example.smsotp.ui.main;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

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
    private User user;
    private Integer userId;
    private EditText userEditText;
    private EditText passEditText;

    public AddUserFragment() {
        super(R.layout.fragment_add_user);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (AppCompatActivity) requireActivity();

        Bundle args = requireArguments();
        if (!args.isEmpty()) {
            userId = args.getInt(UserFragment.ARG_ID);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddUserBinding.bind(view);
        userEditText = Objects.requireNonNull(binding.userField.getEditText());
        passEditText = Objects.requireNonNull(binding.passField.getEditText());

        activity.setSupportActionBar(binding.include.toolbar);
        binding.include.toolbar.setTitle("");

        // If we got here from action_editUser (if userId arg was provided)
        if (userId != null) {
            Thread fetchThread = new Thread(() -> {
                user = AppDatabase.getInstance(activity).userDao().getById(userId);
                activity.runOnUiThread(() -> {
                    userEditText.setText(user.username);
                    passEditText.setText(user.password);
                });
            });
            fetchThread.start();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_user, menu);
        // We change item's title to make sense when updating user's data
        if (userId != null)
            menu.findItem(R.id.save_user).setTitle(R.string.save_changes);
        else menu.findItem(R.id.save_user).setTitle(R.string.create_user);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_user) {
            String userText = userEditText.getText().toString().trim();
            String passText = passEditText.getText().toString().trim();
            if (validateInputs(userText, passText)) {
                Thread thread = new Thread(() -> {
                    try {
                        // If we entered from main fragment add new user action
                        if (userId == null) {
                            user = new User(userText, passText);
                            AppDatabase.getInstance(activity).userDao().insert(user);
                        } else {// Else we entered from existing user edit action
                            user.username = userText;
                            user.password = passText;
                            AppDatabase.getInstance(activity).userDao().update(user);
                        }
                        activity.onBackPressed();
                    } catch (SQLiteConstraintException ex) {
                        Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                        activity.runOnUiThread(() -> binding.userField.setError("Username already taken!"));
                    }
                });
                thread.start();
            }
        }
        return super.onOptionsItemSelected(item);
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