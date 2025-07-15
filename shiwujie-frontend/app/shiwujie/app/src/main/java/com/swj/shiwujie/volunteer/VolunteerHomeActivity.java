package com.swj.shiwujie.volunteer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.swj.shiwujie.R;
import com.swj.shiwujie.databinding.ActivityVolunteerHomeBinding;

public class VolunteerHomeActivity extends AppCompatActivity {
    private ActivityVolunteerHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVolunteerHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupViews();
    }

    private void setupViews() {
        // 配置底部导航栏
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_community,
                R.id.navigation_message, R.id.navigation_family,
                R.id.navigation_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 