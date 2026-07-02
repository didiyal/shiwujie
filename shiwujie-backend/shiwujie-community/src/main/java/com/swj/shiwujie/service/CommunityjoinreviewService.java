package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.community.communityJoinReview.CommunityJoinReviewVO;
import com.swj.shiwujie.model.domain.community.Communityjoinreview;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.community.communityJoinReview.CommunityJoinReviewUpdateRequest;

import java.util.List;


/**
* @author Administrator
* @description 针对表【CommunityJoinReview(社区加入审核表)】的数据库操作Service
* @createDate 2025-07-25 18:15:48
*/
public interface CommunityjoinreviewService extends IService<Communityjoinreview> {

    /**
     * 更新社区审核状态
     * @param updateRequest 审核更新请求
     * @param loginVolunteerId 管理员ID
     * @param loginUserPhone 管理员手机号
     * @return 是否成功
     */
    boolean updateCommunityJoinReview(CommunityJoinReviewUpdateRequest updateRequest, Long loginVolunteerId, String loginUserPhone);

    /**
     * 获取社区审核列表
     * @param loginVolunteerId 管理员ID
     * @return 审核列表VO
     */
    List<CommunityJoinReviewVO> getCommunityJoinReviewVOList(Long loginVolunteerId);

    /**
     * 根据ID获取社区审核详情
     * @param reviewId 审核ID
     * @return 审核详情VO
     */
    CommunityJoinReviewVO getCommunityJoinReviewVOById(Long reviewId);





    //region 工具方法

    /**
     * 审核信息脱敏
     * @param communityjoinreview 审核记录
     * @return 脱敏后的VO
     */
    CommunityJoinReviewVO getCommunityJoinReviewVO(Communityjoinreview communityjoinreview);


    //endregion

}
