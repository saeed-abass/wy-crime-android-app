package com.example.wycrimeapp.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.example.wycrimeapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("WY Crime Explorer");

        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        isAdmin = prefs.getBoolean("isAdmin", false);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        MainPagerAdapter adapter = new MainPagerAdapter(this, isAdmin);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Map"); break;
                case 1: tab.setText("List"); break;
            }
        }).attach();

        // NOTE: Search is now embedded on the Map screen, so there is no separate Search tab.
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Convenience helper: other screens can jump back to the Map tab.
    public void selectMapTab() {
        viewPager.setCurrentItem(0);  // 0 = Map tab
    }
    public void refreshListTab() {
        viewPager.setCurrentItem(1); // 1 = List tab
        ListFragment listFragment = (ListFragment) getSupportFragmentManager()
                .findFragmentByTag("f1");
        if (listFragment != null) listFragment.loadCrimes();
    }
}