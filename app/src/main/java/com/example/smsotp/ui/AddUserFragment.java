package com.example.smsotp.ui;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentAddUserBinding;
import com.example.smsotp.model.User;
import com.example.smsotp.viewmodel.UserViewModel;

import java.util.Objects;

import static androidx.navigation.Navigation.findNavController;

public class AddUserFragment extends Fragment {
    private static final String TAG = "AddUserFragment";
    private FragmentAddUserBinding binding;
    private UserViewModel viewModel;
    private AppCompatActivity activity;
    private EditText userEditText, passEditText;
    private InputMethodManager imm;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (AppCompatActivity) requireActivity();
        // We attach a view model to this Fragment instance when we come from editAction in nav_graph
        //(i.e. userId == -1)
        int userId = AddUserFragmentArgs.fromBundle(requireArguments()).getUserId();
        if (userId != -1) {
            viewModel = new ViewModelProvider(this).get(UserViewModel.class);
            viewModel.init(userId);
        }
        imm = ContextCompat.getSystemService(activity, InputMethodManager.class);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentAddUserBinding.inflate(inflater, container, false);

        userEditText = Objects.requireNonNull(binding.userField.getEditText());
        passEditText = Objects.requireNonNull(binding.passField.getEditText());

        Toolbar toolbar = binding.include.toolbar;
        activity.setSupportActionBar(toolbar);
        toolbar.setTitle(null);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);
        toolbar.setNavigationOnClickListener(this::navigateUp);

        // If we came here from action_editUser (i.e. if viewModel is attached)
        if (viewModel != null) {
            viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
                userEditText.setText(user.username);
                passEditText.setText(user.password);
            });
        } else {
            userEditText.requestFocus();
            userEditText.postDelayed(() -> imm.showSoftInput(userEditText, InputMethodManager.SHOW_FORCED),
                    100);
        }
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_add_user, menu);
        // We change item's title to make sense when updating user's data
        int titleRes = viewModel != null ? R.string.save_changes : R.string.create_user;
        menu.findItem(R.id.save_user).setTitle(titleRes);
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
                        if (viewModel == null) {
                            AppDatabase.getInstance(getContext())
                                    .userDao().insert(new User(userText, passText));
                        } else {// Else we entered from existing userEditAction
                            viewModel.updateUser(userText, passText);
                        }
                        navigateUp(requireView());
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

    private void navigateUp(View v) {
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        findNavController(v).navigateUp();
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