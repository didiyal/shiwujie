package com.swj.shiwujie.volunteer.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_community, container, false);

        recyclerView = root.findViewById(R.id.activitiesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<CommunityActivity> activities = new ArrayList<>();
        // 使用正确的资源ID引用
        activities.add(new CommunityActivity(
                R.drawable.new1,  // 这里会自动找到new1.jpeg
                "报名中",
                "2025.03.30 至 2025.03.30",
                "公益助残送温暖",
                "社区公益"
        ));
        activities.add(new CommunityActivity(
                R.drawable.new4,  // 这里会自动找到new4.jpeg
                "报名中",
                "2025.04.01 至 2025.04.02",
                "维护盲道：为盲人出行保驾护航",
                "公益活动"
        ));
        activities.add(new CommunityActivity(
                R.drawable.new1,
                "报名中",
                "2025.04.15 至 2025.04.16",
                "助盲环保行动",
                "环保活动"
        ));
        activities.add(new CommunityActivity(
                R.drawable.new4,
                "进行中",
                "2025.04.20 至 2025.04.21",
                "盲人阅读推广活动",
                "文化活动"
        ));

        adapter = new CommunityAdapter(activities);
        recyclerView.setAdapter(adapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 