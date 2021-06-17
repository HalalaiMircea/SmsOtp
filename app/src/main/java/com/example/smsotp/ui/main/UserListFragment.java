package com.example.smsotp.ui.main;

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
import androidx.recyclerview.widget.*;

import com.example.smsotp.databinding.FragmentUserListBinding;
import com.example.smsotp.databinding.UserListItemBinding;
import com.example.smsotp.sql.User;
import com.example.smsotp.ui.MainFragmentDirections;
import com.example.smsotp.ui.main.UserListViewModel.UserItem;

public class UserListFragment extends Fragment {
    private static final String TAG = "UserListFragment";
    private FragmentUserListBinding binding;
    private UserListViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(UserListViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        final Context context = getContext();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            binding.list.setLayoutManager(new GridLayoutManager(context, 2));
        else binding.list.setLayoutManager(new LinearLayoutManager(context));
        binding.list.setHasFixedSize(true);

        final Adapter adapter = new Adapter();
        binding.list.setAdapter(adapter);
        viewModel.getUserItemsData().observe(getViewLifecycleOwner(), adapter::submitList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * {@link RecyclerView.Adapter} that can display a {@link User}.
     */
    private static class Adapter extends ListAdapter<UserItem, ViewHolder> {
        private static final DiffUtil.ItemCallback<UserItem> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<UserItem>() {
                    @Override
                    public boolean areItemsTheSame(UserItem oldItem, UserItem newItem) {
                        return oldItem.getId() == newItem.getId();
                    }

                    @Override
                    public boolean areContentsTheSame(UserItem oldItem, UserItem newItem) {
                        return oldItem.getUsername().equals(newItem.getUsername());
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
        public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
            // Here we set info for each individual item's info
            UserItem item = getItem(position);
            if (item != null) holder.bind(item);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final UserListItemBinding binding;
        private int userId;

        public ViewHolder(UserListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(this);
        }

        public void bind(UserItem item) {
            binding.userName.setText(item.getUsername());
            userId = item.getId();
        }

        @Override
        public void onClick(View v) {
            if (getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                NavDirections action = MainFragmentDirections.actionMainFragmentToUserFragment(userId);
                Navigation.findNavController(v).navigate(action);
            }
        }
    }
}