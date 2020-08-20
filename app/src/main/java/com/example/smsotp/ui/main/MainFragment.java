package com.example.smsotp.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import static androidx.navigation.fragment.NavHostFragment.findNavController;

public class MainFragment extends Fragment {
    private static final String TAG = "SMSOTP_MainFragment";
    private FragmentMainBinding binding;
    private AppCompatActivity activity;

    public MainFragment() {
        super(R.layout.fragment_main);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.bind(view);
        activity = (AppCompatActivity) requireActivity();

        setupTabLayout();
        binding.fab.setOnClickListener(v -> findNavController(this).navigate(R.id.action_mainFragment_to_addUserFragment));
    }

    private void setupTabLayout() {
        // In Fragments we use ChildFragmentManager instead of SupportFragmentManager!!!!!!!!!!!!!!!
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(activity, getChildFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(adapter);
        binding.tabs.setupWithViewPager(viewPager);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the tabs of {@link TabLayout}.
     */
    public static class SectionsPagerAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {

        private static final String TAG = "SMSOTP_PagerAdapter";
        @StringRes
        private static final int[] TAB_TITLES = {R.string.status_tabitem, R.string.users_tabitem,
                R.string.statistics_tabitem};
        private final AppCompatActivity mContext;
        private FloatingActionButton fab;
        private Fragment[] fragments;

        public SectionsPagerAdapter(AppCompatActivity context, FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mContext = context;
            fab = context.findViewById(R.id.fab);
            fragments = new Fragment[]{new StatusFragment(), new UserListFragment()};
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            return fragments[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mContext.getResources().getString(TAB_TITLES[position]);
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                case 2:
                    fab.hide();
                    break;
                case 1:
                    fab.show();
                    break;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
}