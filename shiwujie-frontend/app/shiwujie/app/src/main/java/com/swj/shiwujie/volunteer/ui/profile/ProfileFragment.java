package com.swj.shiwujie.volunteer.ui.profile;

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
import com.swj.shiwujie.data.model.VolunteerVO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.content.Context;
import com.swj.shiwujie.common.utils.VolunteerUserInfoManager;

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
        View root = inflater.inflate(R.layout.fragment_volunteer_profile, container, false);
        initViews(root);
        initServices();
        initListeners();
        fetchUserInfo(); // 改为主动获取用户信息
        return root;
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
        VolunteerUserInfoManager.init();
    }

    private void initListeners() {
        btnFamily.setOnClickListener(v -> {
            android.util.Log.d("ProfileFragment", "家庭按钮被点击");
            handleFamilyClick();
        });
        btnEditInfo.setOnClickListener(v -> handleEditInfoClick());
        btnChangePassword.setOnClickListener(v -> handleChangePasswordClick());
        btnLogout.setOnClickListener(v -> handleLogoutClick());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountClick());
        tvCommunityStatus.setOnClickListener(v -> handleCommunityClick());
        android.util.Log.d("ProfileFragment", "所有监听器设置完成");
    }

    private void fetchUserInfo() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getVolunteerVOById("Bearer " + token, userId).enqueue(new ApiCallback<VolunteerVO>(requireContext()) {
                         @Override
             public void onSuccess(VolunteerVO data) {
                 updateUI(data);
                 // 全局实名认证检查由VolunteerHomeActivity处理，这里不再重复检查
                 // if (data.getIdCard() == null || data.getIdCard().isEmpty()) {
                 //     showIdCardVerificationDialog();
                 // }
             }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(VolunteerVO data) {
        // 手机号显示
       
        tvAccount.setText("手机号：" + data.getPhone());

        // 更新用户名
        String name = data.getName();
        tvUsername.setText("用户名：" + (name != null && !name.isEmpty() ? name : "未设置"));

        // 更新社区状态
        if (data.getCommunityId() == null) {
            tvCommunityStatus.setText("未加入社区");
        } else {
            tvCommunityStatus.setText("已加入社区");
        }

        // 更新家庭状态
        if (data.getFamilyId() == null) {
            btnFamily.setText("未加入家庭");
        } else {
            btnFamily.setText("已加入家庭");
        }

        // 只要身份证字段不为空就显示已实名，并脱敏显示身份证号
        String idCard = data.getIdCard();
        if (idCard != null && !idCard.isEmpty()) {
            tvAuthStatus.setText("已实名认证");
            // 脱敏身份证号（前3后4，中间*）
            String masked = idCard;
            if (idCard.length() > 7) {
                masked = idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
            }
            // 假设有一个TextView用于显示身份证号，如tvIdCard
            TextView tvIdCard = getView().findViewById(R.id.tvIdCard);
            if (tvIdCard != null) {
                tvIdCard.setText(masked);
                tvIdCard.setVisibility(View.VISIBLE);
            }
        } else {
            tvAuthStatus.setText("未实名认证");
            // 隐藏身份证号显示
            TextView tvIdCard = getView().findViewById(R.id.tvIdCard);
            if (tvIdCard != null) {
                tvIdCard.setVisibility(View.GONE);
            }
        }
    }

    private void showIdCardVerificationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("身份验证提醒")
                .setMessage("您还未进行实名认证，是否现在进行认证？")
                .setPositiveButton("立即认证", (dialog, which) -> {
                    showIdCardInputDialog();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showIdCardInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_id_card_verification, null);
        builder.setView(dialogView);

        EditText etIdCard = dialogView.findViewById(R.id.etIdCard);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(true); // 允许点击外部取消

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String idCard = etIdCard.getText().toString().trim();

            // 验证身份证号
            if (TextUtils.isEmpty(idCard)) {
                Toast.makeText(requireContext(), "身份证号不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$")) {
                Toast.makeText(requireContext(), "身份证号格式不正确", Toast.LENGTH_SHORT).show();
                return;
            }

            // 调用实名认证API
            performIdCardVerification(idCard, dialog);
        });

        dialog.show();
    }

    private void performIdCardVerification(String idCard, AlertDialog dialog) {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        
        VolunteerVO volunteer = new VolunteerVO();
        volunteer.setVolunteerId(userId);
        volunteer.setIdCard(idCard);
        volunteer.setGender(0); 

        // 调用更新用户信息的API
        apiService.updateVolunteerInfo(
                "Bearer " + token,
                volunteer
        ).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(requireContext(), "实名认证成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                // 重新获取用户信息以更新UI
                fetchUserInfo();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "实名认证失败：" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleFamilyClick() {
        android.util.Log.d("ProfileFragment", "handleFamilyClick被调用");
        VolunteerVO userInfo = VolunteerUserInfoManager.getCurrentUserInfo();
        if (userInfo != null) {
            android.util.Log.d("ProfileFragment", "用户信息获取成功，准备跳转到家庭页面");
            // 跳转到志愿者家庭页面
            if (getActivity() != null) {
                android.util.Log.d("ProfileFragment", "Activity存在，开始导航");
                try {
                    // 使用NavController导航到家庭页面
                    androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                    android.util.Log.d("ProfileFragment", "NavController获取成功，开始导航到家庭页面");
                    
                    // 尝试使用popBackStack先清除当前页面，然后导航
                    navController.popBackStack();
                    navController.navigate(R.id.navigation_family);
                    
                    android.util.Log.d("ProfileFragment", "导航命令已发送");
                    Toast.makeText(requireContext(), "正在跳转到家庭页面", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    android.util.Log.e("ProfileFragment", "导航失败", e);
                    Toast.makeText(requireContext(), "跳转失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                android.util.Log.e("ProfileFragment", "Activity为null");
                Toast.makeText(requireContext(), "页面跳转失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            android.util.Log.e("ProfileFragment", "用户信息为null");
            Toast.makeText(requireContext(), "用户信息无效", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEditInfoClick() {
        NavigationHelper.toVolunteerEditProfile(requireContext());
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

            apiService.updateVolunteerPassword(
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

        apiService.volunteerLogout("Bearer " + token)
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
                .setMessage("确定要注销账户吗？注销后账户将被永久删除且无法恢复！")
                .setPositiveButton("确定注销", (dialog, which) -> performDeleteAccount())
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

        apiService.deleteVolunteer("Bearer " + token, userId)
                .enqueue(new ApiCallback<Boolean>(requireContext()) {
                    @Override
                    public void onSuccess(Boolean data) {
                        // 设置清理AI对话的标记
                        setClearConversationFlag();
                        Toast.makeText(requireContext(), "账户已注销", Toast.LENGTH_SHORT).show();
                        navigateToChooseIdentity();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToChooseIdentity() {
        Intent intent = new Intent(requireContext(), ChooseIdentityActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
    
    private void handleCommunityClick() {
        // TODO: 跳转到社区页面
        Toast.makeText(requireContext(), "即将跳转到社区页面", Toast.LENGTH_SHORT).show();
    }
} 