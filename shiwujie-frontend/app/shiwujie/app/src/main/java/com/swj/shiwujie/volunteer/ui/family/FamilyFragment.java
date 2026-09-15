package com.swj.shiwujie.volunteer.ui.family;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;

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
import com.swj.shiwujie.data.model.FamilyJoinReviewVO;
import com.swj.shiwujie.data.model.FamilyVO;
import com.swj.shiwujie.data.model.VolunteerVO;
import com.swj.shiwujie.data.model.BaseResponse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Response;

public class FamilyFragment extends Fragment {
    private CardView cardFamilyInfo;
    private CardView cardEmptyState;
    private RecyclerView rvFamilyMembers;
    private Button btnJoinFamily;
    private Button btnCreateFamily;
    private Button btnDeleteFamily;
    private Button btnRemoveMembers;
    private Button btnLeaveFamily;
    private ApiService apiService;
    private FamilyVO currentFamily;

    // 成员适配器（2026-09-15：统一列表，不分盲人/志愿者；加入申请卡随审核取消下线）
    private UnifiedMemberAdapter familyMemberAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_family, container, false);
        initViews(root);
        initServices();
        initAdapters();
        checkFamilyStatus();
        return root;
    }

    private void initViews(View root) {
        cardFamilyInfo = root.findViewById(R.id.cardFamilyInfo);
        cardEmptyState = root.findViewById(R.id.cardEmptyState);
        rvFamilyMembers = root.findViewById(R.id.rvFamilyMembers);
        btnJoinFamily = root.findViewById(R.id.btnJoinFamily);
        btnCreateFamily = root.findViewById(R.id.btnCreateFamily);
        btnDeleteFamily = root.findViewById(R.id.btnDeleteFamily);
        btnRemoveMembers = root.findViewById(R.id.btnRemoveMembers);
        btnLeaveFamily = root.findViewById(R.id.btnLeaveFamily);

        // 设置RecyclerView的布局管理器
        rvFamilyMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 设置按钮点击事件
        btnJoinFamily.setOnClickListener(v -> showJoinFamilyDialog());
        btnCreateFamily.setOnClickListener(v -> showCreateFamilyDialog());
        btnDeleteFamily.setVisibility(View.GONE); // 默认隐藏
        btnDeleteFamily.setOnClickListener(v -> showDeleteFamilyDialog());
        btnRemoveMembers.setVisibility(View.GONE); // 默认隐藏，只有家主才显示
        btnRemoveMembers.setOnClickListener(v -> showRemoveMembersDialog());
        btnLeaveFamily.setVisibility(View.GONE); // 默认隐藏
    }

    private void initServices() {
        apiService = RetrofitClient.getInstance().createService(ApiService.class);
        SharedPrefsUtil.init(requireContext());
    }

    private void initAdapters() {
        familyMemberAdapter = new UnifiedMemberAdapter();
        rvFamilyMembers.setAdapter(familyMemberAdapter);
    }

    private void checkFamilyStatus() {
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();

        if (token == null || userId == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先获取用户信息，检查是否已加入家庭
        apiService.getVolunteerVOById("Bearer " + token, userId).enqueue(new ApiCallback<VolunteerVO>(requireContext()) {
            @Override
            public void onSuccess(VolunteerVO data) {
                if (data.getFamilyId() != null) {
                    // 已加入家庭，获取家庭信息
                    getFamilyInfo(data.getFamilyId());
                } else {
                    // 未加入家庭，显示空状态
                    showEmptyState();
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void getFamilyInfo(Long familyId) {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            return;
        }

        apiService.getFamilyVOById("Bearer " + token, familyId).enqueue(new ApiCallback<FamilyVO>(requireContext()) {
            @Override
            public void onSuccess(FamilyVO response) {
                if (response != null) {
                    currentFamily = response;
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
                    showEmptyState();
                }
            }
        });
    }
    
    private void clearUserFamilyId() {
        // 清除本地存储的家庭ID，让用户重新加入家庭
        String token = SharedPrefsUtil.getToken();
        Long userId = SharedPrefsUtil.getUserId();
        
        if (token != null && userId != null) {
            // 可以调用更新用户信息的API，清除familyId
            // 暂时只显示提示

        }
    }

    private void updateFamilyInfo(FamilyVO family) {
        android.util.Log.d("FamilyFragment", "开始更新UI,家庭ID: " + family.getFamilyId());
        currentFamily = family;

        // 确保在主线程中更新UI
        requireActivity().runOnUiThread(() -> {
            cardFamilyInfo.setVisibility(View.VISIBLE);
            cardEmptyState.setVisibility(View.GONE);

            // 成员数量（标题旁小字）
            int totalMembers = 0;
            if (family.getBlindVOList() != null) {
                totalMembers += family.getBlindVOList().size();
            }
            if (family.getVolunteerVOList() != null) {
                totalMembers += family.getVolunteerVOList().size();
            }
            // 2026-09-16：家主在 VO 装配时被移出志愿者列表，计数需补回
            if (family.getCreatorVolunteer() != null) {
                totalMembers += 1;
            }
            TextView tvMemberCount = requireView().findViewById(R.id.tvMemberCount);
            if (tvMemberCount != null) {
                tvMemberCount.setText(totalMembers + "人");
            }

            // 检查是否是家主（决定解散/移除/退出按钮可见性）
            Long currentUserId = SharedPrefsUtil.getUserId();
            if (currentUserId != null && family.getCreatorVolunteer() != null
                    && currentUserId.equals(family.getCreatorVolunteer().getVolunteerId())) {
                btnDeleteFamily.setVisibility(View.VISIBLE);
                btnRemoveMembers.setVisibility(View.VISIBLE);
                btnLeaveFamily.setVisibility(View.GONE);
            } else {
                btnDeleteFamily.setVisibility(View.GONE);
                btnRemoveMembers.setVisibility(View.GONE);
                btnLeaveFamily.setVisibility(View.VISIBLE);
                btnLeaveFamily.setOnClickListener(v -> showLeaveFamilyDialog());
            }

            // 统一成员列表
            updateMemberLists(family);
        });
    }

    private void showEmptyState() {
        cardFamilyInfo.setVisibility(View.GONE);
        cardEmptyState.setVisibility(View.VISIBLE);
    }

    private void showJoinFamilyDialog() {
        // 创建输入框
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_PHONE); // 允许输入手机号
        input.setHint("请输入家主手机号");

        // 创建对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("加入家庭")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String familyVolunteerPhone = input.getText().toString().trim();
                    if (familyVolunteerPhone.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入家主手机号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendJoinFamilyRequest(familyVolunteerPhone);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showCreateFamilyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("创建家庭")
                .setMessage("确定要创建一个新的家庭吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    sendCreateFamilyRequest();
                })
                .setNegativeButton("取消", null)
                .show();
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
                    // 2026-09-15：加入家庭已取消审核，直连生效
                    Toast.makeText(requireContext(), "已成功加入家庭", Toast.LENGTH_SHORT).show();
                    // 家庭关系变化，重启 WS 刷新会话状态
                    com.swj.shiwujie.common.network.WebSocketService.restart(requireContext());
                    checkFamilyStatus();
                }
            }

            @Override
            public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                super.onResponse(call, response);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "申请失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendCreateFamilyRequest() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            android.util.Log.e("FamilyFragment", "创建家庭失败: token为空");
            return;
        }
        
        android.util.Log.d("FamilyFragment", "开始创建家庭请求");
        apiService.createFamily("Bearer " + token).enqueue(new ApiCallback<FamilyVO>(requireContext()) {
            @Override
            public void onSuccess(FamilyVO data) {
                if (data != null && data.getFamilyId() != null) {
                    android.util.Log.d("FamilyFragment", "创建家庭成功,familyId: " + data.getFamilyId());
                    showSuccessDialog("创建家庭成功！");
                    // 家庭关系变化，重启 WS 刷新会话状态（2026-09-15）
                    com.swj.shiwujie.common.network.WebSocketService.restart(requireContext());
                    // 使用返回的familyId重新获取完整的家庭信息
                    getFamilyInfo(data.getFamilyId());
                } else {
                    android.util.Log.e("FamilyFragment", "创建家庭失败: 返回数据为空或familyId为空");
                    Toast.makeText(requireContext(), "创建家庭失败:返回数据无效", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("FamilyFragment", "创建家庭失败: " + message);
                Toast.makeText(requireContext(), "创建家庭失败:" + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    private void updateMemberLists(FamilyVO family) {
        // 2026-09-15：成员统一列表（不分盲人/志愿者），名称兜底"用户+手机尾号"，身份标签区分
        List<Object> allMembers = new ArrayList<>();
        // 2026-09-16：家主在 VO 装配时被移出志愿者列表，这里补回置顶展示
        if (family.getCreatorVolunteer() != null) {
            allMembers.add(family.getCreatorVolunteer());
        }
        if (family.getBlindVOList() != null) {
            allMembers.addAll(family.getBlindVOList());
        }
        if (family.getVolunteerVOList() != null) {
            allMembers.addAll(family.getVolunteerVOList());
        }
        familyMemberAdapter.updateMembers(allMembers, family.getCreatorVolunteer());
        android.util.Log.d("FamilyFragment", "成员列表更新完成，共 " + allMembers.size() + " 人");
    }

    /** 统一成员：id + 展示名 + 身份标签 */
    private static class MemberInfo {
        Long id;
        String name;
        String role;
        boolean creator;

        MemberInfo(Long id, String name, String role, boolean creator) {
            this.id = id;
            this.name = name;
            this.role = role;
            this.creator = creator;
        }
    }

    /** 统一成员适配器（2026-09-15）：不分盲人/志愿者，名称兜底"用户+手机尾号"，身份标签区分 */
    private class UnifiedMemberAdapter extends RecyclerView.Adapter<UnifiedMemberAdapter.ViewHolder> {
        private List<Object> members = new ArrayList<>();
        private VolunteerVO creator;

        void updateMembers(List<Object> newMembers, VolunteerVO creatorInfo) {
            this.members = new ArrayList<>(newMembers);
            this.creator = creatorInfo;
            notifyDataSetChanged();
        }

        /** 名称兜底：无名/null 时显示「用户+手机尾号」 */
        private String displayName(String name, String phone) {
            // "无名"（历史默认名）与空值一并兜底为「用户+手机尾号」（2026-09-16）
            if (name != null && !name.trim().isEmpty() && !"无名".equals(name.trim())) {
                return name;
            }
            if (phone != null && phone.length() >= 4) {
                return "用户" + phone.substring(phone.length() - 4);
            }
            return "用户";
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
            Object member = members.get(position);
            String name;
            String role;
            if (member instanceof BlindVO) {
                BlindVO b = (BlindVO) member;
                name = displayName(b.getName(), b.getPhone());
                role = "盲人";
            } else if (member instanceof VolunteerVO) {
                VolunteerVO v = (VolunteerVO) member;
                name = displayName(v.getName(), v.getPhone());
                boolean isCreator = creator != null && v.getVolunteerId() != null
                        && v.getVolunteerId().equals(creator.getVolunteerId());
                role = isCreator ? "家主" : "家属";
            } else {
                name = "用户";
                role = "";
            }
            holder.tvMemberName.setText(name);
            holder.tvMemberRole.setText(role);
            holder.tvMemberId.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMemberName;
            TextView tvMemberId;
            TextView tvMemberRole;

            ViewHolder(View view) {
                super(view);
                tvMemberName = view.findViewById(R.id.tvMemberName);
                tvMemberId = view.findViewById(R.id.tvMemberId);
                tvMemberRole = view.findViewById(R.id.tvMemberRole);
            }
        }
    }

    private void showDeleteFamilyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("解散家庭")
                .setMessage("确定要解散家庭吗？此操作不可恢复！")
                .setPositiveButton("确定", (dialog, which) -> deleteFamily())
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteFamily() {
        String token = "Bearer " + SharedPrefsUtil.getToken();
        if (token == null) return;

        apiService.deleteFamily(token).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean response) {
                Toast.makeText(requireContext(), "家庭已解散", Toast.LENGTH_SHORT).show();
                // 刷新页面状态
                checkFamilyStatus();
            }
        });
    }

    private void showRemoveMembersDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_remove_members, null);
        RecyclerView rvMembers = dialogView.findViewById(R.id.rvMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        RemoveMemberAdapter adapter = new RemoveMemberAdapter();
        rvMembers.setAdapter(adapter);
        
        // 合并盲人和志愿者成员列表
        List<MemberItem> allMembers = new ArrayList<>();
        if (currentFamily != null) {
            if (currentFamily.getBlindVOList() != null) {
                for (BlindVO blind : currentFamily.getBlindVOList()) {
                    allMembers.add(new MemberItem(blind.getBlindId(), null, blind.getName(), "盲人"));
                }
            }
            if (currentFamily.getVolunteerVOList() != null) {
                for (VolunteerVO volunteer : currentFamily.getVolunteerVOList()) {
                    // 不包含家主自己
                    if (!volunteer.getVolunteerId().equals(SharedPrefsUtil.getUserId())) {
                        allMembers.add(new MemberItem(null, volunteer.getVolunteerId(), volunteer.getName(), "志愿者"));
                            }
                }
            }
        }
        adapter.setMembers(allMembers);

        builder.setView(dialogView)
                .setTitle("选择要移除的成员")
                .setPositiveButton("确定", (dialog, which) -> {
                    List<MemberItem> selectedMembers = adapter.getSelectedMembers();
                    if (!selectedMembers.isEmpty()) {
                        removeSelectedMembers(selectedMembers);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void removeSelectedMembers(List<MemberItem> members) {
        String token = "Bearer " + SharedPrefsUtil.getToken();
        int totalMembers = members.size();
        final int[] successCount = {0};
        final int[] failCount = {0};

        for (MemberItem member : members) {
            Long blindId = member.blindId;
            Long volunteerId = member.volunteerId;
            
            apiService.removeUserFromFamily(token, currentFamily.getFamilyId(), blindId, volunteerId)
                    .enqueue(new ApiCallback<Boolean>(requireContext()) {
                            @Override
                        public void onSuccess(Boolean data) {
                            if (data) {
                                successCount[0]++;
                            } else {
                                failCount[0]++;
                            }
                            
                            // 当所有请求完成时
                            if (successCount[0] + failCount[0] == totalMembers) {
                                String message = String.format("成功移除%d个成员", successCount[0]);
                                if (failCount[0] > 0) {
                                    message += String.format("，%d个成员移除失败", failCount[0]);
                                }
                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                
                                // 刷新家庭信息
                                getFamilyInfo(currentFamily.getFamilyId());
                            }
                        }
                    });
        }
    }

    private static class MemberItem {
        Long blindId;
        Long volunteerId;
        String name;
        String type;
        boolean isSelected;

        MemberItem(Long blindId, Long volunteerId, String name, String type) {
            this.blindId = blindId;
            this.volunteerId = volunteerId;
            this.name = name;
            this.type = type;
            this.isSelected = false;
        }
    }

    private class RemoveMemberAdapter extends RecyclerView.Adapter<RemoveMemberAdapter.ViewHolder> {
        private List<MemberItem> members = new ArrayList<>();

        void setMembers(List<MemberItem> members) {
            this.members = members;
            notifyDataSetChanged();
        }

        List<MemberItem> getSelectedMembers() {
            return members.stream()
                    .filter(member -> member.isSelected)
                    .collect(Collectors.toList());
        }

        @NonNull
                            @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_remove_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MemberItem member = members.get(position);
            holder.tvName.setText(member.name);
            holder.tvType.setText(member.type);
            holder.checkbox.setChecked(member.isSelected);
            
            holder.itemView.setOnClickListener(v -> {
                member.isSelected = !member.isSelected;
                holder.checkbox.setChecked(member.isSelected);
            });
            
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                member.isSelected = isChecked;
            });
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvType;
            CheckBox checkbox;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvType = itemView.findViewById(R.id.tvType);
                checkbox = itemView.findViewById(R.id.checkbox);
                            }
        }
    }

    private void showLeaveFamilyDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出家庭")
                .setMessage("确定要退出当前家庭吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    leaveFamily();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        }

    private void leaveFamily() {
        String token = "Bearer " + SharedPrefsUtil.getToken();
        apiService.leaveFamily(token).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean data) {
                if (data) {
                    Toast.makeText(requireContext(), "已退出家庭", Toast.LENGTH_SHORT).show();
                    // 刷新页面状态
                    checkFamilyStatus();
                } else {
                    Toast.makeText(requireContext(), "退出家庭失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面恢复时都检查家庭状态
        checkFamilyStatus();
    }

    private boolean isCreator() {
        Long currentUserId = SharedPrefsUtil.getUserId();
        return currentFamily != null && 
               currentFamily.getCreatorVolunteer() != null && 
               currentUserId != null && 
               currentUserId.equals(currentFamily.getCreatorVolunteer().getVolunteerId());
    }
} 