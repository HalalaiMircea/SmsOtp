package com.example.smsotp.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;

import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentUserBinding;
import com.example.smsotp.viewmodel.UserViewModel;

import static androidx.navigation.Navigation.findNavController;

public class UserFragment extends Fragment {
    private static final String TAG = "UserFragment";
    private FragmentUserBinding binding;
    private UserViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int userId = UserFragmentArgs.fromBundle(requireArguments()).getUserId();
        UserViewModel.Factory factory = new UserViewModel.Factory(requireActivity().getApplication(), userId);
        viewModel = new ViewModelProvider(this, factory).get(UserViewModel.class);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentUserBinding.inflate(inflater, container, false);

        final Toolbar toolbar = binding.include.toolbar;
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setNavigationOnClickListener(v -> findNavController(v).navigateUp());

        viewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            String text = getString(R.string.user_id) + ": " + user.id;
            binding.idTextView.setText(text);
            toolbar.setTitle(user.username);
        });
        viewModel.getCommCount().observe(getViewLifecycleOwner(), count -> {
            String text = getString(R.string.d_comm_executed, count);
            binding.commCount.setText(text);
        });

        binding.clearComm.setOnClickListener(v -> {
            viewModel.clearCommands();
            Toast.makeText(getContext(), getString(R.string.cleared_commands), Toast.LENGTH_SHORT).show();
        });

        binding.editFab.setOnClickListener(v -> {
            NavDirections action = UserFragmentDirections.actionEditUser(viewModel.getUserId());
            findNavController(v).navigate(action);
        });
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_user, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_delete) {
            DialogFragment dialog = new DeleteDialog(viewModel);
            dialog.show(getParentFragmentManager(), "DeleteDialog");
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class DeleteDialog extends DialogFragment {
        private final UserViewModel viewModel;

        public DeleteDialog(UserViewModel viewModel) {
            this.viewModel = viewModel;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setMessage(R.string.del_user_confirm)
                    .setPositiveButton(R.string.delete_user, this::onPositive)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .create();
        }

        private void onPositive(DialogInterface dialog, int which) {
            viewModel.deleteUser();
            dialog.dismiss();
            requireActivity().onBackPressed();
        }
    }
}