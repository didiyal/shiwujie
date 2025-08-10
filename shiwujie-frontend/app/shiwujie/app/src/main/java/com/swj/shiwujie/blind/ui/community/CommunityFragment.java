package com.swj.shiwujie.blind.ui.community;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
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

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.utils.UserInfoManager;
import com.swj.shiwujie.data.model.BlindCommunityJoinRequest;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.data.model.CommunityVO;
import com.swj.shiwujie.data.model.HelppostAddRequest;
import com.swj.shiwujie.data.model.HelppostVO;
import com.swj.shiwujie.data.model.ActivityVO;
import com.swj.shiwujie.data.model.ActivitysignVO;
import com.swj.shiwujie.data.model.ActivitySignAddRequest;
import com.swj.shiwujie.data.model.Page;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityFragment extends Fragment {
    
    // 主视图
    private View mainView;
    
    // 导航标签
    private TextView tabCommunityActivity;
    private TextView tabCommunityHelp;
    private TextView tabMyCommunity;
    private View tabIndicator;
    
    // 内容布局
    private LinearLayout communityActivityLayout;
    private LinearLayout communityHelpLayout;
    private LinearLayout myCommunityLayout;
    
    // 社区活动页面组件
    private EditText activitySearchEditText;
    private Button activitySearchButton;
    private androidx.recyclerview.widget.RecyclerView activityRecyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout activitySwipeRefreshLayout;
    private ActivityAdapter activityAdapter;
    
    // 社区求助页面组件
    private EditText helpSearchEditText;
    private Button helpSearchButton;
    private Button addHelpPostButton;
    private androidx.recyclerview.widget.RecyclerView helpRecyclerView;
    private HelppostAdapter helppostAdapter;
    
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
    private Button myActivitiesButton;

    
    // 新增的社区信息组件
    private TextView communityIdText;
    private TextView joinTimeText;
    private TextView communityStatusText;
    
    private ApiService apiService;
    private BlindVO currentUserInfo;
    private CommunityVO selectedCommunity;
    private CommunityVO currentCommunity;
    
    // 存储求助帖ID列表
    private List<Long> helppostIds = new ArrayList<>();
    
    // 活动相关变量
    private List<ActivityVO> activityList = new ArrayList<>();
    private Long currentPage = 1L;
    private Long pageSize = 10L;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    
    // SharedPreferences键名
    private static final String PREF_HELPPOST_IDS = "helppost_ids";
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            // 初始化SharedPrefsUtil
        SharedPrefsUtil.init(getContext());
        
        // 初始化API服务
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
            
            // 初始化UserInfoManager
            UserInfoManager.init();
            
            // 加载已保存的求助帖ID
            loadSavedHelppostIds();
            
            // 创建主视图
            mainView = inflater.inflate(R.layout.fragment_blind_community_main, container, false);
            
            // 初始化组件
            initComponents(mainView);
            
            // 设置标签点击监听器
            setupTabClickListeners();
            
            // 加载用户信息
            loadUserInfo();
            
            return mainView;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "页面加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    
    private void loadUserInfo() {
        try {
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
                    updateViewBasedOnUserStatus();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "初始化用户信息失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateViewBasedOnUserStatus() {
        if (currentUserInfo == null) {
            // 用户信息为空，显示我的社区页面的未加入状态
            showMyCommunityTab();
            showNoJoinState();
            return;
        }
        
        // 检查用户是否已加入社区
        if (currentUserInfo.getCommunityId() != null && currentUserInfo.getCommunityId() > 0) {
            // 已加入社区，自动切换到"我的社区"标签并显示社区详情
            showMyCommunityTab();
            loadCommunityInfo(currentUserInfo.getCommunityId());
        } else {
            // 未加入社区，显示我的社区页面的未加入状态
            showMyCommunityTab();
            showNoJoinState();
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
        
        // 更新社区详细信息
        if (communityIdText != null && community.getCommunityId() != null) {
            communityIdText.setText(String.valueOf(community.getCommunityId()));
        }
        
        if (joinTimeText != null) {
            // 这里可以根据实际需求显示加入时间
            // 暂时显示当前时间，实际应该从用户信息中获取
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            joinTimeText.setText(sdf.format(new java.util.Date()));
        }
        
        if (communityStatusText != null) {
            communityStatusText.setText("已加入");
            communityStatusText.setTextColor(getResources().getColor(R.color.success_green));
        }
        
        // 加载统计数据
        loadStatistics();
    }
    
    private void loadStatistics() {
        // TODO: 加载用户的求助和活动统计数据
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
    
    private void showMyCommunityTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, false);
        updateTabState(tabMyCommunity, true);
        
        // 移动指示器 - 我的社区现在是第一个位置
        moveTabIndicator(0);
        
        // 显示对应内容
        communityActivityLayout.setVisibility(View.GONE);
        communityHelpLayout.setVisibility(View.GONE);
        myCommunityLayout.setVisibility(View.VISIBLE);
    }

    private void showCommunityActivityTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, true);
        updateTabState(tabCommunityHelp, false);
        updateTabState(tabMyCommunity, false);
        
        // 移动指示器 - 社区活动现在是第二个位置
        moveTabIndicator(1);
        
        // 显示对应内容
        communityActivityLayout.setVisibility(View.VISIBLE);
        communityHelpLayout.setVisibility(View.GONE);
        myCommunityLayout.setVisibility(View.GONE);
        
        // 加载活动列表
        loadActivityList();
    }

    private void showCommunityHelpTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, true);
        updateTabState(tabMyCommunity, false);
        
        // 移动指示器 - 社区求助现在是第三个位置
        moveTabIndicator(2);
        
        // 显示对应内容
        communityActivityLayout.setVisibility(View.GONE);
        communityHelpLayout.setVisibility(View.VISIBLE);
        myCommunityLayout.setVisibility(View.GONE);
        
        // 加载求助帖列表
        loadHelppostList();
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
            int indicatorWidth = 60; // dp
            int marginStart = 16; // dp
            int tabWidth = (getResources().getDisplayMetrics().widthPixels - 32) / 3; // 减去左右边距
            
            int targetX = marginStart + (tabWidth * position) + (tabWidth - indicatorWidth) / 2;
            
            // 使用动画移动指示器
            tabIndicator.animate()
                    .translationX(targetX)
                    .setDuration(200)
                    .start();
        }
    }
    
    private void showNoJoinState() {
        if (noJoinLayout != null) {
            noJoinLayout.setVisibility(View.VISIBLE);
        }
        if (joinedLayout != null) {
            joinedLayout.setVisibility(View.GONE);
        }
        
        // 更新用户名
        if (userNameText != null && currentUserInfo != null && currentUserInfo.getName() != null) {
            userNameText.setText("欢迎，" + currentUserInfo.getName());
        }
    }
    
    private void showJoinedState() {
        if (noJoinLayout != null) {
            noJoinLayout.setVisibility(View.GONE);
        }
        if (joinedLayout != null) {
            joinedLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void initComponents(View view) {
        // 初始化导航标签
        tabCommunityActivity = view.findViewById(R.id.tabCommunityActivity);
        tabCommunityHelp = view.findViewById(R.id.tabCommunityHelp);
        tabMyCommunity = view.findViewById(R.id.tabMyCommunity);
        tabIndicator = view.findViewById(R.id.tabIndicator);
        
        // 初始化内容布局
        communityActivityLayout = view.findViewById(R.id.communityActivityLayout);
        communityHelpLayout = view.findViewById(R.id.communityHelpLayout);
        myCommunityLayout = view.findViewById(R.id.myCommunityLayout);
        
        // 初始化社区活动页面组件
        activitySearchEditText = view.findViewById(R.id.activitySearchEditText);
        activitySearchButton = view.findViewById(R.id.activitySearchButton);
        activityRecyclerView = view.findViewById(R.id.activityRecyclerView);
        activitySwipeRefreshLayout = view.findViewById(R.id.activitySwipeRefreshLayout);
        
        // 初始化社区求助页面组件
        helpSearchEditText = view.findViewById(R.id.helpSearchEditText);
        helpSearchButton = view.findViewById(R.id.helpSearchButton);
        addHelpPostButton = view.findViewById(R.id.addHelpPostButton);
        helpRecyclerView = view.findViewById(R.id.helpRecyclerView);
        
        // 初始化求助帖列表
        helppostAdapter = new HelppostAdapter();
        if (helpRecyclerView != null) {
            helpRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            helpRecyclerView.setAdapter(helppostAdapter);
        }
        
        // 初始化活动列表
        activityAdapter = new ActivityAdapter(getContext(), activityList);
        if (activityRecyclerView != null) {
            activityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            activityRecyclerView.setAdapter(activityAdapter);
            
            // 设置活动列表点击事件
            activityAdapter.setOnActivityClickListener(new ActivityAdapter.OnActivityClickListener() {
                @Override
                public void onActivityDetailClick(ActivityVO activity) {
                    showActivityDetailDialog(activity);
                }
                
                @Override
                public void onActivitySignUpClick(ActivityVO activity) {
                    signUpForActivity(activity);
                }
            });
        }
        
        // 设置下拉刷新
        if (activitySwipeRefreshLayout != null) {
            activitySwipeRefreshLayout.setOnRefreshListener(() -> {
                // 重置活动列表并重新加载
                resetActivityList();
                loadActivityList();
            });
        }
        
        // 初始化我的社区页面组件
        noJoinLayout = view.findViewById(R.id.noJoinLayout);
        joinedLayout = view.findViewById(R.id.joinedLayout);
        userNameText = view.findViewById(R.id.userNameText);
        communitySelectionLayout = view.findViewById(R.id.communitySelectionLayout);
        searchCommunityEditText = view.findViewById(R.id.searchCommunityEditText);
        searchCommunityButton = view.findViewById(R.id.searchCommunityButton);
        communityInfoLayout = view.findViewById(R.id.communityInfoLayout);
        communityNameText = view.findViewById(R.id.communityNameText);
        communityDescriptionText = view.findViewById(R.id.communityDescriptionText);
        confirmJoinButton = view.findViewById(R.id.confirmJoinButton);
        communityTitleText = view.findViewById(R.id.communityTitleText);
        communityDescText = view.findViewById(R.id.communityDescText);
        helpPostButton = view.findViewById(R.id.helpPostButton);
        activityButton = view.findViewById(R.id.activityButton);
        myActivitiesButton = view.findViewById(R.id.myActivitiesButton);

        
        // 新增的社区信息组件
        communityIdText = view.findViewById(R.id.communityIdText);
        joinTimeText = view.findViewById(R.id.joinTimeText);
        communityStatusText = view.findViewById(R.id.communityStatusText);
        
        // 设置按钮点击事件
        setupButtonListeners();
    }
    
    private void setupButtonListeners() {
        // 社区活动页面按钮
        if (activitySearchButton != null) {
            activitySearchButton.setOnClickListener(v -> searchActivity());
        }
        
        // 社区求助页面按钮
        if (helpSearchButton != null) {
            helpSearchButton.setOnClickListener(v -> searchHelp());
        }
        if (addHelpPostButton != null) {
            addHelpPostButton.setOnClickListener(v -> showHelpPostDialog());
        }
        
        // 未加入社区时的按钮
        if (searchCommunityButton != null) {
            searchCommunityButton.setOnClickListener(v -> searchCommunity());
        }
        if (confirmJoinButton != null) {
            confirmJoinButton.setOnClickListener(v -> joinSelectedCommunity());
        }
        
        // 已加入社区时的按钮
        if (helpPostButton != null) {
            helpPostButton.setOnClickListener(v -> {
                // 跳转到社区求助页面
                showCommunityHelpTab();
            });
        }
        
        if (activityButton != null) {
            activityButton.setOnClickListener(v -> {
                // 跳转到社区活动页面
                showCommunityActivityTab();
            });
        }

        // 参与的活动按钮
        if (myActivitiesButton != null) {
            myActivitiesButton.setOnClickListener(v -> {
                loadMyActivities();
            });
        }
        

    }
    
    private void searchActivity() {
        if (activitySearchEditText != null) {
            String searchText = activitySearchEditText.getText().toString().trim();
            if (searchText.isEmpty()) {
                // 如果搜索框为空，重新加载所有活动
                resetActivityList();
                loadActivityList();
            } else {
                // 根据搜索关键词过滤活动
                filterActivitiesByKeyword(searchText);
            }
        }
    }
    
    private void searchHelp() {
        if (helpSearchEditText != null) {
            String searchText = helpSearchEditText.getText().toString().trim();
            Toast.makeText(getContext(), "搜索求助: " + searchText, Toast.LENGTH_SHORT).show();
            // TODO: 实现求助搜索功能
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
                
                // 保存求助帖ID - 这是查询求助信息的标识
                if (helppost.getHelppostId() != null) {
                    helppostIds.add(helppost.getHelppostId());
                    Toast.makeText(getContext(), "求助帖ID已保存: " + helppost.getHelppostId(), Toast.LENGTH_SHORT).show();
                    saveHelppostIds(); // 保存到SharedPreferences
                }
                
                // 刷新求助帖列表
                loadHelppostList();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "发布求助帖失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 加载求助帖信息
     */
    private void loadHelppostList() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUserInfo == null) {
            Toast.makeText(getContext(), "用户信息获取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否有社区信息
        if (currentCommunity == null) {
            Toast.makeText(getContext(), "请先加入社区", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 调用API获取盲人发布的求助帖列表
        // 使用blindId参数查询盲人的求助帖
        apiService.getBlindHelpposts("Bearer " + token, currentUserInfo.getBlindId(), currentCommunity.getCommunityId(), 1L, 50L)
                .enqueue(new ApiCallback<Page<HelppostVO>>(getContext()) {
                    @Override
                    public void onSuccess(Page<HelppostVO> page) {
                        if (page != null && page.getRecords() != null) {
                            List<HelppostVO> helpposts = page.getRecords();
                            if (helppostAdapter != null) {
                                helppostAdapter.setHelppostList(helpposts);
                                helppostAdapter.setOnHelppostClickListener(helppostItem -> {
                                    // 点击求助帖时的处理
                                    Toast.makeText(getContext(), "查看求助帖详情: " + helppostItem.getHelppostId(), Toast.LENGTH_SHORT).show();
                                });
                                
                                if (helpposts.isEmpty()) {
                                    Toast.makeText(getContext(), "您还没有发布过任何求助", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "成功加载 " + helpposts.size() + " 条求助帖", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            if (helppostAdapter != null) {
                                helppostAdapter.setHelppostList(new ArrayList<>());
                            }
                            Toast.makeText(getContext(), "您还没有发布过任何求助", Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), "获取求助帖失败: " + message, Toast.LENGTH_SHORT).show();
                        if (helppostAdapter != null) {
                            helppostAdapter.setHelppostList(new ArrayList<>());
                        }
                    }
                });
    }
    
    private void searchCommunity() {
        if (searchCommunityEditText != null) {
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
        if (communityInfoLayout != null) {
            communityInfoLayout.setVisibility(View.VISIBLE);
            if (communityNameText != null) {
            communityNameText.setText(community.getCommunityName());
            }
            if (communityDescriptionText != null) {
            communityDescriptionText.setText(community.getCommunityDescription());
            }
        }
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
                Toast.makeText(getContext(), "已发送加入申请等待审核", Toast.LENGTH_SHORT).show();
                // 刷新用户信息
                loadUserInfo();
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "加入社区失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载已保存的求助帖ID
     */
    private void loadSavedHelppostIds() {
        try {
            String savedIds = SharedPrefsUtil.getString(PREF_HELPPOST_IDS, "");
            if (!savedIds.isEmpty()) {
                helppostIds.clear();
                String[] idStrings = savedIds.split(",");
                for (String idString : idStrings) {
                    if (!idString.trim().isEmpty()) {
                        helppostIds.add(Long.parseLong(idString.trim()));
                    }
                }
                Log.d("CommunityFragment", "加载了 " + helppostIds.size() + " 个已保存的求助帖ID");
            }
        } catch (Exception e) {
            Log.e("CommunityFragment", "加载求助帖ID失败", e);
        }
    }
    
    /**
     * 保存求助帖ID到SharedPreferences
     */
    private void saveHelppostIds() {
        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < helppostIds.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(helppostIds.get(i));
            }
            SharedPrefsUtil.putString(PREF_HELPPOST_IDS, sb.toString());
            Log.d("CommunityFragment", "保存了 " + helppostIds.size() + " 个求助帖ID");
        } catch (Exception e) {
            Log.e("CommunityFragment", "保存求助帖ID失败", e);
        }
    }

    /**
     * 加载活动列表
     */
    private void loadActivityList() {
        if (isLoading || !hasMoreData) {
            return;
        }
        
        // 检查用户是否已登录
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否有当前社区
        if (currentCommunity == null || currentCommunity.getCommunityId() == null) {

            return;
        }
        
        isLoading = true;
        
        // 调用API获取活动列表
        apiService.getActivityList(
                "Bearer " + token,
                currentCommunity.getCommunityId(),

                currentPage,
                pageSize,
                null // 不按状态过滤
        ).enqueue(new ApiCallback<Page<ActivityVO>>(getContext()) {
            @Override
            public void onSuccess(Page<ActivityVO> page) {
                isLoading = false;
                
                // 停止下拉刷新
                if (activitySwipeRefreshLayout != null) {
                    activitySwipeRefreshLayout.setRefreshing(false);
                }
                
                if (page != null && page.getRecords() != null) {
                    List<ActivityVO> newActivities = page.getRecords();
                    
                    if (currentPage == 1) {
                        // 第一页，清空列表
                        activityList.clear();
                    }
                    
                    activityList.addAll(newActivities);
                    activityAdapter.updateData(activityList);
                    
                    // 检查是否还有更多数据
                    hasMoreData = page.getCurrent() < page.getPages();
                    if (hasMoreData) {
                        currentPage++;
                    }
                    
                    // 显示加载结果
                    if (activityList.isEmpty()) {
                        Toast.makeText(getContext(), "暂无活动", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "加载了 " + newActivities.size() + " 个活动", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "加载活动列表失败", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String message) {
                isLoading = false;
                
                // 停止下拉刷新
                if (activitySwipeRefreshLayout != null) {
                    activitySwipeRefreshLayout.setRefreshing(false);
                }
                
                Toast.makeText(getContext(), "加载活动列表失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 重置活动列表
     */
    private void resetActivityList() {
        activityList.clear();
        currentPage = 1L;
        hasMoreData = true;
        activityAdapter.updateData(activityList);
    }
    
    /**
     * 根据关键词过滤活动
     */
    private void filterActivitiesByKeyword(String keyword) {
        List<ActivityVO> filteredList = new ArrayList<>();
        
        for (ActivityVO activity : activityList) {
            if (activity.getActivityName() != null && activity.getActivityName().toLowerCase().contains(keyword.toLowerCase()) ||
                activity.getActivityContent() != null && activity.getActivityContent().toLowerCase().contains(keyword.toLowerCase()) ||
                activity.getActivityLocation() != null && activity.getActivityLocation().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(activity);
            }
        }
        
        activityAdapter.updateData(filteredList);
        
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "未找到匹配的活动", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "找到 " + filteredList.size() + " 个匹配的活动", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示活动详情对话框
     */
    private void showActivityDetailDialog(ActivityVO activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_activity_detail, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // 获取对话框中的组件
        TextView activityNameText = dialogView.findViewById(R.id.dialog_activity_name);
        TextView activityContentText = dialogView.findViewById(R.id.dialog_activity_content);
        TextView activityLocationText = dialogView.findViewById(R.id.dialog_activity_location);
        TextView activityTimeText = dialogView.findViewById(R.id.dialog_activity_time);
        TextView activityStatusText = dialogView.findViewById(R.id.dialog_activity_status);
        TextView maxParticipantsText = dialogView.findViewById(R.id.dialog_max_participants);
        Button closeButton = dialogView.findViewById(R.id.dialog_btn_close);
        Button signUpButton = dialogView.findViewById(R.id.dialog_btn_sign_up);
        
        // 设置活动信息
        activityNameText.setText(activity.getActivityName());
        activityContentText.setText(activity.getActivityContent());
        activityLocationText.setText("地点: " + activity.getActivityLocation());
        
        // 格式化时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String timeText = "";
        if (activity.getStartTime() != null) {
            timeText += "开始: " + sdf.format(activity.getStartTime());
        }
        if (activity.getEndTime() != null) {
            timeText += "\n结束: " + sdf.format(activity.getEndTime());
        }
        activityTimeText.setText(timeText);
        
        // 设置状态
        activityStatusText.setText("状态: " + getActivityStatusText(activity.getActivityStatus()));
        
        // 设置人数限制
        if (activity.getMaxParticipants() != null) {
            maxParticipantsText.setText("人数限制: " + activity.getMaxParticipants() + "人");
        } else {
            maxParticipantsText.setText("人数限制: 不限");
        }
        
        // 设置按钮点击事件
        closeButton.setOnClickListener(v -> dialog.dismiss());
        signUpButton.setOnClickListener(v -> {
            dialog.dismiss();
            signUpForActivity(activity);
        });
        
        dialog.show();
    }
    
    /**
     * 活动报名
     */
    private void signUpForActivity(ActivityVO activity) {
        // 检查活动状态
        if ("2".equals(activity.getActivityStatus()) || "3".equals(activity.getActivityStatus())) {
            Toast.makeText(getContext(), "该活动已结束或已取消，无法报名", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查用户是否已登录
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查用户信息
        if (currentUserInfo == null || currentUserInfo.getBlindId() == null) {
            Toast.makeText(getContext(), "用户信息获取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("活动报名")
                .setMessage("确定要报名参加活动「" + activity.getActivityName() + "」吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 调用活动报名API
                    submitActivitySignUp(activity);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 提交活动报名
     */
    private void submitActivitySignUp(ActivityVO activity) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建活动报名请求
        ActivitySignAddRequest request = new ActivitySignAddRequest();
        request.setActivityId(activity.getActivityId());
        request.setBlindId(currentUserInfo.getBlindId());
        
        // 调用API
        apiService.addActivitySign("Bearer " + token, request).enqueue(new ApiCallback<Boolean>(getContext()) {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    Toast.makeText(getContext(), "活动报名成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "活动报名失败", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String message) {
                Toast.makeText(getContext(), "活动报名失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 获取活动状态文本
     */
    private String getActivityStatusText(String status) {
        switch (status) {
            case "0": return "未开始";
            case "1": return "进行中";
            case "2": return "已结束";
            case "3": return "已取消";
            default: return "未知";
        }
    }

    /**
     * 加载盲人参与的活动列表
     */
    private void loadMyActivities() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(getContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserInfo == null) {
            Toast.makeText(getContext(), "用户信息获取失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否有社区信息
        if (currentCommunity == null) {
            return;
        }

        // 调用API获取盲人参与的活动列表
        // 使用blindId参数查询盲人参与的活动
        apiService.getActivitySignListByVolunteer("Bearer " + token, 1L, 50L, null)
                .enqueue(new ApiCallback<Page<ActivitysignVO>>(getContext()) {
                    @Override
                    public void onSuccess(Page<ActivitysignVO> page) {
                        if (page != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
                            // 过滤出盲人参与的活动
                            List<ActivitysignVO> blindActivities = new ArrayList<>();
                            for (ActivitysignVO activitysign : page.getRecords()) {
                                if (activitysign.getBlindId() != null && 
                                    activitysign.getBlindId().equals(currentUserInfo.getBlindId())) {
                                    blindActivities.add(activitysign);
                                }
                            }
                            
                            if (!blindActivities.isEmpty()) {
                                // 显示我的活动列表
                                showMyActivitiesDialogFromSignList(blindActivities);
                            } else {
                                Toast.makeText(getContext(), "您还没有参与过任何活动", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "您还没有参与过任何活动", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), "获取我的活动失败: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 显示我的活动对话框
     */
    private void showMyActivitiesDialog(List<ActivityVO> activities) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("我的活动");

        // 创建RecyclerView来显示活动列表
        androidx.recyclerview.widget.RecyclerView recyclerView = new androidx.recyclerview.widget.RecyclerView(getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 创建适配器
        ActivityAdapter adapter = new ActivityAdapter(getContext(), activities);
        adapter.setOnActivityClickListener(new ActivityAdapter.OnActivityClickListener() {
            @Override
            public void onActivityDetailClick(ActivityVO activity) {
                showActivityDetailDialog(activity);
            }

            @Override
            public void onActivitySignUpClick(ActivityVO activity) {
                // 在我的活动中，不需要报名功能
                Toast.makeText(getContext(), "您已参与此活动", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // 设置对话框内容
        builder.setView(recyclerView);
        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 显示我的活动对话框（从活动报名列表）
     */
    private void showMyActivitiesDialogFromSignList(List<ActivitysignVO> activitysigns) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("我的活动");

        // 创建丰富的文本显示
        StringBuilder content = new StringBuilder();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        
        for (int i = 0; i < activitysigns.size(); i++) {
            ActivitysignVO activitysign = activitysigns.get(i);
            content.append("📌 活动 ").append(i + 1).append("\n");
            content.append("━━━━━━━━━━━━━━━━━━━━━━\n");
            content.append("🏷️ 活动ID: ").append(activitysign.getActivityId()).append("\n");
            
            if (activitysign.getSignUpTime() != null) {
                content.append("📝 报名时间: ").append(sdf.format(activitysign.getSignUpTime())).append("\n");
            }
            
            if (activitysign.getCheckInTime() != null) {
                content.append("✅ 签到时间: ").append(sdf.format(activitysign.getCheckInTime())).append("\n");
            } else {
                content.append("⏳ 签到状态: 未签到\n");
            }
            
            if (activitysign.getCheckOutTime() != null) {
                content.append("🏁 签退时间: ").append(sdf.format(activitysign.getCheckOutTime())).append("\n");
            } else if (activitysign.getCheckInTime() != null) {
                content.append("🔄 签退状态: 进行中\n");
            }
            
            if (i < activitysigns.size() - 1) {
                content.append("\n");
            }
        }

        builder.setMessage(content.toString());
        builder.setPositiveButton("关闭", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}