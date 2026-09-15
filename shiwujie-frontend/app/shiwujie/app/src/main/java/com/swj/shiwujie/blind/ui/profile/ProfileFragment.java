package com.swj.shiwujie.blind.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.ChooseIdentityActivity;
import com.swj.shiwujie.R;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.navigation.NavigationHelper;
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.data.model.FamilyVO;
import com.swj.shiwujie.data.model.FamilyJoinReviewVO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Context;
import com.swj.shiwujie.common.utils.UserInfoManager;

import java.util.List;
import android.os.Handler;
import android.os.Looper;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class ProfileFragment extends Fragment {
    private TextView tvUsername;
    private TextView tvAccount;
    private TextView tvAuthStatus;
    private TextView tvCommunityStatus;
    private TextView btnFamily;
    private TextView btnEditInfo;
    private TextView btnChangePassword;
    private TextView btnLogout;
    private TextView btnDeleteAccount;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(root);
        initServices();
        initListeners(root);
        fetchUserInfo(); // 改为主动获取用户信息
        return root;
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 检查是否需要自动跳转到编辑页面
        if (getArguments() != null && "edit_profile".equals(getArguments().getString("action"))) {
            // 获取来源标记
            String source = getArguments().getString("source", "normal");
            
            // 立即跳转到编辑页面，不需要延迟
            if ("ai".equals(source)) {
                // 来自AI页面，传递特殊标记
                Intent intent = new Intent(requireContext(), com.swj.shiwujie.blind.EditProfileActivity.class);
                intent.putExtra("source", "ai");
                startActivity(intent);
            } else {
                // 常规跳转，使用NavigationHelper
                NavigationHelper.toBlindEditProfile(requireContext());
            }
            
            // 立即清理参数，避免重复跳转
            if (getArguments() != null) {
                getArguments().remove("action");
                getArguments().remove("source");
            }
        }
    }

    private void initViews(View root) {
        tvUsername = root.findViewById(R.id.tvUsername);
        tvAccount = root.findViewById(R.id.tvAccount);
        tvAuthStatus = root.findViewById(R.id.tvAuthStatus);
        tvCommunityStatus = root.findViewById(R.id.tvCommunityStatus);
        btnFamily = root.findViewById(R.id.btnFamily);
        btnEditInfo = root.findViewById(R.id.btnEditInfo);
        btnChangePassword = root.findViewById(R.id.btnChangePassword);
        btnLogout = root.findViewById(R.id.btnLogout);
        btnDeleteAccount = root.findViewById(R.id.btnDeleteAccount);
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(requireContext());
        // 在Activity或Fragment中初始化
        UserInfoManager.init();
    }

    private void initListeners(View root) {
        btnFamily.setOnClickListener(v -> {
            android.util.Log.d("ProfileFragment", "家庭按钮被点击");
            handleFamilyClick();
        });
        btnEditInfo.setOnClickListener(v -> handleEditInfoClick());
        btnChangePassword.setOnClickListener(v -> handleChangePasswordClick());
        btnLogout.setOnClickListener(v -> handleLogoutClick());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountClick());
        tvCommunityStatus.setOnClickListener(v -> handleCommunityClick()); // 修改为新的控件
        // 2026-09-15：社区入口 = tvCommunityStatus 状态行；家庭入口 = btnFamily 状态行
        // 返回按钮：回 AI 主页（2026-09-16 修复：initListeners 在 onCreateView 内执行，
        // 此前用 requireView() 会因视图未挂载直接抛异常，导致点"我的"进页面即闪退）
        View backButton = root.findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (getActivity() != null) {
                    androidx.navigation.NavController navController =
                            androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigateUp();
                }
            });
        }
        android.util.Log.d("ProfileFragment", "所有监听器设置完成");
    }

    private void fetchUserInfo() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(requireContext()) {
            @Override
            public void onSuccess(BlindVO data) {
                // 2026-09-14：身份校验已按产品要求关闭——"我的"页不再弹身份验证提醒
                updateUI(data);
            }

            @Override
            public void onError(String message) {
                // 对于用户信息获取失败，不自动跳转，只显示错误信息
                Toast.makeText(requireContext(), "获取用户信息失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getFamilyJoinRequests() {
        String token = SharedPrefsUtil.getToken();
        android.util.Log.d("ProfileFragment", "开始获取家庭申请列表");
        apiService.getFamilyJoinReviewVOList("Bearer " + token).enqueue(new ApiCallback<List<FamilyJoinReviewVO>>(requireContext()) {
            @Override
            public void onSuccess(List<FamilyJoinReviewVO> data) {
                android.util.Log.d("ProfileFragment", "获取家庭申请列表成功: " + (data != null ? data.size() : "null"));
                // TODO: 处理申请列表数据
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("ProfileFragment", "获取家庭申请列表失败: " + message);
                Toast.makeText(requireContext(), "获取加入申请失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(BlindVO data) {
        // 显示手机号
        tvAccount.setText("手机号：" + data.getPhone());
        // 更新用户名：未设置时默认"用户+手机尾号"（2026-09-15）
        String name = data.getName();
        String displayName;
        if (name != null && !name.trim().isEmpty()) {
            displayName = name;
        } else {
            displayName = defaultNameByPhone(data.getPhone());
        }
        tvUsername.setText("用户名：" + displayName);

        // 更新社区状态（点按进入社区页）
        if (data.getCommunityId() == null) {
            tvCommunityStatus.setText("未加入社区 ›");
        } else {
            tvCommunityStatus.setText("已加入社区 ›");
        }

        // 更新家庭状态（点按进入家庭页）
        if (data.getFamilyId() == null) {
            btnFamily.setText("未加入家庭 ›");
        } else {
            btnFamily.setText("已加入家庭 ›");
        }

        // 2026-09-14：身份校验/实名已下线，不再展示"已验证/未实名"状态
        tvAuthStatus.setVisibility(android.view.View.GONE);

        // 如果用户有家庭ID，获取家庭信息并检查是否是家主
        if (data.getFamilyId() != null) {
            String token = SharedPrefsUtil.getToken();
            apiService.getFamilyVOById("Bearer " + token, data.getFamilyId()).enqueue(new ApiCallback<FamilyVO>(requireContext()) {
                @Override
                public void onSuccess(FamilyVO familyData) {
                    if (familyData != null) {
                        // 检查是否是家主
                        Long currentUserId = SharedPrefsUtil.getUserId();
                        if (currentUserId != null && familyData.getCreatorVolunteer() != null && 
                            currentUserId.equals(familyData.getCreatorVolunteer().getVolunteerId())) {
                            android.util.Log.d("FamilyFragment", "当前用户是家主，获取申请列表");
                            getFamilyJoinRequests();
                        } else {
                            android.util.Log.d("FamilyFragment", "当前用户不是家主");
                        }
                    }
                }

                @Override
                public void onError(String message) {
                    android.util.Log.e("FamilyFragment", "获取家庭信息失败: " + message);
                }
            });
        }
    }

    private void showDisabilityVerificationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("身份验证提醒")
                .setMessage("您还未进行身份验证，是否现在进行验证？")
                .setPositiveButton("立即验证", (dialog, which) -> {
                    // TODO: 跳转到身份验证页面

                })
            
                .show();
    }

    private void handleFamilyClick() {
        android.util.Log.d("ProfileFragment", "handleFamilyClick被调用");
        // 2026-09-15：不再以缓存用户信息为前置条件（此前缓存未就绪时点击无响应），家庭页自行加载数据
        if (getActivity() != null) {
            try {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                // 替换式导航：家庭页替换“我的”，返回钮/返回键即回 AI 主页（2026-09-16）
                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_profile, true)
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(R.id.navigation_family, null, navOptions);
            } catch (Exception e) {
                android.util.Log.e("ProfileFragment", "导航失败", e);
                Toast.makeText(requireContext(), "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** 未设置姓名时的默认展示名：用户 + 手机尾号（2026-09-15） */
    private String defaultNameByPhone(String phone) {
        if (phone != null && phone.length() >= 4) {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return "用户";
    }

    private void handleEditInfoClick() {
        NavigationHelper.toBlindEditProfile(requireContext());
    }

    private void handleChangePasswordClick() {
        showChangePasswordDialog();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText etOriginPassword = dialogView.findViewById(R.id.etOriginPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String originPassword = etOriginPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // 验证新密码
            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(requireContext(), "新密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用修改密码API
            String token = SharedPrefsUtil.getToken();
            Long userId = SharedPrefsUtil.getUserId();

            if (token == null || userId == null) {
                Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
                return;
            }

            apiService.updateBlindPassword(
                    "Bearer " + token,
                    userId,
                    originPassword,
                    newPassword
            ).enqueue(new ApiCallback<Boolean>(requireContext()) {
                @Override
                public void onSuccess(Boolean response) {
                    Toast.makeText(requireContext(), "密码修改成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(requireContext(), "修改失败：" + message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void handleLogoutClick() {
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.logout("Bearer " + token)
                .enqueue(new ApiCallback<Boolean>(requireContext()) {
                    @Override
                    public void onSuccess(Boolean data) {
                        // 设置清理AI对话的标记
                        setClearConversationFlag();
                        // 清除本地存储的登录信息
                        SharedPrefsUtil.clearAll();
                        // 退出成功直接跳转到身份选择页面，并添加标记
                        Intent intent = new Intent(requireContext(), ChooseIdentityActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra(ChooseIdentityActivity.EXTRA_FROM_LOGOUT, true);
                        startActivity(intent);
                        if (getActivity() != null) {
                            getActivity().finish();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleDeleteAccountClick() {
        new AlertDialog.Builder(requireContext())
                .setTitle("注销账户")
                .setMessage("确定要注销账户吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> {
                    performDeleteAccount();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performDeleteAccount() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.deleteBlindAccount("Bearer " + token, userId)
                .enqueue(new ApiCallback<Boolean>(requireContext()) {
                    @Override
                    public void onSuccess(Boolean data) {
                                // 设置清理AI对话的标记
                                setClearConversationFlag();
                                // 删除成功后立即清除本地数据并跳转
                                SharedPrefsUtil.clearAll();
                                Intent intent = new Intent(requireContext(), ChooseIdentityActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                if (getActivity() != null) {
                                    getActivity().finish();
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToChooseIdentity() {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            Toast.makeText(requireContext(), "账户已注销", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(requireContext(), ChooseIdentityActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void handleCommunityClick() {
        // 2026-09-15 二期重构：直接跳转社区页面（tabBar 删除前的 TODO 已落地；去掉用户信息门禁）
        if (getActivity() != null) {
            try {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                // 社区页同样替换式导航（2026-09-16）
                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.navigation_profile, true)
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(R.id.navigation_community, null, navOptions);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 设置清理AI对话的标记
     */
    private void setClearConversationFlag() {
        try {
            Context context = requireContext();
            android.content.SharedPreferences prefs = context.getSharedPreferences("ai_conversation_history", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("should_clear_conversation", true);
            editor.apply();
        } catch (Exception e) {
            android.util.Log.e("ProfileFragment", "设置清理对话标记失败", e);
        }
    }
    
    private void sendJoinFamilyRequest(String familyVolunteerPhone) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.joinFamily("Bearer " + token, familyVolunteerPhone).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean data) {
                if (data != null && data) {
                    Toast.makeText(requireContext(), "申请已发送，等待家主审核", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "申请加入家庭失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "申请失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
} 