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
    private CardView cardFamilyRequests;
    private TextView tvFamilyName;
    private TextView tvFamilyDescription;
    private TextView tvCreator;
    private TextView tvFamilyId;
    private RecyclerView rvBlindMembers;
    private RecyclerView rvVolunteerMembers;
    private RecyclerView rvFamilyRequests;
    private Button btnJoinFamily;
    private Button btnCreateFamily;
    private Button btnEditFamily;
    private Button btnDeleteFamily;
    private Button btnRemoveMembers;
    private Button btnLeaveFamily;
    private ApiService apiService;
    private FamilyVO currentFamily;
    private List<FamilyJoinReviewVO> currentRequests;

    // 成员适配器
    private MemberAdapter blindMemberAdapter;
    private MemberAdapter volunteerMemberAdapter;
    private FamilyRequestAdapter familyRequestAdapter;

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
        cardFamilyRequests = root.findViewById(R.id.cardFamilyRequests);
        tvFamilyName = root.findViewById(R.id.tvFamilyName);
        tvFamilyDescription = root.findViewById(R.id.tvFamilyDescription);
        tvCreator = root.findViewById(R.id.tvCreator);
        tvFamilyId = root.findViewById(R.id.tvFamilyId);
        rvBlindMembers = root.findViewById(R.id.rvBlindMembers);
        rvVolunteerMembers = root.findViewById(R.id.rvVolunteerMembers);
        rvFamilyRequests = root.findViewById(R.id.rvFamilyRequests);
        btnJoinFamily = root.findViewById(R.id.btnJoinFamily);
        btnCreateFamily = root.findViewById(R.id.btnCreateFamily);
        btnEditFamily = root.findViewById(R.id.btnEditFamily);
        btnDeleteFamily = root.findViewById(R.id.btnDeleteFamily);
        btnRemoveMembers = root.findViewById(R.id.btnRemoveMembers);
        btnLeaveFamily = root.findViewById(R.id.btnLeaveFamily);

        // 设置RecyclerView的布局管理器
        rvBlindMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVolunteerMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFamilyRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 设置按钮点击事件
        btnJoinFamily.setOnClickListener(v -> showJoinFamilyDialog());
        btnCreateFamily.setOnClickListener(v -> showCreateFamilyDialog());
        btnEditFamily.setOnClickListener(v -> showEditFamilyDialog());
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
        blindMemberAdapter = new MemberAdapter("盲人");
        volunteerMemberAdapter = new MemberAdapter("志愿者");
        familyRequestAdapter = new FamilyRequestAdapter();
        
        rvBlindMembers.setAdapter(blindMemberAdapter);
        rvVolunteerMembers.setAdapter(volunteerMemberAdapter);
        rvFamilyRequests.setAdapter(familyRequestAdapter);
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
                    
                    // 检查是否是家主
                    Long currentUserId = SharedPrefsUtil.getUserId();
                    if (currentUserId != null && response.getCreatorVolunteer() != null && 
                        currentUserId.equals(response.getCreatorVolunteer().getVolunteerId())) {
                        // 是家主，获取申请列表
                            getFamilyJoinRequests();
                    } else {
                        cardFamilyRequests.setVisibility(View.GONE);
                    }
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

    private void getFamilyJoinRequests() {
        String token = SharedPrefsUtil.getToken();
        if (token == null) {
            return;
        }

        apiService.getFamilyJoinReviewVOList("Bearer " + token).enqueue(new ApiCallback<List<FamilyJoinReviewVO>>(requireContext()) {
            @Override
            public void onSuccess(List<FamilyJoinReviewVO> data) {
                if (data != null) {
                    // 只显示待审核的申请
                    List<FamilyJoinReviewVO> pendingRequests = data.stream()
                        .filter(request -> "待审核".equals(request.getReviewStatus()))
                        .collect(Collectors.toList());
                    
                    if (!pendingRequests.isEmpty()) {
                        cardFamilyRequests.setVisibility(View.VISIBLE);
                        familyRequestAdapter.updateRequests(pendingRequests);
                    } else {
                        cardFamilyRequests.setVisibility(View.GONE);
                    }
                } else {
                    cardFamilyRequests.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                cardFamilyRequests.setVisibility(View.GONE);
            }
        });
    }

    private void updateFamilyRequests() {
        if (currentRequests != null && !currentRequests.isEmpty()) {
            cardFamilyRequests.setVisibility(View.VISIBLE);
            familyRequestAdapter.updateRequests(currentRequests);
        } else {
            cardFamilyRequests.setVisibility(View.GONE);
        }
    }

    private void updateFamilyInfo(FamilyVO family) {
        android.util.Log.d("FamilyFragment", "开始更新UI,家庭ID: " + family.getFamilyId());
        currentFamily = family;
        
        // 确保在主线程中更新UI
        requireActivity().runOnUiThread(() -> {
            cardFamilyInfo.setVisibility(View.VISIBLE);
            cardEmptyState.setVisibility(View.GONE);

            // 更新家庭ID
            tvFamilyId.setText(String.format("家庭ID: %d", family.getFamilyId()));

            // 更新家庭基本信息
            tvFamilyName.setText(family.getFamilyName() != null ? family.getFamilyName() : "未命名家庭");
            tvFamilyDescription.setText(family.getFamilyDescription() != null ? family.getFamilyDescription() : "暂无描述");

            // 更新创建者信息
            VolunteerVO creator = family.getCreatorVolunteer();
            if (creator != null) {
                String creatorInfo = String.format("创建者: %s\nID: %d", 
                    creator.getName(), 
                    creator.getVolunteerId());
                tvCreator.setText(creatorInfo);

                // 检查当前用户是否是创建者
                Long currentUserId = SharedPrefsUtil.getUserId();
                btnEditFamily.setVisibility(currentUserId != null && currentUserId.equals(creator.getVolunteerId()) 
                    ? View.VISIBLE : View.GONE);
            } else {
                tvCreator.setText("创建者信息不可用");
                btnEditFamily.setVisibility(View.GONE);
            }

            // 检查是否是家主
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

            // 更新成员列表
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
        input.setInputType(InputType.TYPE_CLASS_NUMBER); // 只允许输入数字
        input.setHint("请输入家庭账号");

        // 创建对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("加入家庭")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String familyIdStr = input.getText().toString().trim();
                    if (familyIdStr.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入家庭账号", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        Long familyId = Long.parseLong(familyIdStr);
                        sendJoinFamilyRequest(familyId);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "请输入有效的家庭账号", Toast.LENGTH_SHORT).show();
                    }
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
                    Toast.makeText(requireContext(), "加入家庭申请已发送，等待家主审核", Toast.LENGTH_SHORT).show();
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

    private void showEditFamilyDialog() {
        if (currentFamily == null) {
            Toast.makeText(requireContext(), "家庭信息不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建输入框
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("请输入家庭描述");
        input.setText(currentFamily.getFamilyDescription());
        input.setMinLines(3);
        input.setMaxLines(5);

        // 设置输入框的布局参数
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50, 0, 50, 0);
        input.setLayoutParams(lp);

        // 创建对话框
        new AlertDialog.Builder(requireContext())
                .setTitle("修改家庭信息")
                .setView(input)
                .setPositiveButton("确定", (dialog, which) -> {
                    String description = input.getText().toString().trim();
                    if (description.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入家庭描述", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateFamilyDescription(description);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateFamilyDescription(String description) {
        String token = SharedPrefsUtil.getToken();
        if (token == null || currentFamily == null) {
            Toast.makeText(requireContext(), "用户信息无效，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("FamilyFragment", "开始更新家庭信息");
        apiService.updateFamily(
            "Bearer " + token, 
            currentFamily.getFamilyId(),
            currentFamily.getFamilyName(),
            description
        ).enqueue(new ApiCallback<Boolean>(requireContext()) {
            @Override
            public void onSuccess(Boolean data) {
                if (data) {
                    android.util.Log.d("FamilyFragment", "更新家庭信息成功");
                    Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show();
                    // 重新获取家庭信息以更新UI
                    getFamilyInfo(currentFamily.getFamilyId());
                } else {
                    android.util.Log.e("FamilyFragment", "更新家庭信息失败");
                    Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                android.util.Log.e("FamilyFragment", "更新家庭信息失败: " + message);
                Toast.makeText(requireContext(), "更新失败: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMemberLists(FamilyVO family) {
        List<MemberInfo> blindMembers = new ArrayList<>();
        if (family.getBlindVOList() != null) {
            for (BlindVO blind : family.getBlindVOList()) {
                if (blind != null && blind.getBlindId() != null && blind.getName() != null) {
                    blindMembers.add(new MemberInfo(blind.getBlindId(), blind.getName()));
                }
            }
        }
        blindMemberAdapter.updateMembers(blindMembers);

        List<MemberInfo> volunteerMembers = new ArrayList<>();
        if (family.getVolunteerVOList() != null) {
            VolunteerVO creator = family.getCreatorVolunteer();
            for (VolunteerVO volunteer : family.getVolunteerVOList()) {
                if (volunteer != null && volunteer.getVolunteerId() != null && 
                    volunteer.getName() != null && 
                    (creator == null || !volunteer.getVolunteerId().equals(creator.getVolunteerId()))) {
                    volunteerMembers.add(new MemberInfo(volunteer.getVolunteerId(), volunteer.getName()));
                }
            }
        }
        volunteerMemberAdapter.updateMembers(volunteerMembers);
        
        android.util.Log.d("FamilyFragment", "成员列表更新完成");
    }

    private static class MemberInfo {
        Long id;
        String name;

        MemberInfo(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {
        private List<MemberInfo> members = new ArrayList<>();
        private final String memberType; // "盲人" 或 "志愿者"

        MemberAdapter(String memberType) {
            this.memberType = memberType;
        }

        void updateMembers(List<MemberInfo> newMembers) {
            this.members = newMembers;
            notifyDataSetChanged();
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
            MemberInfo member = members.get(position);
            holder.tvMemberName.setText(member.name);
            holder.tvMemberId.setText("ID: " + member.id);
            holder.tvMemberRole.setText(memberType);
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

    // 家庭申请适配器
    private class FamilyRequestAdapter extends RecyclerView.Adapter<FamilyRequestAdapter.ViewHolder> {
        private List<FamilyJoinReviewVO> requests = new ArrayList<>();

        void updateRequests(List<FamilyJoinReviewVO> newRequests) {
            this.requests = new ArrayList<>(newRequests);
            notifyDataSetChanged();
        }

        void removeRequest(FamilyJoinReviewVO request) {
            int position = requests.indexOf(request);
            if (position != -1) {
                requests.remove(position);
                notifyItemRemoved(position);
                if (requests.isEmpty()) {
                    cardFamilyRequests.setVisibility(View.GONE);
                }
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_family_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FamilyJoinReviewVO request = requests.get(position);
            
            // 设置申请者信息
            holder.tvApplicantName.setText("申请者ID: " + request.getBlindId());
            holder.tvApplicantId.setText("申请者类型: 盲人");
            holder.tvApplyTime.setText(request.getApplyTime());

            // 设置按钮点击事件
            holder.btnApprove.setOnClickListener(v -> handleApprove(request));
            holder.btnReject.setOnClickListener(v -> handleReject(request));
        }

        @Override
        public int getItemCount() {
            return requests.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvApplicantName;
            TextView tvApplicantId;
            TextView tvApplyTime;
            Button btnApprove;
            Button btnReject;

            ViewHolder(View view) {
                super(view);
                tvApplicantName = view.findViewById(R.id.tvApplicantName);
                tvApplicantId = view.findViewById(R.id.tvApplicantId);
                tvApplyTime = view.findViewById(R.id.tvApplyTime);
                btnApprove = view.findViewById(R.id.btnApprove);
                btnReject = view.findViewById(R.id.btnReject);
            }
        }

        private void handleApprove(FamilyJoinReviewVO request) {
            String token = "Bearer " + SharedPrefsUtil.getToken();
                        Long currentUserId = SharedPrefsUtil.getUserId();
                        
            apiService.updateFamilyJoinReview(token, request.getReviewId(), true, currentUserId)
                    .enqueue(new ApiCallback<Boolean>(requireContext()) {
                            @Override
                            public void onSuccess(Boolean data) {
                            if (data) {
                                Toast.makeText(requireContext(), "已同意申请", Toast.LENGTH_SHORT).show();
                                // 立即从UI移除
                                familyRequestAdapter.removeRequest(request);
                                // 刷新家庭信息
                                getFamilyInfo(currentFamily.getFamilyId());
                            } else {
                                Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show();
                            }
                            }
                        });
        }

        private void handleReject(FamilyJoinReviewVO request) {
            String token = "Bearer " + SharedPrefsUtil.getToken();
                        Long currentUserId = SharedPrefsUtil.getUserId();
                        
            apiService.updateFamilyJoinReview(token, request.getReviewId(), false, currentUserId)
                    .enqueue(new ApiCallback<Boolean>(requireContext()) {
                            @Override
                            public void onSuccess(Boolean data) {
                            if (data) {
                                Toast.makeText(requireContext(), "已拒绝申请", Toast.LENGTH_SHORT).show();
                                // 立即从UI移除
                                familyRequestAdapter.removeRequest(request);
                            } else {
                                Toast.makeText(requireContext(), "操作失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
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