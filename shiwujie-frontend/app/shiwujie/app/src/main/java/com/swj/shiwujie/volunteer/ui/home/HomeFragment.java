package com.swj.shiwujie.volunteer.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.swj.shiwujie.R;
import com.swj.shiwujie.common.network.RetrofitClient;
import com.swj.shiwujie.common.network.ApiService;
import com.swj.shiwujie.common.service.FloatingWindowService;
import com.swj.shiwujie.common.network.WebSocketManager;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;
import com.swj.shiwujie.data.model.BaseResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 志愿者端主页：入队等待匹配 + 语言设置等。
 * 2026-09-16：紧急求助来电(3)/取消(4)/匹配成功(1)信令处理已提升到 VolunteerHomeActivity
 * （Activity 级监听，全 tab/后台可用）；本页面只保留主动入队逻辑。
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "VolunteerHomeFragment";

    private Button btnLearnCall;
    private WebSocketManager webSocketManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_volunteer_home, container, false);
        initViews(root);
        webSocketManager = WebSocketManager.getInstance();
        return root;
    }

    private void initViews(View root) {
        btnLearnCall = root.findViewById(R.id.btn_learn_call);
        btnLearnCall.setOnClickListener(v -> startVideoHelpMatching());
    }

    private void startVideoHelpMatching() {
        Log.d(TAG, "开始视频求助匹配（入队等待）");

        // 设置匹配状态到WebSocketManager
        webSocketManager.setMatchingStatus(true);

        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法进行匹配");
            Toast.makeText(requireContext(), "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            webSocketManager.setMatchingStatus(false);
            return;
        }

        // 发送志愿者创建视频求助请求（入队）
        ApiService apiService = RetrofitClient.getInstance().createService(ApiService.class);
        apiService.volunteerCreateVideohelp("Bearer " + token).enqueue(new Callback<BaseResponse<Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Response<BaseResponse<Boolean>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    BaseResponse<Boolean> result = response.body();
                    if (result.getCode() == 1 && Boolean.TRUE.equals(result.getData())) {
                        Log.d(TAG, "入队成功，开始等待匹配");
                        // 启动等待匹配悬浮窗（含取消入口）
                        startFloatingWindowService();
                    } else {
                        Log.e(TAG, "匹配失败 - 业务错误: " + result.getMessage());
                        Toast.makeText(requireContext(), "匹配失败: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                        webSocketManager.setMatchingStatus(false);
                    }
                } else {
                    Log.e(TAG, "HTTP请求失败 - 状态码: " + response.code());
                    Toast.makeText(requireContext(), "网络请求失败 - HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    webSocketManager.setMatchingStatus(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<BaseResponse<Boolean>> call, @NonNull Throwable t) {
                Log.e(TAG, "网络请求失败", t);
                Toast.makeText(requireContext(), "网络请求失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                webSocketManager.setMatchingStatus(false);
            }
        });
    }

    private void startFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().startService(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 2026-09-16：切换 tab 不停等待悬浮窗——志愿者入队后可长挂（队列 TTL 1 小时），
        // 悬浮窗取消/匹配成功分别由悬浮窗自身与 Activity 级信令处理
    }
}
