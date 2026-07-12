package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.CommunityMapper;
import com.swj.shiwujie.mapper.CommunityjoinreviewMapper;
import com.swj.shiwujie.model.VO.community.communityJoinReview.CommunityJoinReviewVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.community.Communityjoinreview;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.enums.community.CommunityReviewStatusEnum;
import com.swj.shiwujie.model.request.community.communityJoinReview.CommunityJoinReviewUpdateRequest;
import com.swj.shiwujie.service.CommunityjoinreviewService;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Administrator
 * @description 针对表【communityjoinreview(社区加入审核表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
public class CommunityjoinreviewServiceImpl extends ServiceImpl<CommunityjoinreviewMapper, Communityjoinreview> implements CommunityjoinreviewService {

    @Resource
    private CommunityMapper communityMapper;

    @Resource
    private InnerVolunteerService innerVolunteerService;

    @Resource
    private InnerBlindService innerBlindService;

    
    
    
    /**
     * 更新社区审核状态
     * @param updateRequest 审核更新请求
     * @param loginVolunteerId 管理员ID
     * @param loginUserPhone 管理员手机号
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateCommunityJoinReview(CommunityJoinReviewUpdateRequest updateRequest, Long loginVolunteerId, String loginUserPhone) {
        Long reviewId = updateRequest.getReviewId();
        ThrowUtils.throwIf(ObjUtil.isNull(reviewId), ErrorCode.PARAMS_ERROR, "审核ID不能为空");

        synchronized (loginUserPhone.intern()) {
            Communityjoinreview review = this.getById(reviewId);
            ThrowUtils.throwIf(ObjUtil.isNull(review), ErrorCode.PARAMS_ERROR, "审核记录不存在");

            Boolean reviewResult = updateRequest.getReviewResult();
            if (ObjUtil.isNull(reviewResult)) {
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "审核结果不能为空");
            }

            // 更新审核状态(成功)
            if (reviewResult) {
                review.setReviewStatus(CommunityReviewStatusEnum.PASSED.getReviewStatus());
                // 更新用户的社区ID
                Long communityId = review.getCommunityId();
                Long volunteerId = review.getVolunteerId();
                Long blindId = review.getBlindId();

                if (ObjUtil.isNotNull(volunteerId)) {
                    Volunteer volunteer = innerVolunteerService.getById(volunteerId);
                    volunteer.setCommunityId(communityId);
                    innerVolunteerService.updateById(volunteer);
                } else if (ObjUtil.isNotNull(blindId)) {
                    Blind blind = innerBlindService.getById(blindId);
                    blind.setCommunityId(communityId);
                    innerBlindService.updateById(blind);
                }
            } else {
                review.setReviewStatus(CommunityReviewStatusEnum.REJECTED.getReviewStatus());
            }
            review.setReviewerId(loginVolunteerId);
            review.setReviewTime(DateUtil.date());
            boolean updateResult = this.updateById(review);
            ThrowUtils.throwIf(!updateResult, ErrorCode.SYSTEM_ERROR, "审核状态更新失败");
        }

        return true;
    }



    /**
     * 获取社区审核列表
     * @param loginVolunteerId 管理员ID
     * @return 审核列表VO
     */
    @Override
    public List<CommunityJoinReviewVO> getCommunityJoinReviewVOList(Long loginVolunteerId) {
        // 查询管理员所在社区的审核记录
        Volunteer volunteer = innerVolunteerService.getById(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.PARAMS_ERROR, "用户不存在");
        Community community = communityMapper.selectOne(new QueryWrapper<Community>().eq("community_id", volunteer.getCommunityId()));
        ThrowUtils.throwIf(ObjUtil.isNull(community), ErrorCode.PARAMS_ERROR, "管理员未关联社区");

        List<Communityjoinreview> reviewList = this.list(new QueryWrapper<Communityjoinreview>()
                .eq("community_id", community.getCommunityId())
                .orderByAsc("review_status")
                .orderByDesc("apply_time"));

        List<CommunityJoinReviewVO> voList = new ArrayList<>();
        for (Communityjoinreview review : reviewList) {
            voList.add(getCommunityJoinReviewVO(review));
        }
        return voList;
    }



    /**
     * 根据ID获取社区审核详情
     * @param reviewId 审核ID
     * @return 审核详情VO
     */
    @Override
    public CommunityJoinReviewVO getCommunityJoinReviewVOById(Long reviewId) {
        ThrowUtils.throwIf(reviewId <= 0, ErrorCode.PARAMS_ERROR, "审核ID不合法");
        Communityjoinreview review = this.getById(reviewId);
        ThrowUtils.throwIf(ObjUtil.isNull(review), ErrorCode.PARAMS_ERROR, "审核记录不存在");
        return getCommunityJoinReviewVO(review);
    }

    //region 工具方法

    /**
     * 审核信息脱敏
     * @param communityjoinreview 审核记录
     * @return 脱敏后的VO
     */
    @Override
    public CommunityJoinReviewVO getCommunityJoinReviewVO(Communityjoinreview communityjoinreview) {
        CommunityJoinReviewVO vo = new CommunityJoinReviewVO();
        vo.setReviewId(communityjoinreview.getReviewId());
        vo.setCommunityId(communityjoinreview.getCommunityId());
        vo.setVolunteerId(communityjoinreview.getVolunteerId());
        vo.setBlindId(communityjoinreview.getBlindId());
        vo.setApplyTime(communityjoinreview.getApplyTime());
        vo.setReviewTime(communityjoinreview.getReviewTime());
        vo.setReviewerId(communityjoinreview.getReviewerId());

        // 转换审核状态为中文
        Integer reviewStatus = communityjoinreview.getReviewStatus();
        if (Objects.equals(reviewStatus, CommunityReviewStatusEnum.WAIT_REVIEW.getReviewStatus())) {
            vo.setReviewStatus(CommunityReviewStatusEnum.WAIT_REVIEW.getName());
        } else if (Objects.equals(reviewStatus, CommunityReviewStatusEnum.PASSED.getReviewStatus())) {
            vo.setReviewStatus(CommunityReviewStatusEnum.PASSED.getName());
        } else if (Objects.equals(reviewStatus, CommunityReviewStatusEnum.REJECTED.getReviewStatus())) {
            vo.setReviewStatus(CommunityReviewStatusEnum.REJECTED.getName());
        }

        return vo;
    }



    //endregion
}




