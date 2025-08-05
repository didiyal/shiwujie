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

import java.util.ArrayList;
import java.util.List;

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
    private TextView myHelpCountText;
    private TextView myActivityCountText;
    private Button allHelpPostsButton;
    private Button allActivitiesButton;
    private Button myHelpButton;
    private Button myActivitiesButton;
    
    private ApiService apiService;
    private BlindVO currentUserInfo;
    private CommunityVO selectedCommunity;
    private CommunityVO currentCommunity;
    
    // 存储求助帖ID列表
    private List<Long> helppostIds = new ArrayList<>();
    
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
            // 用户信息为空，保持当前视图
            return;
        }
        
        // 检查用户是否已加入社区
        if (currentUserInfo.getCommunityId() != null && currentUserInfo.getCommunityId() > 0) {
            // 已加入社区，自动切换到"我的社区"标签并显示社区详情
            showMyCommunityTab();
            loadCommunityInfo(currentUserInfo.getCommunityId());
        } else {
            // 未加入社区，显示未加入状态
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
        
        // 加载统计数据
        loadStatistics();
    }
    
    private void loadStatistics() {
        // TODO: 加载用户的求助和活动统计数据
        if (myHelpCountText != null) {
            myHelpCountText.setText("0");
        }
        if (myActivityCountText != null) {
            myActivityCountText.setText("0");
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
        communityActivityLayout.setVisibility(View.VISIBLE);
        communityHelpLayout.setVisibility(View.GONE);
        myCommunityLayout.setVisibility(View.GONE);
    }
    
    private void showCommunityHelpTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, true);
        updateTabState(tabMyCommunity, false);
        
        // 移动指示器
        moveTabIndicator(1);
        
        // 显示对应内容
        communityActivityLayout.setVisibility(View.GONE);
        communityHelpLayout.setVisibility(View.VISIBLE);
        myCommunityLayout.setVisibility(View.GONE);
        
        // 加载求助帖列表
        loadHelppostList();
    }
    
    private void showMyCommunityTab() {
        // 更新标签状态
        updateTabState(tabCommunityActivity, false);
        updateTabState(tabCommunityHelp, false);
        updateTabState(tabMyCommunity, true);
        
        // 移动指示器
        moveTabIndicator(2);
        
        // 显示对应内容
        communityActivityLayout.setVisibility(View.GONE);
        communityHelpLayout.setVisibility(View.GONE);
        myCommunityLayout.setVisibility(View.VISIBLE);
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
        myHelpCountText = view.findViewById(R.id.myHelpCountText);
        myActivityCountText = view.findViewById(R.id.myActivityCountText);
        allHelpPostsButton = view.findViewById(R.id.allHelpPostsButton);
        allActivitiesButton = view.findViewById(R.id.allActivitiesButton);
        myHelpButton = view.findViewById(R.id.myHelpButton);
        myActivitiesButton = view.findViewById(R.id.myActivitiesButton);
        
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
                Toast.makeText(getContext(), "发起求助功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (activityButton != null) {
        activityButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "查看活动功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (allHelpPostsButton != null) {
            allHelpPostsButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "全部求助帖功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (allActivitiesButton != null) {
            allActivitiesButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "全部活动功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (myHelpButton != null) {
            myHelpButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "我的求助功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (myActivitiesButton != null) {
            myActivitiesButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "我的活动功能开发中", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void searchActivity() {
        if (activitySearchEditText != null) {
            String searchText = activitySearchEditText.getText().toString().trim();
            Toast.makeText(getContext(), "搜索活动: " + searchText, Toast.LENGTH_SHORT).show();
            // TODO: 实现活动搜索功能
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
        
        // 使用存储的求助帖ID来查询求助帖信息
        if (!helppostIds.isEmpty()) {
            // 创建列表存储所有求助帖
            List<HelppostVO> allHelpposts = new ArrayList<>();
            final int[] completedRequests = {0};
            final int totalRequests = helppostIds.size();
            
            // 为每个求助帖ID发起请求
            for (Long helppostId : helppostIds) {
                apiService.getBlindHelppostInfo("Bearer " + token, helppostId).enqueue(new ApiCallback<HelppostVO>(getContext()) {
                    @Override
                    public void onSuccess(HelppostVO helppost) {
                        allHelpposts.add(helppost);
                        completedRequests[0]++;
                        
                        // 当所有请求都完成时，更新UI
                        if (completedRequests[0] == totalRequests) {
                            if (helppostAdapter != null) {
                                helppostAdapter.setHelppostList(allHelpposts);
                                helppostAdapter.setOnHelppostClickListener(helppostItem -> {
                                    // 点击求助帖时的处理
                                    Toast.makeText(getContext(), "查看求助帖详情: " + helppostItem.getHelppostId(), Toast.LENGTH_SHORT).show();
                                });
                                
                                Toast.makeText(getContext(), "成功加载 " + allHelpposts.size() + " 条求助帖信息", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    
                    @Override
                    public void onError(String message) {
                        completedRequests[0]++;
                        Log.e("CommunityFragment", "加载求助帖ID " + helppostId + " 失败: " + message);
                        
                        // 当所有请求都完成时，更新UI（即使有些失败了）
                        if (completedRequests[0] == totalRequests) {
                            if (helppostAdapter != null) {
                                helppostAdapter.setHelppostList(allHelpposts);
                                helppostAdapter.setOnHelppostClickListener(helppostItem -> {
                                    // 点击求助帖时的处理
                                    Toast.makeText(getContext(), "查看求助帖详情: " + helppostItem.getHelppostId(), Toast.LENGTH_SHORT).show();
                                });
                                
                                if (allHelpposts.isEmpty()) {
                                    Toast.makeText(getContext(), "暂无有效的求助帖信息", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "成功加载 " + allHelpposts.size() + " 条求助帖信息", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                });
            }
        } else {
            // 如果没有存储的ID，显示空列表
            if (helppostAdapter != null) {
                helppostAdapter.setHelppostList(new ArrayList<>());
            }
            Toast.makeText(getContext(), "请先发布求助帖", Toast.LENGTH_SHORT).show();
        }
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
}