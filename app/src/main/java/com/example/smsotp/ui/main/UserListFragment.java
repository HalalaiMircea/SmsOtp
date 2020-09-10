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
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsotp.AppDatabase;
import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentUserListBinding;
import com.example.smsotp.databinding.UserListItemBinding;
import com.example.smsotp.entity.User;

import java.util.List;

import static androidx.navigation.Navigation.findNavController;

public class UserListFragment extends Fragment {
    private static final String TAG = "UserListFragment";
    private FragmentUserListBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);

        final Context context = getContext();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.list.setLayoutManager(new GridLayoutManager(context, 2));
        else binding.list.setLayoutManager(new LinearLayoutManager(context));
        binding.list.setHasFixedSize(true);

        Thread thread = new Thread(() -> {
            List<User> result = AppDatabase.getInstance(context).userDao().getAll();
            requireActivity().runOnUiThread(() -> {
                Adapter adapter = new Adapter(result);
                binding.list.setAdapter(adapter);
            });
        });
        thread.start();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * {@link RecyclerView.Adapter} that can display a {@link User}.
     */
    @SuppressWarnings("NullableProblems")
    public static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final List<User> userDataList;

        public Adapter(List<User> users) {
            userDataList = users;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(UserListItemBinding.inflate(LayoutInflater.from(parent.getContext()),
                    parent, false));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            // Here we set info for each individual item's info
            holder.binding.userName.setText(userDataList.get(position).username);
            holder.setUserId(userDataList.get(position).id);
        }

        @Override
        public int getItemCount() {
            return userDataList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public UserListItemBinding binding;
            private int userId;

            public ViewHolder(UserListItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;

                binding.getRoot().setOnClickListener(v -> {
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                        Bundle args = new Bundle();
                        args.putInt(UserFragment.ARG_ID, userId);
                        findNavController(v).navigate(R.id.action_mainFragment_to_userFragment, args);
                    }
                });
            }

            public void setUserId(int userId) {
                this.userId = userId;
            }

            @Override
            public String toString() {
                return super.toString() + " '" + binding.userName.getText() + "'";
            }
        }
    }
}