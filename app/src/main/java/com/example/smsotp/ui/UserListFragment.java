package com.example.smsotp.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smsotp.databinding.FragmentUserListBinding;
import com.example.smsotp.databinding.UserListItemBinding;
import com.example.smsotp.entity.User;
import com.example.smsotp.viewmodel.MainViewModel;

public class UserListFragment extends Fragment {
    private static final String TAG = "UserListFragment";
    private FragmentUserListBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);
        MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        final Context context = getContext();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.list.setLayoutManager(new GridLayoutManager(context, 2));
        else binding.list.setLayoutManager(new LinearLayoutManager(context));
        binding.list.setHasFixedSize(true);

        final Adapter adapter = new Adapter();
        binding.list.setAdapter(adapter);
        viewModel.getUsers().observe(getViewLifecycleOwner(), adapter::submitList);

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
    private static class Adapter extends ListAdapter<User, ViewHolder> {
        private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(User oldItem, User newItem) {
                return oldItem.id == newItem.id;
            }

            @Override
            public boolean areContentsTheSame(User oldItem, User newItem) {
                return oldItem.username.equals(newItem.username);
            }
        };

        public Adapter() {
            super(DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    UserListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            // Here we set info for each individual item's info
            holder.binding.userName.setText(getItem(position).username);
            holder.userId = getItem(position).id;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        private int userId;
        private UserListItemBinding binding;

        public ViewHolder(UserListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(this::onClick);
        }

        private void onClick(View v) {
            if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                NavDirections action = MainFragmentDirections.actionMainFragmentToUserFragment(userId);
                Navigation.findNavController(v).navigate(action);
            }
        }
    }
}