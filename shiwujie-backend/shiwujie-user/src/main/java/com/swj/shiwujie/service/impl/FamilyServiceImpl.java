package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Family;
import com.swj.shiwujie.model.domain.user.FamilyJoinReview;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.user.family.FamilyRemoveUserRequest;
import com.swj.shiwujie.model.request.user.family.FamilyUpdateRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.FamilyJoinReviewService;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.mapper.FamilyMapper;
import com.swj.shiwujie.service.VolunteerService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author Administrator
 * @description 针对表【Family(家庭信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
public class FamilyServiceImpl extends ServiceImpl<FamilyMapper, Family>
        implements FamilyService {

    @Resource
    private VolunteerService volunteerService;

    @Resource
    private BlindService blindService;

    @Resource
    private FamilyJoinReviewService familyJoinReviewService;


    /**
     * 创建家庭
     *
     * @param loginVolunteerId 登录用户id
     * @param loginUserPhone   登录用户手机号
     * @return 脱敏后的家庭信息
     */
    @Override
    public FamilyVO createFamily(Long loginVolunteerId, String loginUserPhone) {

        synchronized (loginUserPhone.intern()) {
            // 检测是否已经有家庭
            QueryWrapper<Family> familyQueryWrapper = new QueryWrapper<>();
            familyQueryWrapper.eq("creator_volunteer_id",loginVolunteerId);
            Family one = this.getOne(familyQueryWrapper);
            ThrowUtils.throwIf(ObjUtil.isNotNull(one),ErrorCode.PARAMS_ERROR,"您已经创建过家庭了");

            // 家主
            Volunteer volunteer = volunteerService.getById(loginVolunteerId);

            // 创建家庭
            Family family = new Family();
            family.setCreatorVolunteerId(loginVolunteerId);
            boolean save = this.save(family);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);

            //修改家主信息
            volunteer.setFamilyId(family.getFamilyId());
            boolean b = volunteerService.updateById(volunteer);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);


            return this.getFamilyVO(family);
        }

    }

    /**
     * 获取家庭信息
     *
     * @param familyId       家庭id
     * @param loginUserPhone 登录用户手机号
     * @return 脱敏后的家庭信息
     */
    @Override
    public FamilyVO getFamilyVOById(Long familyId, String loginUserPhone) {
        synchronized (loginUserPhone.intern()){
            Family family = this.getById(familyId);
            ThrowUtils.throwIf(ObjUtil.isNull(family),ErrorCode.PARAMS_ERROR,"家庭不存在");
            return this.getFamilyVO(family);
        }

    }

    /**
     * 删除家庭
     *
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone   登录志愿者手机号
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean deleteFamily(Long loginVolunteerId, String loginUserPhone) {
        synchronized (loginUserPhone.intern()) {
            // 家主
            Volunteer creatorVolunteer = volunteerService.getById(loginVolunteerId);

            //2. 删除家庭
            Long familyId = creatorVolunteer.getFamilyId();


            QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
            blindQueryWrapper.eq("family_id", familyId);
            List<Blind> blindList = blindService.list(blindQueryWrapper);
            for (Blind blind : blindList) {
                blind.setFamilyId(null);
            }
            blindService.updateBatchById(blindList);


            QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
            volunteerQueryWrapper.eq("family_id", familyId);
            List<Volunteer> volunteerList = volunteerService.list(volunteerQueryWrapper);
            for (Volunteer volunteer : volunteerList) {
                volunteer.setFamilyId(null);
            }
            boolean b = volunteerService.updateBatchById(volunteerList);



            return this.removeById(familyId);
        }
    }

    /**
     * 申请加入家庭
     *
     * @param familyVolunteerPhone 家庭志愿者手机号
     * @param loginBlindId     加入盲人信息
     * @param loginVolunteerId 加入志愿者信息
     * @param loginUserPhone   登录手机号
     * @return 是否申请成功
     */
    @Override
    public boolean joinFamily(String familyVolunteerPhone, Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(familyVolunteerPhone),ErrorCode.PARAMS_ERROR,"家庭信息不能为空");


        Volunteer volunteer = volunteerService.getByPhone(familyVolunteerPhone);
        Long familyId = volunteer.getFamilyId();

        // 校验用户
        if(ObjUtil.isNotNull(loginBlindId)){

            FamilyJoinReview familyJoinReview = new FamilyJoinReview();
            familyJoinReview.setFamilyId(familyId);
            familyJoinReview.setBlindId(loginBlindId);
            familyJoinReview.setApplyTime(DateUtil.date());

            boolean save = familyJoinReviewService.save(familyJoinReview);
            ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR);
        } else if (ObjUtil.isNotNull(loginVolunteerId)) {
            // 家主不能申请加入自己的家庭
            Family family = this.getById(familyId);
            ThrowUtils.throwIf(loginVolunteerId.equals(family.getCreatorVolunteerId()),
                    ErrorCode.PARAMS_ERROR,"家主不能加入自己的家庭");

            // 创建申请信息
            FamilyJoinReview familyJoinReview = new FamilyJoinReview();
            familyJoinReview.setFamilyId(familyId);
            familyJoinReview.setVolunteerId(loginVolunteerId);
            familyJoinReview.setApplyTime(DateUtil.date());

            boolean save = familyJoinReviewService.save(familyJoinReview);
            ThrowUtils.throwIf(!save,ErrorCode.SYSTEM_ERROR);
        }

        return true;


    }


    /**
     * 更新家庭信息
     *
     * @param familyUpdateRequest 家庭id,更新信息
     * @param loginVolunteerId    家主id
     * @return 更新后的脱敏家庭信息
     */
    @Override
    public boolean updateFamily(FamilyUpdateRequest familyUpdateRequest, Long loginVolunteerId) {

            Long familyId = familyUpdateRequest.getFamilyId();
            Family family = this.getById(familyId);

            // 只有家主可以修改
            ThrowUtils.throwIf(!loginVolunteerId.equals(family.getCreatorVolunteerId())
                    , ErrorCode.PARAMS_ERROR, "只有家主可以修改家庭信息");
            // 检查更新
            String familyName = familyUpdateRequest.getFamilyName();
            if (StrUtil.isNotBlank(familyName)) {
                family.setFamilyName(familyName);
            }
            String familyDescription = familyUpdateRequest.getFamilyDescription();
            if (StrUtil.isNotBlank(familyDescription)) {
                family.setFamilyDescription(familyDescription);
            }

            boolean b = this.updateById(family);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

            return true;


    }

    /**
     * 从家庭中移除用户
     *
     * @param familyRemoveUserRequest 家庭id,用户id
     * @param loginVolunteerId        家主id
     * @param loginUserPhone          家主手机号
     * @return 脱敏后的家庭信息
     */
    @Override
    public Boolean removeUserFromFamily(FamilyRemoveUserRequest familyRemoveUserRequest, Long loginVolunteerId, String loginUserPhone) {
        synchronized (loginUserPhone.intern()) {
            Long familyId = familyRemoveUserRequest.getFamilyId();
            Long blindId = familyRemoveUserRequest.getBlindId();
            Long volunteerId = familyRemoveUserRequest.getVolunteerId();

            Family family = this.getById(familyId);
            // 只有家主可以修改
            ThrowUtils.throwIf(!loginVolunteerId.equals(family.getCreatorVolunteerId())
                    , ErrorCode.PARAMS_ERROR, "只有家主可以修改家庭信息");


            // 用户删掉字段
            if (ObjUtil.isNotNull(blindId)) {
                Blind blind = blindService.getById(blindId);
                blind.setFamilyId(null);
                boolean save = blindService.updateById(blind);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
            } else if (ObjUtil.isNotNull(volunteerId)) {
                Volunteer volunteer = volunteerService.getById(volunteerId);
                volunteer.setFamilyId(null);
                boolean save = volunteerService.updateById(volunteer);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR);
            }


            return true;
        }
    }

    /**
     * 用户主动退出家庭
     * @param loginBlindId     登录盲人id(可以为空)
     * @param loginVolunteerId 登录志愿者id(可以为空)
     * @param loginUserPhone   登录手机号
     * @return
     */
    @Override
    public boolean userLeaveFromFamily(Long loginBlindId, Long loginVolunteerId, String loginUserPhone) {

        synchronized (loginUserPhone.intern()){
            // 用户删掉字段
            if (ObjUtil.isNotNull(loginBlindId)) {
                Blind blind = blindService.getById(loginBlindId);
                blind.setFamilyId(null);
                boolean update = blindService.updateById(blind);
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "退出家庭失败");
            } else if (ObjUtil.isNotNull(loginVolunteerId)) {
                Volunteer volunteer = volunteerService.getById(loginVolunteerId);
                volunteer.setFamilyId(null);
                boolean update = volunteerService.updateById(volunteer);
                ThrowUtils.throwIf(!update, ErrorCode.SYSTEM_ERROR, "退出家庭失败");
            }



            return true;
        }

    }


    // region 工具方法

    /**
     * 家庭脱敏
     *
     * @param family 家庭信息
     * @return 脱敏后的家庭信息
     */
    @Override
    public FamilyVO getFamilyVO(Family family) {
        if (ObjUtil.isNull(family)) {
            return null;
        }

        Long familyId = family.getFamilyId();
        Long creatorVolunteerId = family.getCreatorVolunteerId();

        FamilyVO result = new FamilyVO();

        BeanUtils.copyProperties(family,result);

        VolunteerVO creatorVolunteerVO = new VolunteerVO();

        // 脱敏家庭成员信息
        List<VolunteerVO> volunteerVOList = volunteerService.getVolunteerVOListByFamilyId(familyId);
        for (VolunteerVO volunteerVO : volunteerVOList) {
            // 找到家主信息并移除出去
            if (Objects.equals(creatorVolunteerId, volunteerVO.getVolunteerId())) {
                creatorVolunteerVO = volunteerVO;
                volunteerVOList.remove(volunteerVO);
                break;
            }

        }
        result.setCreatorVolunteer(creatorVolunteerVO);
        result.setVolunteerVOList(volunteerVOList);


        List<BlindVO> blindVOList = blindService.getBlindListByFamilyId(familyId);
        result.setBlindVOList(blindVOList);


        return result;
    }


    // endregion


}




