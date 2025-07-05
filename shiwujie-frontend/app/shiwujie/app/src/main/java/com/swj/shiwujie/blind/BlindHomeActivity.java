package com.swj.shiwujie.blind;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.swj.shiwujie.R;
import com.swj.shiwujie.databinding.ActivityBlindHomeBinding;

public class BlindHomeActivity extends AppCompatActivity {
    private ActivityBlindHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlindHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 