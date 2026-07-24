package com.swj.shiwujie.common.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.EmergencyHelpData;
import com.swj.shiwujie.data.model.SocketDataV0;
import com.swj.shiwujie.common.utils.SharedPrefsUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 紧急求助管理器
 * 专门管理紧急求助的状态和流程
 * 与现有的VideoCallManager完全独立
 */
public class EmergencyHelpManager {
    private static final String TAG = "EmergencyHelpManager";

    /** 家属无响应超时：超过该时长未接听则自动复位状态，避免 isInEmergencyHelp 永真导致用户无法再次求助 */
    private static final long EMERGENCY_HELP_TIMEOUT_MS = 60_000L; // 60秒

    private static EmergencyHelpManager instance;
    private Context context;
    private Handler mainHandler;

    // 当前紧急求助状态
    private EmergencyHelpData currentHelpData;
    private boolean isInEmergencyHelp = false;

    // 超时兜底任务（复用 mainHandler，主线程触发）
    private Runnable emergencyTimeoutRunnable;
    
    // 回调接口
    public interface EmergencyHelpCallback {
        void onHelpRequestSuccess();
        void onHelpRequestFailed(String error);
        void onHelpCancelled();
        void onHelpResponseSuccess();
        void onHelpResponseFailed(String error);
        void onHelpHangupSuccess();
        void onHelpHangupFailed(String error);
        /** 家属在超时窗口内未响应，状态已自动复位（用户可再次发起求助） */
        default void onHelpTimeout() {}
    }

    /**
     * chunk-2e-4 gate ③：紧急确认 token 消费回调（独立于通话生命周期回调）。
     * 盲人 App 屏幕显式确认 → 命中后端 /blind/confirm → 后端消费 token + 推 5003。
     */
    public interface EmergencyTokenConfirmCallback {
        void onConfirmSuccess();
        void onConfirmFailed(String reason);
    }
    
    private EmergencyHelpCallback callback;
    
    private EmergencyHelpManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized EmergencyHelpManager getInstance() {
        if (instance == null) {
            instance = new EmergencyHelpManager();
        }
        return instance;
    }
    
    /**
     * 设置上下文
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
    /**
     * 设置回调接口
     */
    public void setCallback(EmergencyHelpCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        if (context != null) {
            mainHandler.post(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * chunk-2e-4 gate ③：消费 AI {@code request_emergency_help_prepare} 签发的紧急确认 token（非-MCP HTTP 端点）。
     *
     * <p>盲人 App 屏幕显式确认 → 命中后端 {@code POST /api/call/urgenthelp/blind/confirm} → 后端
     * {@code consumeByApp} 一次性消费 token + 推 WS 5003（既有的 blindhome 跳转 + 开启紧急求助 UI
     * 由 5003 触发，不在本方法内）。人工确认超越轮次闸（gate ②），后端不查 same-turn。</p>
     *
     * <p>与 {@link #requestEmergencyHelp} 的通话生命周期正交：本方法不建 Urgenthelp 记录、不动
     * {@code isInEmergencyHelp} 状态——仅消费 token 触发 5003，后续接听/挂断仍走既有 create/leave/hangup。</p>
     *
     * @param emergencyToken AI prepare() 经 114 帧下发、App 回传的确认码
     * @param cb             成功/失败回调（主线程回调：Callback.onResponse 在 Retrofit 内部已切主线程，无需 mainHandler）
     */
    public void confirmEmergencyToken(String emergencyToken, EmergencyTokenConfirmCallback cb) {
        if (emergencyToken == null || emergencyToken.trim().isEmpty()) {
            Log.w(TAG, "gate ③ 紧急确认：token 为空，忽略");
            if (cb != null) cb.onConfirmFailed("确认码为空");
            return;
        }
        String jwt = SharedPrefsUtil.getToken();
        if (jwt == null || jwt.isEmpty()) {
            Log.e(TAG, "gate ③ 紧急确认：JWT 为空");
            if (cb != null) cb.onConfirmFailed("登录状态异常，请重新登录");
            return;
        }
        Log.d(TAG, "=== gate ③ 紧急确认 token 消费 ===");
        RetrofitClient.getInstance().createService(ApiService.class)
                .blindConfirmUrgenthelp("Bearer " + jwt, emergencyToken)
                .enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        Log.d(TAG, "紧急确认响应: " + response.code());
                        BaseResponse<Boolean> body = response.body();
                        if (response.isSuccessful() && body != null
                                && body.getCode() == 1 && Boolean.TRUE.equals(body.getData())) {
                            Log.d(TAG, "gate ③ 紧急确认成功，后端已推 5003");
                            if (cb != null) cb.onConfirmSuccess();
                        } else {
                            String msg = body != null ? body.getMessage() : "确认失败";
                            Log.w(TAG, "gate ③ 紧急确认失败: " + msg);
                            if (cb != null) cb.onConfirmFailed(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Log.e(TAG, "gate ③ 紧急确认网络异常", t);
                        if (cb != null) cb.onConfirmFailed("网络异常，请重试");
                    }
                });
    }
    
    /**
     * 检查是否在紧急求助中
     */
    public boolean isInEmergencyHelp() {
        return isInEmergencyHelp;
    }
    
    /**
     * 盲人发起紧急求助
     */
    public void requestEmergencyHelp(String blindPhone) {
        Log.d(TAG, "=== 盲人发起紧急求助 ===");
        Log.d(TAG, "盲人手机号: " + blindPhone);
        
        if (isInEmergencyHelp) {
            Log.w(TAG, "已在紧急求助中，忽略重复请求");
            return;
        }
        
        // 创建紧急求助数据
        currentHelpData = EmergencyHelpData.createHelpRequest(blindPhone);
        isInEmergencyHelp = true;
        
        // 获取token
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法发起紧急求助");
            showToast("登录状态异常，请重新登录");
            if (callback != null) {
                callback.onHelpRequestFailed("登录状态异常");
            }
            return;
        }
        
        // 发送HTTP请求
        RetrofitClient.getInstance().createService(ApiService.class).blindCreateUrgenthelp("Bearer " + token)
                .enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        Log.d(TAG, "紧急求助请求响应: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Boolean> baseResponse = response.body();
                            Log.d(TAG, "响应数据: " + baseResponse.toString());
                            
                            if (baseResponse.getCode() == 1 && Boolean.TRUE.equals(baseResponse.getData())) {
                                Log.d(TAG, "紧急求助请求成功");
                            /*    showToast("紧急求助请求已发送");*/
                                
                                if (callback != null) {
                                    callback.onHelpRequestSuccess();
                                }
                                // 求助已成功发出，启动"家属无响应"超时兜底
                                scheduleEmergencyTimeout();
                            } else {
                                Log.e(TAG, "紧急求助请求失败: " + baseResponse.getMessage());
                              /*  showToast("紧急求助请求失败: " + baseResponse.getMessage());*/
                                
                                // 重置状态
                                currentHelpData = null;
                                isInEmergencyHelp = false;
                                
                                if (callback != null) {
                                    callback.onHelpRequestFailed(baseResponse.getMessage());
                                }
                            }
                        } else {
                            Log.e(TAG, "紧急求助请求失败，响应码: " + response.code());
                          /*  showToast("网络请求失败，请检查网络连接");*/
                            
                            // 重置状态
                            currentHelpData = null;
                            isInEmergencyHelp = false;
                            
                            if (callback != null) {
                                callback.onHelpRequestFailed("网络请求失败");
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Log.e(TAG, "紧急求助请求异常", t);
                     /*   showToast("网络请求异常: " + t.getMessage());*/
                        
                        // 重置状态
                        currentHelpData = null;
                        isInEmergencyHelp = false;
                        
                        if (callback != null) {
                            callback.onHelpRequestFailed("网络请求异常: " + t.getMessage());
                        }
                    }
                });
    }
    
    /**
     * 盲人取消紧急求助
     */
    public void cancelEmergencyHelp() {
        Log.d(TAG, "=== 盲人取消紧急求助 ===");
        
        if (!isInEmergencyHelp || currentHelpData == null) {
            Log.w(TAG, "当前不在紧急求助中，忽略取消请求");
            return;
        }
        
        // 获取token
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法取消紧急求助");
           /* showToast("登录状态异常，请重新登录");*/
            return;
        }
        
        // 发送HTTP请求
        RetrofitClient.getInstance().createService(ApiService.class).blindLeaveUrgenthelp("Bearer " + token)
                .enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        Log.d(TAG, "取消紧急求助响应: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Boolean> baseResponse = response.body();
                            Log.d(TAG, "响应数据: " + baseResponse.toString());
                            
                            if (baseResponse.getCode() == 1 && Boolean.TRUE.equals(baseResponse.getData())) {
                                Log.d(TAG, "取消紧急求助成功");
                                showToast("紧急求助已取消");

                                cancelEmergencyTimeout();
                                
                                // 重置状态，允许重新发起求助
                                currentHelpData = null;
                                isInEmergencyHelp = false;
                                
                                if (callback != null) {
                                    callback.onHelpCancelled();
                                }
                            } else {
                                Log.e(TAG, "取消紧急求助失败: " + baseResponse.getMessage());
                               /* showToast("取消紧急求助失败: " + baseResponse.getMessage());*/
                            }
                        } else {
                            Log.e(TAG, "取消紧急求助失败，响应码: " + response.code());
                            /*showToast("网络请求失败，请检查网络连接");*/
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Log.e(TAG, "取消紧急求助异常", t);
                       /* showToast("网络请求异常: " + t.getMessage());*/
                    }
                });
    }
    
    /**
     * 挂断紧急求助视频通话
     */
    public void hangupEmergencyHelp() {
        Log.d(TAG, "=== 挂断紧急求助视频通话 ===");
        
        // 获取token
        String token = SharedPrefsUtil.getToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token为空，无法挂断紧急求助");
            showToast("登录状态异常，请重新登录");
            if (callback != null) {
                callback.onHelpHangupFailed("登录状态异常");
            }
            return;
        }
        
        // 发送HTTP请求
        RetrofitClient.getInstance().createService(ApiService.class).hangupUrgenthelp("Bearer " + token)
                .enqueue(new Callback<BaseResponse<Boolean>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Boolean>> call, Response<BaseResponse<Boolean>> response) {
                        Log.d(TAG, "挂断紧急求助响应: " + response.code());
                        
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Boolean> baseResponse = response.body();
                            Log.d(TAG, "响应数据: " + baseResponse.toString());
                            
                            if (baseResponse.getCode() == 1 && Boolean.TRUE.equals(baseResponse.getData())) {
                                Log.d(TAG, "挂断紧急求助成功");
                                showToast("通话已结束");

                                cancelEmergencyTimeout();
                                
                                // 更新状态
                                if (currentHelpData != null) {
                                    currentHelpData = EmergencyHelpData.createCallEnded(
                                            currentHelpData.getBlindPhone(),
                                            currentHelpData.getVolunteerPhone()
                                    );
                                }
                                isInEmergencyHelp = false;
                                
                                if (callback != null) {
                                    callback.onHelpHangupSuccess();
                                }
                            } else {
                                Log.e(TAG, "挂断紧急求助失败: " + baseResponse.getMessage());
                                showToast("挂断失败: " + baseResponse.getMessage());
                                
                                if (callback != null) {
                                    callback.onHelpHangupFailed(baseResponse.getMessage());
                                }
                            }
                        } else {
                            Log.e(TAG, "挂断紧急求助失败，响应码: " + response.code());
                            showToast("网络请求失败，请检查网络连接");
                            
                            if (callback != null) {
                                callback.onHelpHangupFailed("网络请求失败");
                            }
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<BaseResponse<Boolean>> call, Throwable t) {
                        Log.e(TAG, "挂断紧急求助异常", t);
                        showToast("网络请求异常: " + t.getMessage());
                        
                        if (callback != null) {
                            callback.onHelpHangupFailed("网络请求异常: " + t.getMessage());
                        }
                    }
                });
    }
    
    /**
     * 处理Socket消息
     * 当收到type=2消息时，表示家属视频初始化成功
     */
    public void handleSocketMessage(int requestType, String blindPhone, String volunteerPhone, long channelId) {
        Log.d(TAG, "=== 处理紧急求助Socket消息 ===");
        Log.d(TAG, "requestType: " + requestType + ", blindPhone: " + blindPhone + 
                ", volunteerPhone: " + volunteerPhone + ", channelId: " + channelId);
        
        if (requestType == SocketDataV0.REQUEST_TYPE_VIDEO_INIT) {
            // 家属视频初始化成功（type=2）→ 通话已建立，取消"家属无响应"超时
            Log.d(TAG, "收到家属视频初始化成功消息");
            cancelEmergencyTimeout();

            if (currentHelpData != null) {
                // 更新紧急求助数据
                currentHelpData.setVolunteerPhone(volunteerPhone);
                currentHelpData.setChannelId(channelId);
                currentHelpData.setStatus(2); // 通话中
                currentHelpData.setMessage("通话已开始");

                Log.d(TAG, "紧急求助状态已更新为通话中");
            }
        }
    }
    
    /**
     * 调度"家属无响应"超时任务。仅在求助成功发出后启用；任何终态（取消/挂断/响应/通话建立）都应取消。
     */
    private void scheduleEmergencyTimeout() {
        cancelEmergencyTimeout();
        emergencyTimeoutRunnable = () -> {
            Log.w(TAG, "=== 紧急求助超时，未收到家属响应，自动复位状态 ===");
            currentHelpData = null;
            isInEmergencyHelp = false;
            if (callback != null) {
                callback.onHelpTimeout();
            }
        };
        mainHandler.postDelayed(emergencyTimeoutRunnable, EMERGENCY_HELP_TIMEOUT_MS);
        Log.d(TAG, "已调度紧急求助超时任务: " + EMERGENCY_HELP_TIMEOUT_MS + "ms");
    }

    /**
     * 取消"家属无响应"超时任务
     */
    private void cancelEmergencyTimeout() {
        if (emergencyTimeoutRunnable != null && mainHandler != null) {
            mainHandler.removeCallbacks(emergencyTimeoutRunnable);
        }
        emergencyTimeoutRunnable = null;
    }

    /**
     * 重置紧急求助状态
     */
    public void resetEmergencyHelp() {
        Log.d(TAG, "=== 重置紧急求助状态 ===");
        cancelEmergencyTimeout();
        currentHelpData = null;
        isInEmergencyHelp = false;
    }
    
} 