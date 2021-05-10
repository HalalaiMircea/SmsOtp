package com.example.smsotp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smsotp.R;
import com.example.smsotp.SettingsActivity;
import com.example.smsotp.databinding.FragmentMainBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainFragment extends Fragment {
    private static final String TAG = "SMSOTP_MainFragment";
    private FragmentMainBinding binding;
    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (position == 0) binding.fab.hide();
            else binding.fab.show();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentMainBinding.inflate(inflater, container, false);

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.include.toolbar);
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback);
        new TabLayoutMediator(binding.tabs, binding.viewPager,
                (tab, position) -> tab.setText(SectionsPagerAdapter.TAB_TITLES[position])
        ).attach();

        binding.fab.setOnClickListener(v -> {
            NavDirections action = MainFragmentDirections.actionMainFragmentToAddUserFragment();
            Navigation.findNavController(v).navigate(action);
        });
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_settings) {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);

            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        binding = null;
    }

    private static class SectionsPagerAdapter extends FragmentStateAdapter {
        @StringRes
        private static final int[] TAB_TITLES = {R.string.status, R.string.users, R.string.statistics};
        private final Fragment[] fragments;

        public SectionsPagerAdapter(Fragment frag) {
            super(frag);
            fragments = new Fragment[]{new StatusFragment(), new UserListFragment()};
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments[position];
        }

        @Override
        public int getItemCount() {
            return fragments.length;
        }
    }
}