package com.swj.shiwujie.blind.ui.community;

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

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.UserInfoManager;
import com.swj.shiwujie.data.model.BlindCommunityJoinRequest;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.data.model.CommunityVO;

public class CommunityFragment extends Fragment {
    
    private View noJoinView;
    private View joinedView;
    private View currentView;
    
    // 未加入社区时的UI组件
    private EditText searchCommunityEditText;
    private Button searchButton;
    private LinearLayout communityInfoCard;
    private TextView communityNameText;
    private TextView communityDescriptionText;
    private Button joinCommunityButton;

    
    // 已加入社区时的UI组件
    private TextView joinedCommunityNameText;
    private TextView joinedCommunityDescriptionText;
    private Button helpButton;
    private Button activityButton;
    private Button membersButton;
    
    private ApiService apiService;
    private BlindVO currentUserInfo;
    private CommunityVO selectedCommunity;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 初始化工具类
        SharedPrefsUtil.init(getContext());
        UserInfoManager.init();
        
        // 初始化API服务
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        
        // 加载用户信息
        loadUserInfo();
        
        // 根据用户状态显示不同的视图
        if (currentUserInfo != null && currentUserInfo.getCommunityId() != null) {
            // 已加入社区
            joinedView = inflater.inflate(R.layout.fragment_blind_community_joined, container, false);
            initJoinedView();
            return joinedView;
        } else {
            // 未加入社区
            noJoinView = inflater.inflate(R.layout.fragment_blind_community_no_join, container, false);
            initNoJoinView();
            return noJoinView;
        }
    }
    
    private void loadUserInfo() {
        UserInfoManager.fetchUserInfo(getContext(), new UserInfoManager.UserInfoCallback() {
            @Override
            public void onSuccess(BlindVO userInfo) {
                currentUserInfo = userInfo;
                updateViewBasedOnUserStatus();
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "获取用户信息失败: " + message, Toast.LENGTH_SHORT).show();
                // 如果获取失败，默认显示未加入状态
                if (getView() == null) {
                    View view = LayoutInflater.from(getContext()).inflate(R.layout.fragment_blind_community_no_join, (ViewGroup) getView(), false);
                    initNoJoinView();
                }
            }
        });
    }
    
    private void updateViewBasedOnUserStatus() {
        if (currentUserInfo != null && currentUserInfo.getCommunityId() != null) {
            // 已加入社区，显示Toast提示
            Toast.makeText(getContext(), "您已加入社区", Toast.LENGTH_SHORT).show();
        }
        // 保持当前视图不变，避免复杂的视图切换
    }
    
    private void initNoJoinView() {
        if (noJoinView == null) return;
        
        searchCommunityEditText = noJoinView.findViewById(R.id.searchCommunityEditText);
        searchButton = noJoinView.findViewById(R.id.searchButton);
        communityInfoCard = noJoinView.findViewById(R.id.communityInfoCard);
        communityNameText = noJoinView.findViewById(R.id.communityNameText);
        communityDescriptionText = noJoinView.findViewById(R.id.communityDescriptionText);
        joinCommunityButton = noJoinView.findViewById(R.id.joinCommunityButton);
        
        // 设置搜索按钮点击事件
        searchButton.setOnClickListener(v -> searchCommunity());
        
        // 设置加入社区按钮点击事件
        joinCommunityButton.setOnClickListener(v -> joinSelectedCommunity());
    }
    
    private void initJoinedView() {
        if (joinedView == null) return;
        
        joinedCommunityNameText = joinedView.findViewById(R.id.communityNameText);
        joinedCommunityDescriptionText = joinedView.findViewById(R.id.communityDescriptionText);
        helpButton = joinedView.findViewById(R.id.helpButton);
        activityButton = joinedView.findViewById(R.id.activityButton);
        membersButton = joinedView.findViewById(R.id.membersButton);
        
        // 设置功能按钮点击事件
        helpButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "求助功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        activityButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "社区活动功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        membersButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "社区成员功能开发中", Toast.LENGTH_SHORT).show();
        });
        
        // 加载社区信息
        loadCommunityInfo();
    }
    
    private void searchCommunity() {
        String searchText = searchCommunityEditText.getText().toString().trim();
        if (searchText.isEmpty()) {
            Toast.makeText(getContext(), "请输入社区ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Long communityId = Long.parseLong(searchText);
            searchCommunityById(communityId);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "输入格式错误，请输入纯数字的社区ID", Toast.LENGTH_SHORT).show();
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
                selectedCommunity = community;
                showCommunityInfo(community);
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "搜索社区失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showCommunityInfo(CommunityVO community) {
        if (communityInfoCard != null) {
            communityInfoCard.setVisibility(View.VISIBLE);
            communityNameText.setText(community.getCommunityName());
            communityDescriptionText.setText(community.getCommunityDescription());
        }
    }
    
    private void showCommunitySelection() {
        Toast.makeText(getContext(), "请先搜索社区", Toast.LENGTH_SHORT).show();
    }
    
    private void joinSelectedCommunity() {
        if (selectedCommunity == null) {
            Toast.makeText(getContext(), "请先搜索并选择社区", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BlindCommunityJoinRequest request = new BlindCommunityJoinRequest(selectedCommunity.getCommunityId());
        
        apiService.blindJoinCommunity("Bearer " + token, request).enqueue(new ApiCallback<Boolean>(getContext()) {
            @Override
            public void onSuccess(Boolean result) {
                Toast.makeText(getContext(), "加入社区成功", Toast.LENGTH_SHORT).show();
                // 刷新用户信息
                loadUserInfo();
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "加入社区失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadCommunityInfo() {
        if (currentUserInfo == null || currentUserInfo.getCommunityId() == null) {
            return;
        }
        
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            return;
        }
        
        apiService.getCommunityById("Bearer " + token, currentUserInfo.getCommunityId()).enqueue(new ApiCallback<CommunityVO>(getContext()) {
            @Override
            public void onSuccess(CommunityVO community) {
                if (joinedCommunityNameText != null) {
                    joinedCommunityNameText.setText(community.getCommunityName());
                }
                if (joinedCommunityDescriptionText != null) {
                    joinedCommunityDescriptionText.setText(community.getCommunityDescription());
                }
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "加载社区信息失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}