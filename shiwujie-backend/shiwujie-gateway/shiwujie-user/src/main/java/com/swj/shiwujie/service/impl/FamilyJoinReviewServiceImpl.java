package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.FamilyJoinReviewMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.familyJoinReview.FamilyJoinReviewVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Family;
import com.swj.shiwujie.model.domain.user.FamilyJoinReview;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.user.FamilyReviewStatusEnum;
import com.swj.shiwujie.model.request.user.familyJoinReview.FamilyJoinReviewUpdateRequest;
import com.swj.shiwujie.service.FamilyJoinReviewService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Administrator
 * @description 针对表【FamilyJoinReview(家庭加入审核表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
public class FamilyJoinReviewServiceImpl extends ServiceImpl<FamilyJoinReviewMapper, FamilyJoinReview>
        implements FamilyJoinReviewService {

    @Resource
    private VolunteerMapper volunteerMapper;

    @Resource
    private BlindMapper blindMapper;


    /**
     * 更新审核信息
     *
     * @param familyJoinReviewUpdateRequest 审核更新内容
     * @param loginVolunteerId              操作人id
     * @return 更新后脱敏后的家庭信息
     */
    @Override
    public boolean updateFamilyJoinReview(FamilyJoinReviewUpdateRequest familyJoinReviewUpdateRequest, Long loginVolunteerId, String loginUserPhone) {

        // 只有家主可以审核
        Long reviewerId = familyJoinReviewUpdateRequest.getReviewerId();
        ThrowUtils.throwIf(ObjUtil.isNull(reviewerId), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!loginVolunteerId.equals(reviewerId)
                , ErrorCode.PARAMS_ERROR, "只有家主可以修改家庭信息");


        synchronized (loginUserPhone.intern()) {
            // 检查更新
            Long reviewId = familyJoinReviewUpdateRequest.getReviewId();
            ThrowUtils.throwIf(ObjUtil.isNull(reviewId), ErrorCode.PARAMS_ERROR);
            FamilyJoinReview review = this.getById(reviewId);

            Boolean reviewResult = familyJoinReviewUpdateRequest.getReviewResult();
            if (reviewResult) {
                // 成功
                review.setReviewStatus(FamilyReviewStatusEnum.PASSED.getReviewStatus());
                review.setReviewTime(DateUtil.date());
                // 用户设置家庭id字段
                Long blindId = review.getBlindId();
                Long volunteerId = review.getVolunteerId();
                Long familyId = review.getFamilyId();
                if (ObjUtil.isNotNull(blindId)) {
                    Blind blind = blindMapper.selectById(blindId);
                    blind.setFamilyId(familyId);
                    blindMapper.updateById(blind);
                } else if (ObjUtil.isNotNull(volunteerId)) {
                    Volunteer volunteer = volunteerMapper.selectById(volunteerId);
                    volunteer.setFamilyId(familyId);
                    volunteerMapper.updateById(volunteer);
                }
            } else {
                // 失败
                review.setReviewStatus(FamilyReviewStatusEnum.REJECTED.getReviewStatus());
                review.setReviewTime(DateUtil.date());
            }


            boolean b = this.updateById(review);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);
        }

        return true;

    }

    /**
     * 获取审核信息列表
     *
     * @param loginVolunteerId 登录用户信息
     * @return 列表
     */
    @Override
    public List<FamilyJoinReviewVO> getFamilyJoinReviewVOList(Long loginVolunteerId) {
        // 查询家庭信息,拿到家庭id
        Volunteer volunteer = volunteerMapper.selectById(loginVolunteerId);
        Long familyId = volunteer.getFamilyId();

        // 查询家庭id符合的申请表
        QueryWrapper<FamilyJoinReview> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("family_id", familyId);
        // 仅查找未审核的
//        queryWrapper.eq("review_status", FamilyReviewStatusEnum.WAIT_REVIEW.getReviewStatus());
        queryWrapper.orderByAsc("review_status");
        List<FamilyJoinReview> familyJoinReviewList = this.list(queryWrapper);

        // 返回
        List<FamilyJoinReviewVO> reviewVOList = new ArrayList<>();
        for (FamilyJoinReview familyJoinReview : familyJoinReviewList) {
            reviewVOList.add(this.getFamilyJoinReviewVO(familyJoinReview));
        }

        return reviewVOList;
    }


    // region 工具方法


    /**
     * 审核信息脱敏
     *
     * @param familyJoinReview 审核信息
     * @return 脱敏后的审核信息
     */
    @Override
    public FamilyJoinReviewVO getFamilyJoinReviewVO(FamilyJoinReview familyJoinReview) {
        FamilyJoinReviewVO result = new FamilyJoinReviewVO();
        result.setReviewId(familyJoinReview.getReviewId());
        result.setFamilyId(familyJoinReview.getFamilyId());
        result.setBlindId(familyJoinReview.getBlindId());
        result.setVolunteerId(familyJoinReview.getVolunteerId());
        result.setApplyTime(familyJoinReview.getApplyTime());


        // 审核状态修改
        String reviewStatusString = null;
        Integer reviewStatus = familyJoinReview.getReviewStatus();
        if (Objects.equals(reviewStatus, FamilyReviewStatusEnum.WAIT_REVIEW.getReviewStatus())) {
            reviewStatusString = FamilyReviewStatusEnum.WAIT_REVIEW.getName();
        } else if (Objects.equals(reviewStatus, FamilyReviewStatusEnum.PASSED.getReviewStatus())) {
            reviewStatusString = FamilyReviewStatusEnum.PASSED.getName();
        } else if (Objects.equals(reviewStatus, FamilyReviewStatusEnum.REJECTED.getReviewStatus())) {
            reviewStatusString = FamilyReviewStatusEnum.REJECTED.getName();
        }
        result.setReviewStatus(reviewStatusString);

        return result;
    }


    // endregion
}




