package com.example.smsotp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smsotp.R;
import com.example.smsotp.SettingsActivity;
import com.example.smsotp.WebService;
import com.example.smsotp.databinding.FragmentMainBinding;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainFragment extends Fragment {
    private static final String TAG = "SMSOTP_MainFragment";
    private FragmentMainBinding binding;
    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                case 2:
                    binding.fab.hide();
                    break;
                case 1:
                    binding.fab.show();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        setHasOptionsMenu(true);
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.include.toolbar);

        int fragCount;
        if (WebService.isRunning.getValue() != null) {
            fragCount = WebService.isRunning.getValue() ? 3 : 2;
        } else fragCount = 2;

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(this, fragCount);
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback);
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabs, binding.viewPager, (tab, position) ->
                tab.setText(SectionsPagerAdapter.TAB_TITLES[position])
        ).attach();
        WebService.isRunning.observe(getViewLifecycleOwner(), adapter::changeCount);

        binding.fab.setOnClickListener(v -> {
            NavDirections action = MainFragmentDirections.actionMainFragmentToAddUserFragment();
            Navigation.findNavController(v).navigate(action);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        binding = null;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
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

    private static class SectionsPagerAdapter extends FragmentStateAdapter {
        @StringRes
        private static final int[] TAB_TITLES = {R.string.status, R.string.users, R.string.web_app};
        private final Fragment[] fragments = {new StatusFragment(), new UserListFragment(), new WebAppFragment()};
        private int fragCount;

        public SectionsPagerAdapter(Fragment parent, int initialCount) {
            super(parent);
            fragCount = initialCount;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments[position];
        }

        @Override
        public int getItemCount() {
            return fragCount;
        }

        public void changeCount(boolean keepLast) {
            final int lastFragCount = fragCount;
            if (keepLast) {
                fragCount = 3;
                if (lastFragCount != fragCount) notifyItemInserted(2);
            } else {
                fragCount = 2;
                if (lastFragCount != fragCount) notifyItemRemoved(2);
            }
        }
    }
}