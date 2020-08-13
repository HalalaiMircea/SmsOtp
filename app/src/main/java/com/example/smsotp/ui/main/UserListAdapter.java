package com.example.smsotp.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.smsotp.databinding.UserListItemBinding;
import com.example.smsotp.entity.User;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link User}.
 */
@SuppressWarnings("NullableProblems")
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private final List<User> mValues;

    public UserListAdapter(List<User> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(UserListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent,
                false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // Here we set info for each item's views, like username, user type and others
//        holder.binding.userName.setText(mValues.get(position).username);
        holder.binding.userName.setText(mValues.get(position).username);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public UserListItemBinding binding;

        public ViewHolder(UserListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + binding.userName.getText() + "'";
        }
    }
}