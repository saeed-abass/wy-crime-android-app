package com.example.wycrimeapp.view;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private final boolean isAdmin;

    public MainPagerAdapter(FragmentActivity fa, boolean isAdmin) {
        super(fa);
        this.isAdmin = isAdmin;
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MapFragment();
            case 1:
                return new ListFragment();
            default:
                return new MapFragment();
        }
    }

    @Override
    public int getItemCount() {
        // Admin functions are now surfaced directly on the List screen
        // (add crime + re-import) to avoid a separate Admin page.
        return 2;
    }
}