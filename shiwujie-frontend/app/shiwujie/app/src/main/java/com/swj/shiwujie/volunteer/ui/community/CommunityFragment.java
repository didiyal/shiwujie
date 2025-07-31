package com.swj.shiwujie.volunteer.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
    private EditText searchEditText;
    private Button searchButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_community, container, false);

        // 初始化搜索组件
        searchEditText = root.findViewById(R.id.searchEditText);
        searchButton = root.findViewById(R.id.searchButton);
        
        // 设置搜索按钮点击事件
        searchButton.setOnClickListener(v -> {
            String searchText = searchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                performSearch(searchText);
            } else {
                Toast.makeText(getContext(), "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            }
        });

        // 初始化RecyclerView
        recyclerView = root.findViewById(R.id.activitiesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 创建活动数据
        List<CommunityActivity> activities = createActivityData();

        adapter = new CommunityAdapter(activities);
        recyclerView.setAdapter(adapter);

        return root;
    }

    private List<CommunityActivity> createActivityData() {
        List<CommunityActivity> activities = new ArrayList<>();
        
        activities.add(new CommunityActivity(
                R.drawable.new1,
                "报名中",
                "2025.03.30 至 2025.03.30",
                "公益助残送温暖",
                "社区公益"
        ));
        activities.add(new CommunityActivity(
                R.drawable.new4,
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
        activities.add(new CommunityActivity(
                R.drawable.new1,
                "已结束",
                "2025.04.25 至 2025.04.26",
                "无障碍设施检查活动",
                "设施检查"
        ));

        return activities;
    }

    private void performSearch(String searchText) {
        // 实现搜索逻辑
        Toast.makeText(getContext(), "搜索: " + searchText, Toast.LENGTH_SHORT).show();
        
        // 这里可以添加实际的搜索逻辑
        // 例如：过滤活动列表、调用API等
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 