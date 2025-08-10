package com.swj.shiwujie.blind.ui.family;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.ApiCallback;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.data.model.FamilyVO;
import com.swj.shiwujie.data.model.VolunteerVO;
import com.swj.shiwujie.data.model.BaseResponse;

import retrofit2.Call;
import retrofit2.Response;

import java.util.List;

public class FamilyFragment extends Fragment {
    private View root;
    private CardView cardEmptyState;
    private CardView cardFamilyInfo;
    private Button btnJoinFamily;
    private Button btnLeaveFamily;
    private TextView tvFamilyName;
    private TextView tvFamilyDescription;
    private RecyclerView rvBlindMembers;
    private RecyclerView rvVolunteerMembers;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_family, container, false);
        initViews();
        initService();
        setupClickListeners();
        
        // 检查用户是否已登录并获取基本信息
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        
        if (token != null && userId != null) {
            // 如果已登录，立即获取用户信息并更新状态
            loadUserInfo();
        } else {
            // 未登录，显示空状态
            showEmptyState();
        }
        
        return root;
    }

    private void initViews() {
        cardEmptyState = root.findViewById(R.id.cardEmptyState);
        cardFamilyInfo = root.findViewById(R.id.cardFamilyInfo);
        btnJoinFamily = root.findViewById(R.id.btnJoinFamily);
        btnLeaveFamily = root.findViewById(R.id.btnLeaveFamily);
        btnLeaveFamily.setVisibility(View.GONE);
        tvFamilyName = root.findViewById(R.id.tvFamilyName);
        tvFamilyDescription = root.findViewById(R.id.tvFamilyDescription);
        rvBlindMembers = root.findViewById(R.id.rvFamilyMembers);
        rvVolunteerMembers = root.findViewById(R.id.rvFamilyRequests);

        // 盲人端不需要显示加入申请卡片，始终隐藏
        CardView cardFamilyRequests = root.findViewById(R.id.cardFamilyRequests);
        if (cardFamilyRequests != null) {
            cardFamilyRequests.setVisibility(View.GONE);
        }

        rvBlindMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVolunteerMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void initService() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(requireContext());
    }

    private void setupClickListeners() {
        btnJoinFamily.setOnClickListener(v -> showJoinFamilyDialog());
        btnLeaveFamily.setOnClickListener(v -> showLeaveFamilyDialog());
    }

    private void showJoinFamilyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_join_family, null);
        builder.setView(dialogView);

        android.widget.EditText etFamilyId = dialogView.findViewById(R.id.etFamilyId);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        btnConfirm.setOnClickListener(v -> {
            String familyVolunteerPhone = etFamilyId.getText().toString().trim();
            if (familyVolunteerPhone.isEmpty()) {
                Toast.makeText(requireContext(), "请输入家主手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            joinFamily(familyVolunteerPhone);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void loadUserInfo() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            showEmptyState();
            return;
        }

        // 获取用户信息，检查是否已加入家庭
        apiService.getBlindById("Bearer " + token, userId).enqueue(new ApiCallback<BlindVO>(requireContext()) {
            @Override
            public void onSuccess(BlindVO data) {
                updateViewBasedOnFamilyStatus(data);
            }
            
            @Override
            public void onError(String message) {
                // 获取用户信息失败，显示空状态
                showEmptyState();
            }
        });
    }
    
    private void updateViewBasedOnFamilyStatus(BlindVO userInfo) {
        if (userInfo == null) {
            // 用户信息为空，显示空状态
            showEmptyState();
            return;
        }
        
        // 检查用户是否已加入家庭
        if (userInfo.getFamilyId() != null) {
            // 已加入家庭，获取家庭信息并显示
            getFamilyInfo(userInfo.getFamilyId());
        } else {
            // 未加入家庭，显示空状态
            showEmptyState();
        }
    }
    
    private void checkFamilyStatus() {
        // 保留此方法用于刷新状态（加入/退出家庭后调用）
        loadUserInfo();
    }

    private void getFamilyInfo(Long familyId) {
        String token = "Bearer " + SharedPrefsUtil.getToken();
        if (token == null) {
            return;
        }

        apiService.getFamilyVOById(token, familyId).enqueue(new ApiCallback<FamilyVO>(requireContext()) {
            @Override
            public void onSuccess(FamilyVO response) {
                if (response != null) {
                    updateFamilyInfo(response);
                } else {
                    showEmptyState();
                }
            }
            
            @Override
            public void onError(String message) {
                // 家庭不存在时，显示加入家庭卡片而不是弹窗
                if (message.contains("家庭不存在")) {
                    showEmptyState();
                    // 清除用户的家庭ID，因为家庭已被删除
                    clearUserFamilyId();
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void clearUserFamilyId() {
        // 清除本地存储的家庭ID，让用户重新加入家庭
        // 这里可以调用API更新用户信息，清除家庭ID
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        
        if (token != null && userId != null) {
            // 可以调用更新用户信息的API，清除familyId
            // 暂时只显示提示
        }
    }

    private void updateFamilyInfo(FamilyVO family) {
        if (family != null) {
            cardFamilyInfo.setVisibility(View.VISIBLE);
            cardEmptyState.setVisibility(View.GONE);
            btnJoinFamily.setVisibility(View.GONE);

            tvFamilyName.setText(family.getFamilyName() != null ? family.getFamilyName() : "未命名家庭");
            tvFamilyDescription.setText(family.getFamilyDescription() != null ? family.getFamilyDescription() : "暂无描述");

            // 更新盲人成员列表
            if (family.getBlindVOList() != null) {
                rvBlindMembers.setAdapter(new BlindMemberAdapter(family.getBlindVOList()));
            }

            // 更新志愿者成员列表
            if (family.getVolunteerVOList() != null) {
                rvVolunteerMembers.setAdapter(new VolunteerMemberAdapter(family.getVolunteerVOList()));
            }

            // 检查是否显示退出按钮
            Long currentUserId = SharedPrefsUtil.getUserId();
            if (currentUserId != null && family.getCreatorVolunteer() != null) {
                btnLeaveFamily.setVisibility(currentUserId.equals(family.getCreatorVolunteer().getVolunteerId()) ? 
                    View.GONE : View.VISIBLE);
            }
        } else {
            showEmptyState();
        }
    }

    private void showEmptyState() {
        cardEmptyState.setVisibility(View.VISIBLE);
        cardFamilyInfo.setVisibility(View.GONE);
        btnJoinFamily.setVisibility(View.VISIBLE);
        btnLeaveFamily.setVisibility(View.GONE);
    }

    private void joinFamily(String familyVolunteerPhone) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.joinFamily("Bearer " + token, familyVolunteerPhone).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean response) {
                if (response) {
                    checkFamilyStatus();
                }
            }

            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 显示后端返回的message
                    Toast.makeText(requireContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                }
                super.onResponse(call, response);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "申请失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLeaveFamilyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出家庭")
                .setMessage("确定要退出当前家庭吗？")
                .setPositiveButton("确定", (dialog, which) -> leaveFamily())
                .setNegativeButton("取消", null)
                .show();
    }

    private void leaveFamily() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 直接调用退出家庭接口
        apiService.leaveFamily("Bearer " + token).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean data) {
                if (data) {
                    Toast.makeText(requireContext(), "已退出家庭", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                    // 刷新家庭状态
                    checkFamilyStatus();
                } else {
                    Toast.makeText(requireContext(), "退出家庭失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "退出家庭失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 盲人成员适配器
    private static class BlindMemberAdapter extends RecyclerView.Adapter<BlindMemberAdapter.ViewHolder> {
        private final List<BlindVO> members;

        BlindMemberAdapter(List<BlindVO> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_family_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BlindVO member = members.get(position);
            holder.tvName.setText(member.getName());
            holder.tvRole.setText("盲人");
            holder.tvId.setText(String.format("ID: %d", member.getBlindId()));
        }

        @Override
        public int getItemCount() {
            return members != null ? members.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvRole;
            TextView tvId;

            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(R.id.tvMemberName);
                tvRole = view.findViewById(R.id.tvMemberRole);
                tvId = view.findViewById(R.id.tvMemberId);
            }
        }
    }

    // 志愿者成员适配器
    private static class VolunteerMemberAdapter extends RecyclerView.Adapter<VolunteerMemberAdapter.ViewHolder> {
        private final List<VolunteerVO> members;

        VolunteerMemberAdapter(List<VolunteerVO> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_family_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            VolunteerVO member = members.get(position);
            holder.tvName.setText(member.getName());
            holder.tvRole.setText("志愿者");
            holder.tvId.setText(String.format("ID: %d", member.getVolunteerId()));
        }

        @Override
        public int getItemCount() {
            return members != null ? members.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvRole;
            TextView tvId;

            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(R.id.tvMemberName);
                tvRole = view.findViewById(R.id.tvMemberRole);
                tvId = view.findViewById(R.id.tvMemberId);
            }
        }
    }
} 