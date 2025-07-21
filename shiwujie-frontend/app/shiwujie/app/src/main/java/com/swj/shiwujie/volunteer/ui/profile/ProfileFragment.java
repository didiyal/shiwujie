package com.swj.shiwujie.volunteer.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        btnFamily.setOnClickListener(v -> handleFamilyClick());
        btnEditInfo.setOnClickListener(v -> handleEditInfoClick());
        btnLogout.setOnClickListener(v -> handleLogoutClick());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountClick());
        tvCommunityStatus.setOnClickListener(v -> handleCommunityClick());
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
                // 取消未实名弹窗
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
        // 更新账号信息（使用volunteerId）
        tvAccount.setText("账号：" + data.getVolunteerId());

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
                    // TODO: 跳转到身份验证页面
                })
                .show();
    }

    private void handleFamilyClick() {
        VolunteerVO userInfo = VolunteerUserInfoManager.getCurrentUserInfo();
        if (userInfo != null) {
            // 无论是否已加入家庭，都可以点击跳转
            // TODO: 跳转到家庭页面
            Toast.makeText(requireContext(), "即将跳转到家庭页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEditInfoClick() {
        NavigationHelper.toVolunteerEditProfile(requireContext());
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

    private void handleCommunityClick() {
        // TODO: 跳转到社区页面
        Toast.makeText(requireContext(), "即将跳转到社区页面", Toast.LENGTH_SHORT).show();
    }
} 