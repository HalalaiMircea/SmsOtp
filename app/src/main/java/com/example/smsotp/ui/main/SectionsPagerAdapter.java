package com.example.smsotp.ui.main;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.smsotp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter
        implements ViewPager.OnPageChangeListener {

    private static final String TAG = "SMSOTP_PagerAdapter";
    @StringRes
    private static final int[] TAB_TITLES = {R.string.status_tabitem, R.string.users_tabitem, R.string.statistics_tabitem};
    private final AppCompatActivity mContext;
    private FloatingActionButton fab;
    private Fragment[] fragments;

    public SectionsPagerAdapter(AppCompatActivity context, FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mContext = context;
        fab = context.findViewById(R.id.fab);
        fragments = new Fragment[]{StatusFragment.newInstance(),
                UserFragment.newInstance(1)};
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
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
        Log.d(TAG, "onPageSelected: Selected page=" + position);
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
//        Log.d(TAG, "onPageScrolled: ");
    }

    @Override
    public void onPageScrollStateChanged(int state) {
//        Log.d(TAG, "onPageScrollStateChanged: ");
    }
}