package com.swj.shiwujie.volunteer.ui.community;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;
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
import com.swj.shiwujie.data.model.HelppostAddRequest;
import com.swj.shiwujie.data.model.HelppostVO;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    // 主视图组件
    private View mainView;
    
    // 导航标签
    private TextView tabCommunityActivity;
    private TextView tabCommunityHelp;
    private TextView tabMyCommunity;
    private View tabIndicator;
    
    // 内容区域
    private LinearLayout communityActivityLayout;
    private LinearLayout communityHelpLayout;
    private LinearLayout myCommunityLayout;
    
    // 社区活动页面组件
    private EditText activitySearchEditText;
    private Button activitySearchButton;
    private RecyclerView activityRecyclerView;
    
    // 社区求助页面组件
    private EditText helpSearchEditText;
    private Button helpSearchButton;
    private RecyclerView helpRecyclerView;
    private Button addHelpPostButton;
    
    // 我的社区页面组件
    private LinearLayout noJoinLayout;
    private LinearLayout joinedLayout;
    private TextView userNameText;
    private LinearLayout communitySelectionLayout;
    private EditText searchCommunityEditText;
    private Button searchCommunityButton;
    private LinearLayout communityInfoLayout;
    private TextView communityNameText;
    private TextView communityDescriptionText;
    private Button confirmJoinButton;
    private TextView communityTitleText;
    private TextView communityDescText;
    private Button helpPostButton;
    private Button activityButton;
    private TextView pendingHelpCountText;
    private TextView ongoingActivityCountText;
    private Button allHelpPostsButton;
    private Button allActivitiesButton;
    private Button myHelpButton;
    private Button myActivitiesButton;
    
    private ApiService apiService;
    private VolunteerVO currentUserInfo;
    private CommunityVO currentCommunity;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        try {
            // 初始化SharedPrefsUtil
            SharedPrefsUtil.init(getContext());
            
            // 初始化API服务
            apiService = RetrofitClient.getInstance().createService(ApiService.class);
            
            // 初始化VolunteerUserInfoManager
            VolunteerUserInfoManager.init();
            
            // 创建主视图（包含两种状态的布局）
            View mainView = inflater.inflate(R.layout.fragment_volunteer_community_main, container, false);
            
            // 初始化组件
            initComponents(mainView);
            
            // 检查用户是否已登录并获取基本信息
            String token = SharedPrefsUtil.getToken();
            Long userId = SharedPrefsUtil.getUserId();
            
            if (token != null && userId != null) {
                // 如果已登录，立即获取用户信息
                loadUserInfo();
            }
            
            return mainView;
        } catch (Exception e) {
            e.printStackTrace();
            // 如果出现异常，返回一个简单的视图
            return inflater.inflate(R.layout.fragment_volunteer_community_no_join, container, false);
        }
    }
    
    private void loadUserInfo() {
        try {
            VolunteerUserInfoManager.fetchUserInfo(getContext(), new VolunteerUserInfoManager.UserInfoCallback() {
                @Override
                public void onSuccess(VolunteerVO userInfo) {
                    try {
                        currentUserInfo = userInfo;
                        updateViewBasedOnUserStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "更新用户信息失败", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(getContext(), "获取用户信息失败: " + message, Toast.LENGTH_SHORT).show();
                    // 如果获取用户信息失败，默认显示未加入社区页面
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "初始化用户信息失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateViewBasedOnUserStatus() {
        if (currentUserInfo == null) {
            // 用户信息为空，保持当前视图
            return;
        }
        
        // 检查用户是否已加入社区
        if (currentUserInfo.getCommunityId() != null && currentUserInfo.getCommunityId() > 0) {
            // 已加入社区，自动切换到"我的社区"标签并显示社区详情
            showMyCommunityTab();
        } else {
            // 未加入社区，更新用户名
            if (userNameText != null && currentUserInfo.getName() != null) {
                userNameText.setText(currentUserInfo.getName());
            }
        }
    }
    
    private void loadCommunityInfo(Long communityId) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        apiService.getCommunityById("Bearer " + token, communityId).enqueue(new ApiCallback<CommunityVO>(getContext()) {
            @Override
            public void onSuccess(CommunityVO community) {
                currentCommunity = community;
                updateJoinedView(community);
                // 切换到已加入社区状态
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showJoinedState();
                    });
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "获取社区信息失败: " + message, Toast.LENGTH_SHORT).show();
                // 如果获取社区信息失败，保持当前视图
            }
        });
    }
    
    private void updateJoinedView(CommunityVO community) {
        // 更新社区信息
        if (communityTitleText != null) {
            communityTitleText.setText(community.getCommunityName());
        }
        if (communityDescText != null) {
            communityDescText.setText(community.getCommunityDescription());
        }
        
        // 加载统计数据
        loadCommunityStats();
    }
    
    private void loadCommunityStats() {
        // 这里可以调用API获取待响应求助帖数量和进行中活动数量
        // 暂时使用模拟数据
        if (pendingHelpCountText != null) {
            pendingHelpCountText.setText("5"); // 待响应求助帖数量
        }
        if (ongoingActivityCountText != null) {
            ongoingActivityCountText.setText("3"); // 进行中活动数量
        }
    }
    
    private void setupTabClickListeners() {
        if (tabCommunityActivity != null) {
            tabCommunityActivity.setOnClickListener(v -> showCommunityActivityTab());
        }
        
        if (tabCommunityHelp != null) {
            tabCommunityHelp.setOnClickListener(v -> showCommunityHelpTab());
        }
        
        if (tabMyCommunity != null) {
            tabMyCommunity.setOnClickListener(v -> showMyCommunityTab());
        }
    }
    
    private void showCommunityActivityTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, true);
        updateTabState(tabCommunityHelp, false);
        updateTabState(tabMyCommunity, false);
        
        // 移动指示器
        moveTabIndicator(0);
        
        // 显示对应内容
        if (communityActivityLayout != null) {
            communityActivityLayout.setVisibility(View.VISIBLE);
        }
        if (communityHelpLayout != null) {
            communityHelpLayout.setVisibility(View.GONE);
        }
        if (myCommunityLayout != null) {
            myCommunityLayout.setVisibility(View.GONE);
        }
    }
    
    private void showCommunityHelpTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, true);
        updateTabState(tabMyCommunity, false);
        
        // 移动指示器
        moveTabIndicator(1);
        
        // 显示对应内容
        if (communityActivityLayout != null) {
            communityActivityLayout.setVisibility(View.GONE);
        }
        if (communityHelpLayout != null) {
            communityHelpLayout.setVisibility(View.VISIBLE);
        }
        if (myCommunityLayout != null) {
            myCommunityLayout.setVisibility(View.GONE);
        }
    }
    
    private void showMyCommunityTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, false);
        updateTabState(tabMyCommunity, true);
        
        // 移动指示器
        moveTabIndicator(2);
        
        // 显示对应内容
        if (communityActivityLayout != null) {
            communityActivityLayout.setVisibility(View.GONE);
        }
        if (communityHelpLayout != null) {
            communityHelpLayout.setVisibility(View.GONE);
        }
        if (myCommunityLayout != null) {
            myCommunityLayout.setVisibility(View.VISIBLE);
        }
        
        // 检查用户是否已加入社区
        if (currentUserInfo != null && currentUserInfo.getCommunityId() != null && currentUserInfo.getCommunityId() > 0) {
            // 已加入社区，显示社区详情
            loadCommunityInfo(currentUserInfo.getCommunityId());
        } else {
            // 未加入社区，显示加入社区界面
            showNoJoinState();
        }
    }
    
    private void updateTabState(TextView tab, boolean isSelected) {
        if (tab != null) {
            if (isSelected) {
                tab.setTextColor(getResources().getColor(R.color.text_primary));
            } else {
                tab.setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
    }
    
    private void moveTabIndicator(int position) {
        if (tabIndicator != null) {
            // 计算指示器位置
            int tabWidth = getResources().getDisplayMetrics().widthPixels / 3;
            int margin = 16; // 16dp margin
            int indicatorPosition = position * tabWidth + margin;
            
            // 使用动画移动指示器
            tabIndicator.animate()
                .translationX(indicatorPosition)
                .setDuration(200)
                .start();
        }
    }
    
    private void showNoJoinState() {
        if (noJoinLayout != null && joinedLayout != null) {
            noJoinLayout.setVisibility(View.VISIBLE);
            joinedLayout.setVisibility(View.GONE);
        }
    }
    
    private void showJoinedState() {
        if (noJoinLayout != null && joinedLayout != null) {
            noJoinLayout.setVisibility(View.GONE);
            joinedLayout.setVisibility(View.VISIBLE);
        }
    }
    

    
    private void initComponents(View view) {
        try {
            // 初始化主视图组件
            mainView = view;
            
            // 初始化导航标签
            tabCommunityActivity = view.findViewById(R.id.tabCommunityActivity);
            tabCommunityHelp = view.findViewById(R.id.tabCommunityHelp);
            tabMyCommunity = view.findViewById(R.id.tabMyCommunity);
            tabIndicator = view.findViewById(R.id.tabIndicator);
            
            // 初始化内容区域
            communityActivityLayout = view.findViewById(R.id.communityActivityLayout);
            communityHelpLayout = view.findViewById(R.id.communityHelpLayout);
            myCommunityLayout = view.findViewById(R.id.myCommunityLayout);
            
            // 初始化社区活动页面组件
            activitySearchEditText = view.findViewById(R.id.activitySearchEditText);
            activitySearchButton = view.findViewById(R.id.activitySearchButton);
            activityRecyclerView = view.findViewById(R.id.activityRecyclerView);
            
            if (activitySearchButton != null) {
                activitySearchButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "搜索活动", Toast.LENGTH_SHORT).show();
                });
            }
            
            // 初始化社区求助页面组件
            helpSearchEditText = view.findViewById(R.id.helpSearchEditText);
            helpSearchButton = view.findViewById(R.id.helpSearchButton);
            helpRecyclerView = view.findViewById(R.id.helpRecyclerView);
            addHelpPostButton = view.findViewById(R.id.addHelpPostButton);
            
            if (helpSearchButton != null) {
                helpSearchButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "搜索求助", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (addHelpPostButton != null) {
                addHelpPostButton.setOnClickListener(v -> showHelpPostDialog());
            }
            
            // 初始化我的社区页面组件
            noJoinLayout = view.findViewById(R.id.noJoinLayout);
            joinedLayout = view.findViewById(R.id.joinedLayout);
            userNameText = view.findViewById(R.id.userNameText);
            
            if (currentUserInfo != null && userNameText != null) {
                userNameText.setText(currentUserInfo.getName());
            }
            
            communitySelectionLayout = view.findViewById(R.id.communitySelectionLayout);
            searchCommunityEditText = view.findViewById(R.id.searchCommunityEditText);
            searchCommunityButton = view.findViewById(R.id.searchCommunityButton);
            if (searchCommunityButton != null) {
                searchCommunityButton.setOnClickListener(v -> searchCommunity());
            }
            
            communityInfoLayout = view.findViewById(R.id.communityInfoLayout);
            communityNameText = view.findViewById(R.id.communityNameText);
            communityDescriptionText = view.findViewById(R.id.communityDescriptionText);
            confirmJoinButton = view.findViewById(R.id.confirmJoinButton);
            if (confirmJoinButton != null) {
                confirmJoinButton.setOnClickListener(v -> joinSelectedCommunity());
            }
            
            communityTitleText = view.findViewById(R.id.communityTitleText);
            communityDescText = view.findViewById(R.id.communityDescText);
            
            helpPostButton = view.findViewById(R.id.helpPostButton);
            activityButton = view.findViewById(R.id.activityButton);
            
            if (helpPostButton != null) {
                helpPostButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "跳转到求助帖管理页面", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (activityButton != null) {
                activityButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "跳转到活动管理页面", Toast.LENGTH_SHORT).show();
                });
            }
            
            pendingHelpCountText = view.findViewById(R.id.pendingHelpCountText);
            ongoingActivityCountText = view.findViewById(R.id.ongoingActivityCountText);
            
            allHelpPostsButton = view.findViewById(R.id.allHelpPostsButton);
            allActivitiesButton = view.findViewById(R.id.allActivitiesButton);
            myHelpButton = view.findViewById(R.id.myHelpButton);
            myActivitiesButton = view.findViewById(R.id.myActivitiesButton);
            
            if (allHelpPostsButton != null) {
                allHelpPostsButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "查看全部求助帖", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (allActivitiesButton != null) {
                allActivitiesButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "查看全部活动", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (myHelpButton != null) {
                myHelpButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "我的求助", Toast.LENGTH_SHORT).show();
                });
            }
            
            if (myActivitiesButton != null) {
                myActivitiesButton.setOnClickListener(v -> {
                    Toast.makeText(getContext(), "我的活动", Toast.LENGTH_SHORT).show();
                });
            }
            
            // 设置标签点击事件
            setupTabClickListeners();
            
            // 默认显示社区活动页面
            showCommunityActivityTab();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * 显示求助帖表单对话框
     */
    private void showHelpPostDialog() {
        // 检查用户是否已登录
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_help_post_form, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 获取对话框中的组件
        EditText helpContentEditText = dialogView.findViewById(R.id.helpContentEditText);
        EditText helpLocationEditText = dialogView.findViewById(R.id.helpLocationEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button submitButton = dialogView.findViewById(R.id.submitButton);
        
        // 设置取消按钮点击事件
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // 设置提交按钮点击事件
        submitButton.setOnClickListener(v -> {
            String helpContent = helpContentEditText.getText().toString().trim();
            String helpLocation = helpLocationEditText.getText().toString().trim();
            
            // 验证输入
            if (helpContent.isEmpty()) {
                Toast.makeText(getContext(), "请输入求助内容", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (helpLocation.isEmpty()) {
                Toast.makeText(getContext(), "请输入求助地点", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 提交求助帖
            submitHelpPost(helpContent, helpLocation, dialog);
        });
        
        dialog.show();
    }
    
    /**
     * 提交求助帖
     */
    private void submitHelpPost(String helpContent, String helpLocation, AlertDialog dialog) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建求助帖请求
        HelppostAddRequest request = new HelppostAddRequest(helpContent, helpLocation);
        
        // 调用API
        apiService.addHelppost("Bearer " + token, request).enqueue(new ApiCallback<HelppostVO>(getContext()) {
            @Override
            public void onSuccess(HelppostVO helppost) {
                Toast.makeText(getContext(), "求助帖发布成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                // 这里可以刷新求助列表
                // refreshHelpPostList();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "发布求助帖失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 