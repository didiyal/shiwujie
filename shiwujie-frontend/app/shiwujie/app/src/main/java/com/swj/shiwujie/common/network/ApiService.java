package com.swj.shiwujie.common.network;

import com.swj.shiwujie.data.model.BaseResponse;
import com.swj.shiwujie.data.model.BlindVO;
import com.swj.shiwujie.data.model.FamilyJoinReviewVO;
import com.swj.shiwujie.data.model.FamilyVO;
import com.swj.shiwujie.data.model.VolunteerVO;
import com.swj.shiwujie.data.model.CommunityJoinRequest;
import com.swj.shiwujie.data.model.CommunityVO;
import com.swj.shiwujie.data.model.BlindCommunityJoinRequest;
import com.swj.shiwujie.data.model.HelppostAddRequest;
import com.swj.shiwujie.data.model.HelppostVO;
import com.swj.shiwujie.data.model.ActivityVO;
import com.swj.shiwujie.data.model.ActivitysignVO;
import com.swj.shiwujie.data.model.ActivitySignAddRequest;
import com.swj.shiwujie.data.model.Page;
import com.swj.shiwujie.data.model.HelppostUpdateRequest;
import com.swj.shiwujie.data.model.AiChatRequest;
import com.swj.shiwujie.data.model.AiChatResponse;
import com.swj.shiwujie.data.model.ObstacleDetectionSessionResponse;
import com.swj.shiwujie.data.model.ObstacleDetectionResultResponse;
import com.swj.shiwujie.data.model.ObstacleDetectionHealthResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Body;
import retrofit2.http.Part;
import retrofit2.http.Multipart;
import okhttp3.RequestBody;

/**
 * API接口定义
 * 包含用户认证、信息获取等接口
 */
public interface ApiService {
    /**
     * 盲人用户一键登录接口
     * @param phone 手机号码
     * @return 登录成功后的用户信息，包含token等数据
     */
    @POST("/api/user/blind/login/loginAndRegisterQuickly")
    Call<BaseResponse<BlindVO>> blindQuickLogin(@Query("phone") String phone);

    /**
     * 盲人用户密码登录接口
     * @param phone    手机号码
     * @param password 密码
     * @return 登录成功后的用户信息，包含token等数据
     */
    @POST("/api/user/blind/login/loginAndRegister")
    Call<BaseResponse<BlindVO>> loginAndRegister(
            @Query("phone") String phone,
            @Query("password") String password
    );

    /**
     * 更新志愿者信息
     * @param token JWT令牌
     * @param volunteer 志愿者信息
     * @return 更新结果
     */
    @POST("/api/user/volunteer/update")
    Call<BaseResponse<Boolean>> updateVolunteerInfo(
            @Header("Authorization") String token,
            @Body VolunteerVO volunteer
    );

    /**
     * 检查登录状态接口
     * 响应码说明：
     * - 1: token有效
     * - 40010: token失效，需要重新登录
     * - 40000: 需要重新选择身份
     */
    @GET("/api/user/blind/login/check")
    Call<BaseResponse<Void>> checkLogin(@Header("Authorization") String token);

    /**
     * 退出登录接口
     * - 1: 退出成功
     * - 40010: token已失效
     */
    @GET("/api/user/blind/login/logout")
    Call<BaseResponse<Boolean>> logout(@Header("Authorization") String token);

    /**
     * 注销账户接口
     * 永久删除用户账号及相关数据
     *
     * @param "Bearer ${token}"
     * @param blindId 要删除的盲人用户ID
     * @return 注销结果
     * 响应码说明：
     * - 1: 注销成功
     * - 40010: token失效
     * - 40000: 用户不存在
     */
    @POST("/api/user/blind/delete")
    Call<BaseResponse<Boolean>> deleteBlindAccount(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId
    );

    /**
     * 获取盲人用户详细信息
     * @param blindId 盲人用户ID
     * @return 用户详细信息
     * 响应码说明：
     * - 1: 获取成功
     * - 40010: token失效
     * - 40000: 用户不存在
     */
    @GET("/api/user/blind/get/id/vo")
    Call<BaseResponse<BlindVO>> getBlindById(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId
    );

    /**
     * 获取家庭详细信息
     * @param token JWT令牌
     * @param familyId 家庭ID
     * @return 家庭详细信息
     * 响应码说明：
     * - 1: 获取成功
     * - 40010: token失效
     * - 40000: 家庭不存在
     */
    @GET("/api/user/family/get/id/vo")
    Call<BaseResponse<FamilyVO>> getFamilyVOById(
            @Header("Authorization") String token,
            @Query("familyId") Long familyId
    );

    @POST("/api/user/family/join")
    Call<BaseResponse<Boolean>> joinFamily(@Header("Authorization") String token, @Query("familyVolunteerPhone") String familyVolunteerPhone);

    /**
     * 创建家庭
     * @param token JWT令牌
     * @return 创建结果，包含创建的家庭信息
     * 响应码说明：
     * - 1: 创建成功
     * - 40010: token失效
     * - 40000: 创建失败
     */
    @GET("/api/user/family/add")
    Call<BaseResponse<FamilyVO>> createFamily(@Header("Authorization") String token);

    /**
     * 更新家庭信息
     * @param token JWT令牌
     * @param familyId 家庭ID
     * @param familyName 家庭名称
     * @param familyDescription 家庭描述
     * @return 更新结果
     */
    @PUT("/api/user/family/update")
    Call<BaseResponse<Boolean>> updateFamily(
            @Header("Authorization") String token,
            @Query("familyId") Long familyId,
            @Query("familyName") String familyName,
            @Query("familyDescription") String familyDescription
    );

    /**
     * 删除家庭
     * @param token JWT令牌
     * @return 删除结果
     * 响应码说明：
     * - 1: 删除成功
     * - 40010: token失效
     * - 40000: 家庭不存在
     */
    @DELETE("api/user/family/delete/family")
    Call<BaseResponse<Boolean>> deleteFamily(@Header("Authorization") String token);

    /**
     * 志愿者用户一键登录接口
     * @param phone 手机号码
     * @return 登录成功后的用户信息，包含token等数据
     */
    @POST("/api/user/volunteer/login/loginAndRegisterQuickly")
    Call<BaseResponse<VolunteerVO>> volunteerQuickLogin(@Query("phone") String phone);

    /**
     * 志愿者用户密码登录接口
     * @param phone    手机号码
     * @param password 密码
     * @return 登录成功后的用户信息，包含token等数据
     */
    @POST("/api/user/volunteer/login/loginAndRegister")
    Call<BaseResponse<VolunteerVO>> volunteerLoginAndRegister(
            @Query("phone") String phone,
            @Query("password") String password
    );

    /**
     * 检查志愿者登录状态接口
     * 响应码说明：
     * - 1: token有效
     * - 40010: token失效，需要重新登录
     * - 40000: 需要重新选择身份
     */
    @GET("/api/user/volunteer/login/check")
    Call<BaseResponse<Void>> checkVolunteerLogin(@Header("Authorization") String token);

    /**
     * 获取志愿者用户详细信息
     * @param volunteerId 志愿者用户ID
     * @return 用户详细信息
     * 响应码说明：
     * - 1: 获取成功
     * - 40010: token失效
     * - 40000: 用户不存在
     */
    @GET("/api/user/volunteer/get/id/vo")
    Call<BaseResponse<VolunteerVO>> getVolunteerVOById(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId
    );

    /**
     * 注销志愿者账户接口
     * 永久删除志愿者账号及相关数据
     *
     * @param token "Bearer ${token}"
     * @param volunteerId 要删除的志愿者用户ID
     * @return 注销结果
     * 响应码说明：
     * - 1: 注销成功
     * - 40010: token失效
     * - 40000: 用户不存在
     */
    @POST("/api/user/volunteer/delete")
    Call<BaseResponse<Boolean>> deleteVolunteer(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId
    );

    /**
     * 志愿者退出登录接口
     * - 1: 退出成功
     * - 40010: token已失效
     */
    @GET("/api/user/volunteer/login/logout")
    Call<BaseResponse<Boolean>> volunteerLogout(@Header("Authorization") String token);

    @GET("/api/user/familyJoinReview/get/list/vo")
    Call<BaseResponse<List<FamilyJoinReviewVO>>> getFamilyJoinReviewVOList(@Header("Authorization") String token);

    /**
     * 更新家庭加入申请的审核状态
     * @param token JWT令牌
     * @param reviewId 审核ID
     * @param reviewResult 审核结果（true为同意，false为拒绝）
     * @param reviewerId 审核者ID
     * @return 更新结果
     */
    @PUT("/api/user/familyJoinReview/update")
    Call<BaseResponse<Boolean>> updateFamilyJoinReview(
            @Header("Authorization") String token,
            @Query("reviewId") Long reviewId,
            @Query("reviewResult") Boolean reviewResult,
            @Query("reviewerId") Long reviewerId
    );

    /**
     * 获取家庭加入申请详情
     * @param token JWT令牌
     * @param reviewId 申请记录ID
     * @return 申请详情
     * 响应码说明：
     * - 1: 获取成功
     * - 40010: token失效
     * - 40000: 记录不存在
     */
    @GET("/api/user/familyJoinReview/get/id/vo")
    Call<BaseResponse<FamilyJoinReviewVO>> getFamilyJoinReviewVOById(
            @Header("Authorization") String token,
            @Query("reviewId") Long reviewId
    );

    @DELETE("/api/user/family/delete/user")
    Call<BaseResponse<Boolean>> removeUserFromFamily(
            @Header("Authorization") String token,
            @Query("familyId") Long familyId,
            @Query("blindId") Long blindId,
            @Query("volunteerId") Long volunteerId
    );

    @DELETE("/api/user/family/delete/leave")
    Call<BaseResponse<Boolean>> leaveFamily(@Header("Authorization") String token);

    @POST("/api/user/volunteer/update/password")
    Call<BaseResponse<Boolean>> updateVolunteerPassword(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId,
            @Query("originPassword") String originPassword,
            @Query("newPassword") String newPassword
    );

    @POST("/api/user/volunteer/update/phone")
    Call<BaseResponse<Boolean>> updateVolunteerPhone(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId,
            @Query("phone") String phone
    );

    @PUT("/api/user/blind/update")
    Call<BaseResponse<Boolean>> updateBlindInfo(
            @Header("Authorization") String token,
            @Body BlindVO blind
    );

    @POST("/api/user/blind/update/password")
    Call<BaseResponse<Boolean>> updateBlindPassword(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId,
            @Query("originPassword") String originPassword,
            @Query("newPassword") String newPassword
    );

    @POST("/api/user/blind/update/phone")
    Call<BaseResponse<Boolean>> updateBlindPhone(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId,
            @Query("phone") String phone
    );

    // ==================== 视频求助相关接口 ====================

    /**
     * 盲人加入视频求助匹配
     * @param token JWT令牌
     * @return 加入匹配结果
     */
    @GET("/api/call/videohelp/blind/join")
    Call<BaseResponse<Boolean>> blindJoinVideohelp(@Header("Authorization") String token);

    /**
     * 盲人上传录屏路径
     * @param token JWT令牌
     * @param videoPath 录屏文件路径
     * @return 上传结果
     */
    @POST("/api/call/videohelp/join")
    Call<BaseResponse<Boolean>> blindUpdateVideoPath(
            @Header("Authorization") String token,
            @Query("videoPath") String videoPath
    );

    /**
     * 挂断视频通话
     * @param token JWT令牌
     * @return 挂断结果
     */
    @DELETE("/api/call/videohelp/delete/leave")
    Call<BaseResponse<Boolean>> hangupVideohelp(@Header("Authorization") String token);

    /**
     * 志愿者加入视频求助匹配
     * @param token JWT令牌
     * @return 加入匹配结果
     */
    @GET("/api/call/videohelp/volunteer/add")
    Call<BaseResponse<Boolean>> volunteerCreateVideohelp(@Header("Authorization") String token);

    /**
     * 志愿者取消视频求助匹配
     * @param token JWT令牌
     * @return 取消匹配结果
     */
    @DELETE("/api/call/videohelp/volunteer/delete")
    Call<BaseResponse<Boolean>> volunteerLeaveVideohelp(@Header("Authorization") String token);

    // ==================== 紧急求助相关接口 ====================

    /**
     * 盲人紧急求助
     * @param token JWT令牌
     * @return 求助结果
     */
    @GET("/api/call/urgenthelp/blind/add")
    Call<BaseResponse<Boolean>> blindCreateUrgenthelp(@Header("Authorization") String token);

    /**
     * 盲人取消求助
     * @param token JWT令牌
     * @return 取消结果
     */
    @DELETE("/api/call/urgenthelp/blind/delete")
    Call<BaseResponse<Boolean>> blindLeaveUrgenthelp(@Header("Authorization") String token);

    /**
     * 挂断紧急求助视频通话
     * @param token JWT令牌
     * @return 挂断结果
     */
    @DELETE("/api/call/urgenthelp/delete/leave")
    Call<BaseResponse<Boolean>> hangupUrgenthelp(@Header("Authorization") String token);

    /**
     * 家属回应求助
     * @param token JWT令牌
     * @param blindPhone 求助盲人手机号
     * @return 回应结果
     */
    @GET("/api/call/urgenthelp/volunteer/join")
    Call<BaseResponse<Boolean>> familyJoinUrgenthelp(
            @Header("Authorization") String token,
            @Query("blindPhone") String blindPhone
    );

    // ==================== 社区相关接口 ====================

    /**
     * 志愿者加入社区
     * @param token JWT令牌
     * @param familyVolunteerPhone 家主手机号
     * @return 加入结果
     */
    @POST("/api/user/volunteer/community/join")
    Call<BaseResponse<Boolean>> volunteerJoinCommunity(
            @Header("Authorization") String token,
            @Body CommunityJoinRequest request
    );

    /**
     * 根据ID查询社区信息
     * @param token JWT令牌
     * @param communityId 社区ID
     * @return 社区信息
     */
    @GET("/api/community/community/get/id/vo")
    Call<BaseResponse<CommunityVO>> getCommunityById(
            @Header("Authorization") String token,
            @Query("communityId") Long communityId
    );

    /**
     * 盲人加入社区
     * @param token JWT令牌
     * @param request 加入社区请求
     * @return 加入结果
     */
    @POST("/api/user/blind/community/join")
    Call<BaseResponse<Boolean>> blindJoinCommunity(
            @Header("Authorization") String token,
            @Body BlindCommunityJoinRequest request
    );

    // ==================== 求助帖相关接口 ====================

    /**
     * 视障人士发出求助帖
     * @param token JWT令牌
     * @param request 求助帖创建请求
     * @return 求助帖信息
     */
    @POST("/api/community/helppost/add")
    Call<BaseResponse<HelppostVO>> addHelppost(
            @Header("Authorization") String token,
            @Body HelppostAddRequest request
    );

    /**
     * 根据ID查询求助帖信息
     * @param token JWT令牌
     * @param helppostId 求助帖ID
     * @return 求助帖信息
     */
    @GET("/api/community/helppost/get/id/vo")
    Call<BaseResponse<HelppostVO>> getHelppostById(
            @Header("Authorization") String token,
            @Query("helppostId") Long helppostId
    );

    /**
     * 查询盲人的求助帖信息
     * @param token JWT令牌
     * @param helppostId 求助帖ID
     * @return 求助帖信息
     */
    @GET("/api/community/helppost/get")
    Call<BaseResponse<HelppostVO>> getBlindHelppostInfo(
            @Header("Authorization") String token,
            @Query("helppostId") Long helppostId
    );

    /**
     * 获取求助帖列表
     * @param token JWT令牌
     * @param communityId 社区ID
     * @return 求助帖列表
     */
    @GET("/api/community/helppost/list")
    Call<BaseResponse<Page<HelppostVO>>> getHelppostList(
            @Header("Authorization") String token,
            @Query("communityId") Long communityId
    );

    /**
     * 更新求助帖（志愿者接受求助）
     * @param token JWT令牌
     * @param request 求助帖更新请求
     * @return 更新结果
     */
    @POST("/api/community/helppost/update")
    Call<BaseResponse<Boolean>> updateHelppost(
            @Header("Authorization") String token,
            @Body HelppostUpdateRequest request
    );

    /**
     * 获取志愿者接受的求助帖列表
     * @param token JWT令牌
     * @param volunteerId 志愿者ID
     * @param communityId 社区ID
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 求助帖列表
     */
    @GET("/api/community/helppost/list")
    Call<BaseResponse<Page<HelppostVO>>> getVolunteerHelpposts(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId,
            @Query("communityId") Long communityId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize
    );

    /**
     * 获取盲人发布的求助帖列表
     * @param token JWT令牌
     * @param blindId 盲人ID
     * @param communityId 社区ID
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 求助帖列表
     */
    @GET("/api/community/helppost/list")
    Call<BaseResponse<Page<HelppostVO>>> getBlindHelpposts(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId,
            @Query("communityId") Long communityId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize
    );

    // ==================== 活动相关接口 ====================

    /**
     * 分页查询社区下的活动列表
     * @param token JWT令牌
     * @param communityId 社区ID
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param activityStatus 活动状态（可选）
     * @return 活动列表
     */
    @GET("/api/community/activity/list/vo")
    Call<BaseResponse<Page<ActivityVO>>> getActivityList(
            @Header("Authorization") String token,
            @Query("communityId") Long communityId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize,
            @Query("activityStatus") String activityStatus
    );


    /**
     * 根据ID查询活动详情
     * @param token JWT令牌
     * @param activityId 活动ID
     * @return 活动详情
     */
    @GET("/api/community/activity/get")
    Call<BaseResponse<ActivityVO>> getActivityById(
            @Header("Authorization") String token,
            @Query("activityId") Long activityId
    );

    /**
     * 活动报名
     * @param token JWT令牌
     * @param request 活动报名请求
     * @return 报名结果
     */
    @POST("/api/community/activitysign/add")
    Call<BaseResponse<Boolean>> addActivitySign(
            @Header("Authorization") String token,
            @Body ActivitySignAddRequest request
    );

    /**
     * 查询用户报名的活动列表
     * @param token JWT令牌
     * @param activityId 活动ID（可选）
     * @param blindId 盲人ID（可选）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param volunteerId 志愿者ID（可选）
     * @return 活动报名列表
     */
    @GET("/api/community/activitysign/list/page/vo")
    Call<BaseResponse<Page<ActivityVO>>> getActivitySignList(
            @Header("Authorization") String token,
            @Query("activityId") Long activityId,
            @Query("blindId") Long blindId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize,
            @Query("volunteerId") Long volunteerId
    );

    /**
     * 查询志愿者报名的活动列表（只传递volunteerId）
     * @param token JWT令牌
     * @param current 当前页码
     * @param pageSize 每页大小
     * @param volunteerId 志愿者ID
     * @return 活动报名列表
     */
    @GET("/api/community/activitysign/list/page/vo")
    Call<BaseResponse<Page<ActivitysignVO>>> getActivitySignListByVolunteer(
            @Header("Authorization") String token,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize,
            @Query("volunteerId") Long volunteerId
    );

    /**
     * 获取志愿者的活动列表
     * @param token JWT令牌
     * @param volunteerId 志愿者ID
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 活动列表
     */
    @GET("/api/community/activitysign/list/page/vo")
    Call<BaseResponse<Page<ActivityVO>>> getVolunteerActivities(
            @Header("Authorization") String token,
            @Query("volunteerId") Long volunteerId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize
    );

    /**
     * 获取盲人参与的活动列表
     * @param token JWT令牌
     * @param blindId 盲人ID
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 活动列表
     */
    @GET("/api/community/activitysign/list/page/vo")
    Call<BaseResponse<Page<ActivityVO>>> getBlindActivities(
            @Header("Authorization") String token,
            @Query("blindId") Long blindId,
            @Query("current") Long current,
            @Query("pageSize") Long pageSize
    );

    /**
     * AI文本对话接口
     * @param token JWT令牌
     * @param text 用户输入的文本
     * @return AI对话响应（流式）
     */
    @POST("/api/ai/ai/doChatByText")
    Call<okhttp3.ResponseBody> sendAiTextMessage(
            @Header("Authorization") String token,
            @Query("text") String text
    );

    /**
     * AI图片识别接口
     * @param token JWT令牌
     * @param imageFile 图片文件
     * @return AI图片识别响应（流式）
     */
    @Multipart
    @POST("/api/ai/ai/doChatByImage")
    Call<okhttp3.ResponseBody> sendAiImageMessage(
            @Header("Authorization") String token,
            @Part okhttp3.MultipartBody.Part imageFile
    );

    // ===== 障碍物检测接口 - 严格按照backend_service.py的API接口实现 =====
    // 改造说明：将Python后端的图像处理API转换为Android原生接口
    
    /**
     * 开始障碍物检测会话 - 对应原Python代码的start_session接口
     * 对应原代码：@app.route('/api/start_session', methods=['POST'])
     * @return 会话创建响应（简单JSON字符串）
     */
    @POST("api/start_session")
    Call<String> startObstacleDetectionSession();

    /**
     * 处理单帧图像进行障碍物检测 - 对应原Python代码的process_frame接口
     * 对应原代码：@app.route('/api/process_frame', methods=['POST'])
     * @param requestBody 包含session_id和image的JSON请求体
     * @return 检测结果响应（简单JSON字符串）
     */
    @POST("api/process_frame")
    Call<String> processFrameForObstacleDetection(@Body RequestBody requestBody);

    /**
     * 健康检查接口 - 对应原Python代码的health_check接口
     * 对应原代码：@app.route('/health', methods=['GET'])
     * @return 健康状态响应
     */
    @GET("health")
    Call<ObstacleDetectionHealthResponse> getObstacleDetectionHealthStatus();

    /**
     * 简单健康检查接口 - 对应原Python代码的health_check_simple接口
     * 对应原代码：@app.route('/health/simple', methods=['GET'])
     * @return 简单健康状态
     */
    @GET("health/simple")
    Call<String> getObstacleDetectionHealthStatusSimple();

}