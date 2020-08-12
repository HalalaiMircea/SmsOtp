package com.example.smsotp.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.example.smsotp.databinding.UserListItemBinding;
import com.example.smsotp.ui.main.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem}.
 * TODO: Replace the implementation with code for your data type.
 */
@SuppressWarnings("NullableProblems")
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    private final List<DummyItem> mValues;

    public UserListAdapter(List<DummyItem> items) {
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
        holder.binding.userName.setText(mValues.get(position).content);
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