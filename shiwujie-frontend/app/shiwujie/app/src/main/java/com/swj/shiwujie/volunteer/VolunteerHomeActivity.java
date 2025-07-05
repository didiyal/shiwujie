package com.swj.shiwujie.volunteer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 