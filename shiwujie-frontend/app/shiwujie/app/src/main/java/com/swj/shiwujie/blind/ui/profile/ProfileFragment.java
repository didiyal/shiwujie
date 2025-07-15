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
        View root = inflater.inflate(R.layout.fragment_profile, container, false);
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
        UserInfoManager.init();
    }

    private void initListeners() {
        btnFamily.setOnClickListener(v -> handleFamilyClick());
        btnEditInfo.setOnClickListener(v -> handleEditInfoClick());
        btnLogout.setOnClickListener(v -> handleLogoutClick());
        btnDeleteAccount.setOnClickListener(v -> handleDeleteAccountClick());
        tvCommunityStatus.setOnClickListener(v -> handleCommunityClick()); // 修改为新的控件
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
                updateUI(data);
                // 如果未进行身份验证，显示提示弹窗
                if (!data.getIsDisabilityCard()) {
                    showDisabilityVerificationDialog();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
        // 更新账号信息（使用blindId）
        tvAccount.setText("账号：" + data.getBlindId());

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

        // 更新身份认证状态
        StringBuilder authStatus = new StringBuilder();
        if (data.getIsDisabilityCard()) {
            authStatus.append("已完成身份验证");
        } else {
            authStatus.append("未完成身份验证");
        }
        if (data.getIsIdCard()) {
            authStatus.append(" | 已实名");
        } else {
            authStatus.append(" | 未实名");
        }
        tvAuthStatus.setText(authStatus.toString());

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
        BlindVO userInfo = UserInfoManager.getCurrentUserInfo();
        if (userInfo != null) {
            // 无论是否已加入家庭，都可以点击跳转
            // TODO: 跳转到家庭页面
            Toast.makeText(requireContext(), "即将跳转到家庭页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleEditInfoClick() {
        NavigationHelper.toBlindEditProfile(requireContext());
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
        BlindVO userInfo = UserInfoManager.getCurrentUserInfo();
        if (userInfo != null) {
            // 无论是否已加入社区，都可以点击跳转
            // TODO: 跳转到社区页面
            Toast.makeText(requireContext(), "即将跳转到社区页面", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendJoinFamilyRequest(Long familyId) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.joinFamily("Bearer " + token, familyId).enqueue(new ApiCallback<Boolean>(requireContext()) {
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