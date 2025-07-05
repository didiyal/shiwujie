package com.swj.shiwujie.blind.ui.profile;

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
import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.BlindVO;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private TextView tvUsername;
    private TextView tvAccount;
    private TextView tvAuthStatus;
    private TextView btnFamily;
    private TextView btnEditInfo;
    private TextView btnLogout;
    private TextView btnDeleteAccount;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(root);
        initServices();
        initListeners();
        loadUserInfo();
        return root;
    }

    private void initViews(View root) {
        tvUsername = root.findViewById(R.id.tvUsername);
        tvAccount = root.findViewById(R.id.tvAccount);
        tvAuthStatus = root.findViewById(R.id.tvAuthStatus);
        btnFamily = root.findViewById(R.id.btnFamily);
        btnEditInfo = root.findViewById(R.id.btnEditInfo);
        btnLogout = root.findViewById(R.id.btnLogout);
        btnDeleteAccount = root.findViewById(R.id.btnDeleteAccount);
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(requireContext());
    }

    private void initListeners() {
        btnFamily.setOnClickListener(v -> handleFamilyClick());
        btnEditInfo.setOnClickListener(v -> handleEditInfoClick());
        btnLogout.setOnClickListener(v -> handleLogoutClick());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountClick());
    }

    private void loadUserInfo() {
        // 从SharedPreferences加载用户信息
        String phone = SharedPrefsUtil.getPhone();
        Long userId = SharedPrefsUtil.getUserId();
        
        if (phone != null) {
            tvAccount.setText("账号：" + phone);
        }
        
        if (userId != null) {
            // TODO: 调用API获取用户详细信息
            // 这里暂时使用默认值
            tvUsername.setText("用户名：未设置");
        }
    }

    private void handleFamilyClick() {
        // TODO: 实现创建/加入家庭功能
        Toast.makeText(requireContext(), "创建/加入家庭功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void handleEditInfoClick() {
        // TODO: 实现修改个人信息功能
        Toast.makeText(requireContext(), "修改个人信息功能开发中", Toast.LENGTH_SHORT).show();
    }

    private void handleLogoutClick() {
        // 清除用户信息
        SharedPrefsUtil.clearAll();
        
        // 跳转到身份选择页面
        Intent intent = new Intent(requireContext(), ChooseIdentityActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
                .enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Boolean> baseResponse = response.body();
                            if (baseResponse.getCode() == 0 && Boolean.TRUE.equals(baseResponse.getData())) {
                                // 删除成功后立即清除本地数据并跳转
                                SharedPrefsUtil.clearAll();
                                navigateToChooseIdentity();
                            } else {
                                Toast.makeText(requireContext(), 
                                    baseResponse.getMessage() != null ? baseResponse.getMessage() : "注销失败", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(requireContext(), "注销失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Toast.makeText(requireContext(), "网络错误，请稍后重试", Toast.LENGTH_SHORT).show();
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
} 