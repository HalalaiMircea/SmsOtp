package com.example.smsotp.ui.main;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.databinding.FragmentUserListBinding;
import com.example.smsotp.entity.User;

import java.util.List;

/**
 * A fragment representing a list of Users.
 */
public class UserListFragment extends Fragment {

    private FragmentUserListBinding binding;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public UserListFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);

        final Context context = getContext();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.list.setLayoutManager(new GridLayoutManager(context, 2));
        } else {
            binding.list.setLayoutManager(new LinearLayoutManager(context));
        }
        binding.list.setHasFixedSize(true);

        Thread thread = new Thread(() -> {
            List<User> result = AppDatabase.getInstance(context).userDao().getAll();
            UserListAdapter adapter = new UserListAdapter(result);
            binding.list.setAdapter(adapter);
        });
        thread.start();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}