package com.swj.shiwujie.blind.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;

public class HomeFragment extends Fragment {

    private CardView cardConnectVolunteer;
    private CardView cardEmergencyHelp;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(root);
        return root;
    }

    private void initViews(View root) {
        cardConnectVolunteer = root.findViewById(R.id.cardConnectVolunteer);
        cardEmergencyHelp = root.findViewById(R.id.cardEmergencyHelp);

        // 设置连线志愿者按钮点击事件
        cardConnectVolunteer.setOnClickListener(v -> {
            // TODO: 实现连线志愿者功能
            Toast.makeText(requireContext(), "连线志愿者功能开发中...", Toast.LENGTH_SHORT).show();
        });

        // 设置紧急求助按钮点击事件
        cardEmergencyHelp.setOnClickListener(v -> {
            // TODO: 实现紧急求助功能
            Toast.makeText(requireContext(), "紧急求助功能开发中...", Toast.LENGTH_SHORT).show();
        });
    }
} 