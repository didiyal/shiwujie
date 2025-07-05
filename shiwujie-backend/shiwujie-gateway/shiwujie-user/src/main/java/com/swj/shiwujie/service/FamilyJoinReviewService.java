package com.swj.shiwujie.service;

import com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO;
import com.swj.shiwujie.model.domain.FamilyJoinReview;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.request.user.familyJoinReview.FamilyJoinReviewUpdateRequest;

import java.util.List;

/**
* @author Administrator
* @description 针对表【FamilyJoinReview(家庭加入审核表)】的数据库操作Service
* @createDate 2025-07-01 00:21:42
*/
public interface FamilyJoinReviewService extends IService<FamilyJoinReview> {



    /**
     * 更新家庭信息
     * @param familyJoinReviewUpdateRequest 家庭更新内容
     * @param loginVolunteerId 操作人id
     * @param loginUserPhone 操作人手机号
     * @return 更新后脱敏后的家庭信息
     */
    boolean updateFamilyJoinReview(FamilyJoinReviewUpdateRequest familyJoinReviewUpdateRequest, Long loginVolunteerId,String loginUserPhone);


    /**
     * 获取待审核信息列表
     * @param loginVolunteerId 登录用户信息
     * @return 列表
     */
    List<FamilyJoinReviewVO> getFamilyJoinReviewVOList(Long loginVolunteerId);



    // region 工具方法

    /**
     * 审核信息脱敏
     * @param familyJoinReview 审核信息
     * @return 脱敏后的审核信息
     */
    FamilyJoinReviewVO getFamilyJoinReviewVO(FamilyJoinReview familyJoinReview);


    // endregion

}
