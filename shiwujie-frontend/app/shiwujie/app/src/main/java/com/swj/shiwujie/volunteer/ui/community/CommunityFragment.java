package com.swj.shiwujie.volunteer.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.VolunteerUserInfoManager;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.CommunityJoinRequest;
import com.swj.shiwujie.data.model.CommunityVO;
import com.swj.shiwujie.data.model.VolunteerVO;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private RecyclerView recyclerView;
    private CommunityAdapter adapter;
    private EditText searchEditText;
    private Button searchButton;
    
    // 未加入社区时的视图组件
    private View noJoinView;
    private TextView userNameText;

    private LinearLayout communitySelectionLayout;
    private EditText searchCommunityEditText;
    private Button searchCommunityButton;
    private LinearLayout communityInfoLayout;
    private TextView communityNameText;
    private TextView communityDescriptionText;
    private Button confirmJoinButton;
    
    // 已加入社区时的视图组件
    private View joinedView;
    
    private ApiService apiService;
    private VolunteerVO currentUserInfo;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        // 初始化SharedPrefsUtil
        SharedPrefsUtil.init(getContext());
        
        // 初始化API服务
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        
        // 初始化VolunteerUserInfoManager
        VolunteerUserInfoManager.init();
        
        // 先创建未加入社区的视图
        noJoinView = inflater.inflate(R.layout.fragment_volunteer_community_no_join, container, false);
        
        // 初始化未加入社区的组件
        initNoJoinComponents();
        
        // 创建已加入社区的视图
        joinedView = inflater.inflate(R.layout.fragment_volunteer_community, container, false);
        
        // 初始化已加入社区的组件
        initJoinedComponents();
        
        // 获取用户信息
        loadUserInfo();
        
        return noJoinView; // 默认返回未加入视图
    }
    
    private void loadUserInfo() {
        VolunteerUserInfoManager.fetchUserInfo(getContext(), new VolunteerUserInfoManager.UserInfoCallback() {
            @Override
            public void onSuccess(VolunteerVO userInfo) {
                currentUserInfo = userInfo;
                updateViewBasedOnUserStatus();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "获取用户信息失败: " + message, Toast.LENGTH_SHORT).show();
                // 如果获取用户信息失败，默认显示未加入社区页面
            }
        });
    }
    
    private void updateViewBasedOnUserStatus() {
        if (currentUserInfo == null) {
            // 用户信息为空，保持当前视图（默认是noJoinView）
            return;
        }
        
        // 检查用户是否已加入社区
        if (currentUserInfo.getCommunityId() != null && currentUserInfo.getCommunityId() > 0) {
            // 已加入社区，显示提示信息
            Toast.makeText(getContext(), "您已加入社区，请重新进入页面查看", Toast.LENGTH_LONG).show();
        } else {
            // 未加入社区，保持当前视图（默认是noJoinView）
            // 可以在这里更新用户名等信息
            if (userNameText != null && currentUserInfo.getName() != null) {
                userNameText.setText(currentUserInfo.getName());
            }
        }
    }
    
    private void initNoJoinComponents() {
        // 初始化用户名
        userNameText = noJoinView.findViewById(R.id.userNameText);
        if (currentUserInfo != null) {
            userNameText.setText(currentUserInfo.getName());
        }
        

        
        // 初始化社区选择组件
        communitySelectionLayout = noJoinView.findViewById(R.id.communitySelectionLayout);
        
        // 初始化搜索组件
        searchCommunityEditText = noJoinView.findViewById(R.id.searchCommunityEditText);
        searchCommunityButton = noJoinView.findViewById(R.id.searchCommunityButton);
        searchCommunityButton.setOnClickListener(v -> searchCommunity());
        
        // 初始化社区信息显示组件
        communityInfoLayout = noJoinView.findViewById(R.id.communityInfoLayout);
        communityNameText = noJoinView.findViewById(R.id.communityNameText);
        communityDescriptionText = noJoinView.findViewById(R.id.communityDescriptionText);
        confirmJoinButton = noJoinView.findViewById(R.id.confirmJoinButton);
        confirmJoinButton.setOnClickListener(v -> joinSelectedCommunity());
    }
    
    private void initJoinedComponents() {
        // 初始化搜索组件
        searchEditText = joinedView.findViewById(R.id.searchEditText);
        searchButton = joinedView.findViewById(R.id.searchButton);
        
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
        recyclerView = joinedView.findViewById(R.id.activitiesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // 创建活动数据
        List<CommunityActivity> activities = createActivityData();

        adapter = new CommunityAdapter(activities);
        recyclerView.setAdapter(adapter);
    }
    
    private void showCommunitySelection() {
        // 显示社区选择区域
        communitySelectionLayout.setVisibility(View.VISIBLE);
        
        // 这里可以加载可加入的社区列表
        // 暂时显示搜索功能
        Toast.makeText(getContext(), "请输入社区ID进行搜索", Toast.LENGTH_SHORT).show();
    }
    
    private void searchCommunity() {
        String searchText = searchCommunityEditText.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(getContext(), "请输入社区名称或ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 尝试解析为社区ID
        try {
            Long communityId = Long.parseLong(searchText);
            searchCommunityById(communityId);
        } catch (NumberFormatException e) {
            // 如果不是数字，按名称搜索（这里暂时显示提示）
            Toast.makeText(getContext(), "暂不支持按名称搜索，请输入社区ID", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void searchCommunityById(Long communityId) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiService.getCommunityById("Bearer " + token, communityId).enqueue(new ApiCallback<CommunityVO>(getContext()) {
            @Override
            public void onSuccess(CommunityVO community) {
                showCommunityInfo(community.getCommunityName(), community.getCommunityDescription());
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "查询社区失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showCommunityInfo(String name, String description) {
        communityNameText.setText(name);
        communityDescriptionText.setText(description);
        communityInfoLayout.setVisibility(View.VISIBLE);
    }
    
    private void joinSelectedCommunity() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 从搜索框获取社区ID
        String searchText = searchCommunityEditText.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(getContext(), "请先搜索要加入的社区", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Long communityId = Long.parseLong(searchText);
            CommunityJoinRequest request = new CommunityJoinRequest(communityId);
            
            apiService.volunteerJoinCommunity("Bearer " + token, request).enqueue(new ApiCallback<Boolean>(getContext()) {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(getContext(), "申请加入社区成功，等待审核", Toast.LENGTH_LONG).show();
                    // 清空搜索框和隐藏社区信息
                    searchCommunityEditText.setText("");
                    communityInfoLayout.setVisibility(View.GONE);
                    // 刷新用户信息
                    loadUserInfo();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "加入社区失败: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的社区ID", Toast.LENGTH_SHORT).show();
        }
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