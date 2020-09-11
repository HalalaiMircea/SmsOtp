package com.example.smsotp.ui.main;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentAddUserBinding;
import com.example.smsotp.entity.User;
import com.example.smsotp.viewmodel.UserViewModel;

import java.util.Objects;

public class AddUserFragment extends Fragment {
    private static final String TAG = "AddUserFragment";
    private FragmentAddUserBinding binding;
    private UserViewModel viewModel;
    private AppCompatActivity activity;
    private Integer userId;
    private EditText userEditText;
    private EditText passEditText;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentAddUserBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(UserViewModel.class);

        userEditText = Objects.requireNonNull(binding.userField.getEditText());
        passEditText = Objects.requireNonNull(binding.passField.getEditText());

        Toolbar toolbar = binding.include.toolbar;
        activity.setSupportActionBar(toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());

        // If we came here from action_editUser (if userId arg was provided)
        if (userId != null) {
            viewModel.getUser(userId).observe(getViewLifecycleOwner(), user -> {
                userEditText.setText(user.username);
                passEditText.setText(user.password);
            });
        }
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_user, menu);
        // We change item's title to make sense when updating user's data
        int titleRes = userId != null ? R.string.save_changes : R.string.create_user;
        menu.findItem(R.id.save_user).setTitle(titleRes);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.save_user) {
            String userText = userEditText.getText().toString().trim();
            String passText = passEditText.getText().toString().trim();
            if (validateInputs(userText, passText)) {
                new Thread(() -> {
                    try {
                        // If we entered from main fragment through addNewUserAction
                        if (userId == null) {
                            viewModel.insertUser(new User(userText, passText));
                        } else {// Else we entered from existing userEditAction
                            viewModel.updateUser(userText, passText);
                        }
                        activity.onBackPressed();
                    } catch (SQLiteConstraintException ex) {
                        Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
                        activity.runOnUiThread(() -> binding.userField.setError("Username already taken!"));
                    }
                }).start();
            }
            return true;
        }
        return false;
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
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}