package com.example.smsotp.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;

import com.example.smsotp.R;
import com.example.smsotp.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {
    private static final String TAG = "SMSOTP_MainFragment";
    private FragmentMainBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstance) {
        binding = FragmentMainBinding.inflate(inflater, container, false);

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getContext(), getChildFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) binding.fab.hide();
                else binding.fab.show();
            }
        });
        binding.tabs.setupWithViewPager(viewPager);
        binding.fab.setOnClickListener(v -> {
            NavDirections action = MainFragmentDirections.actionMainFragmentToAddUserFragment();
            Navigation.findNavController(v).navigate(action);
        });
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.viewPager.clearOnPageChangeListeners();
        binding = null;
    }

    private static class SectionsPagerAdapter extends FragmentPagerAdapter {
        @StringRes
        private static final int[] TAB_TITLES = {R.string.status, R.string.users, R.string.statistics};
        private final Context mContext;
        private Fragment[] fragments;

        public SectionsPagerAdapter(Context context, FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            mContext = context;
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
    }
}