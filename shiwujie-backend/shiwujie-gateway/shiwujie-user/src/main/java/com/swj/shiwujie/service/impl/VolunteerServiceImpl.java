package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.*;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.community.CommunityRolePermissionEnum;
import com.swj.shiwujie.model.enums.user.GenderEnum;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerUpdatePasswordRequest;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.service.community.InnerCommunitymanagerService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.swj.shiwujie.constants.UserConstants.*;

/**
 * @author Administrator
 * @description 针对表【Volunteer(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer>
        implements VolunteerService {


    @Resource
    private BlindMapper blindMapper;


    @Resource
    private RedisUtils redisUtils;


    @DubboReference
    private InnerCommunitymanagerService innerCommunitymanagerService;

    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    @Override
    public VolunteerLoginSuccessVO loginAndRegisterQuickly(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");


        // 有账号直接登录
        Volunteer volunteer = this.getByPhone(phone);
        if (ObjUtil.isNotNull(volunteer)) {
            //生成token并存入redis
            String token = this.generateLoginToken(volunteer);
            return this.getLoginSuccessVO(volunteer, token);
        }

        // 无账号,查询手机是否注册了另一张表
        Blind blind = this.getBlindByPhone(phone);
        ThrowUtils.throwIf(ObjUtil.isNotNull(blind), ErrorCode.PARAMS_ERROR, "该手机号已被注册盲人");

        // 无账号自动注册
        Volunteer newVolunteer = new Volunteer();
        newVolunteer.setPhone(phone);
        boolean save = this.save(newVolunteer);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");

        //生成token并存入redis
        String token = this.generateLoginToken(newVolunteer);
        return this.getLoginSuccessVO(newVolunteer, token);


    }

    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @Override
    public VolunteerLoginSuccessVO loginAndRegister(VolunteerLARRequest volunteerLARRequest) {
        String phone = volunteerLARRequest.getPhone();
        String password = volunteerLARRequest.getPassword();

        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        // 密码格式校验
        boolean isMatch = this.validatePassword(password);
        ThrowUtils.throwIf(!isMatch, ErrorCode.PARAMS_ERROR, "密码必须包含字符和数字");
        String md5Password = SecureUtil.md5(password);// 加密


        // 有账号直接登录
        Volunteer volunteer = this.getByPhone(phone);
        if (ObjUtil.isNotNull(volunteer)) {

            //校验密码是否正确
            String volunteerPassword = volunteer.getPassword();
            ThrowUtils.throwIf(!md5Password.equals(volunteerPassword), ErrorCode.PARAMS_ERROR, "密码未设置或密码错误");

            //生成token并存入redis
            String token = this.generateLoginToken(volunteer);
            return this.getLoginSuccessVO(volunteer, token);
        }

        // 无账号,查询手机是否注册了另一张表
        Blind blind = this.getBlindByPhone(phone);
        ThrowUtils.throwIf(ObjUtil.isNotNull(blind), ErrorCode.PARAMS_ERROR, "该手机号已被注册盲人");

        // 无账号自动注册
        Volunteer newVolunteer = new Volunteer();
        newVolunteer.setPhone(phone);
        newVolunteer.setPassword(md5Password);
        boolean save = this.save(newVolunteer);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");


        //生成token并存入redis
        String token = this.generateLoginToken(newVolunteer);
        return this.getLoginSuccessVO(newVolunteer, token);

    }

    /**
     * 修改密码
     *
     * @param volunteerUpdatePassword 原密码与要修改的密码
     * @return 是否成功
     */
    @Override
    public boolean updateVolunteerPassword(VolunteerUpdatePasswordRequest volunteerUpdatePassword) {

        // 校验密码格式
        String newPassword = volunteerUpdatePassword.getNewPassword();
        String originPassword = volunteerUpdatePassword.getOriginPassword();

        boolean isOriginMatch = this.validatePassword(originPassword);
        boolean isNewMatch = this.validatePassword(newPassword);
        ThrowUtils.throwIf(!(isOriginMatch && isNewMatch), ErrorCode.PARAMS_ERROR, "密码必须包含字符和数字");


        Volunteer volunteer = this.getById(volunteerUpdatePassword.getVolunteerId());
        ThrowUtils.throwIf(StrUtil.isBlank(volunteer.getPassword()), ErrorCode.PARAMS_ERROR, "原密码未设置");
        //检查原密码
        String md5OriginPassword = SecureUtil.md5(originPassword);
        ThrowUtils.throwIf(!md5OriginPassword.equals(volunteer.getPassword()), ErrorCode.PARAMS_ERROR, "原密码输入错误");


        // 密码加密更新
        volunteer.setPassword(SecureUtil.md5(newPassword));


        boolean result = this.updateById(volunteer);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);


        return true;
    }

    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     *
     * @param newVolunteer 用户更新信息
     * @return 脱敏后的用户信息
     */
    @Override
    public boolean updateVolunteer(Volunteer newVolunteer) {


        Volunteer volunteer = this.getById(newVolunteer.getVolunteerId());
        ThrowUtils.throwIf(ObjUtil.isNull(volunteer), ErrorCode.PARAMS_ERROR, "修改用户不存在");

        String name = newVolunteer.getName();
        if (StrUtil.isNotBlank(name)) {
            volunteer.setName(name);
        }
        Integer gender = newVolunteer.getGender();
        if (gender == GenderEnum.MAN.getContent() || gender == GenderEnum.WOMEN.getContent()) {
            volunteer.setGender(gender);
        }
        String idCard = newVolunteer.getIdCard();
        if (StrUtil.isNotBlank(idCard)) {
            if (IdcardUtil.isValidCard(idCard))
                volunteer.setIdCard(SecureUtil.md5(idCard));
            else
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "身份证格式错误");
        }

        // todo 修改经纬度地址信息

        boolean b = this.updateById(volunteer);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

        return true;


    }

    /**
     * 删除志愿者(同时删除创建的家庭)
     *
     * @param volunteerId    志愿者id
     * @param loginUserPhone 登录手机号
     * @return 是否成功
     */
    @Override
    public boolean deleteVolunteer(Long volunteerId, String loginUserPhone) {

        // 检测是否有家庭
        Volunteer volunteer = this.getById(volunteerId);
        Long familyId = volunteer.getFamilyId();
        synchronized (loginUserPhone.intern()){
            if(ObjUtil.isNotNull(familyId)){
                // 删除家庭,删除家庭中成员的familyId信息
                QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
                blindQueryWrapper.eq("family_id", familyId);
                List<Blind> blindList = blindMapper.selectList(blindQueryWrapper);
                for (Blind blind : blindList) {
                    blind.setFamilyId(null);
                    blindMapper.updateById(blind);
                }


                QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
                volunteerQueryWrapper.eq("family_id", familyId);
                List<Volunteer> volunteerList = this.list(volunteerQueryWrapper);
                for (Volunteer volunteer1 : volunteerList) {
                    // 家主不修改familyID字段
                    if(!volunteer1.getVolunteerId().equals(volunteerId)){
                        volunteer1.setFamilyId(null);
                    }
                }
                boolean b = this.updateBatchById(volunteerList);
            }

            boolean b = this.removeById(volunteerId);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

            redisUtils.removeToRedis(REDIS_SECRETKEY + "-" + volunteerId);
        }


        return true;
    }


    // region 工具方法

    /**
     * 通过手机号查询志愿者信息
     *
     * @param phone 志愿者手机号
     * @return 志愿者信息
     */
    @Override
    public Blind getBlindByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("phone", phone);
        return blindMapper.selectOne(blindQueryWrapper);
    }

    /**
     * 通过手机号查询用户(视障人士)信息
     *
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    @Override
    public Volunteer getByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone", phone);
        return this.getOne(volunteerQueryWrapper);
    }


    /**
     * 用户注册登录返回信息脱敏
     *
     * @param newVolunteer 返回的盲人信息
     * @param token        登录token
     * @return 脱敏后的对象
     */
    @Override
    public VolunteerLoginSuccessVO getLoginSuccessVO(Volunteer newVolunteer, String token) {
        VolunteerLoginSuccessVO res = new VolunteerLoginSuccessVO();
        BeanUtils.copyProperties(this.getVolunteerVO(newVolunteer), res);
        res.setToken(token);
        return res;
    }


    /**
     * 用户信息脱敏(不含token)
     *
     * @param newVolunteer 盲人信息
     * @return 脱敏后的信息
     */
    @Override
    public VolunteerVO getVolunteerVO(Volunteer newVolunteer) {
        VolunteerVO res = new VolunteerVO();
        res.setVolunteerId(newVolunteer.getVolunteerId());
        res.setCommunityId(newVolunteer.getCommunityId());
        res.setIsActivelyJoined(newVolunteer.getIsActivelyJoined());
        res.setFamilyId(newVolunteer.getFamilyId());
        res.setName(newVolunteer.getName());
        res.setPhone(newVolunteer.getPhone());
        // 若有密码则脱敏,无密码不管
        if (StrUtil.isBlankIfStr(newVolunteer.getPassword())) {
            res.setPassword(null);
        } else {
            res.setPassword("********");
        }
        res.setGender(newVolunteer.getGender());
        res.setWechatId(newVolunteer.getWechatId());
        res.setQqId(newVolunteer.getQqId());

        // 若实名后将信息隐藏
        if (StrUtil.isBlankIfStr(newVolunteer.getIdCard())) {
            res.setIsIdCard(false);
        } else {
            res.setIsIdCard(true);
        }

        res.setOtherInfo(newVolunteer.getOtherInfo());
        res.setHelpCount(newVolunteer.getHelpCount());
        res.setLatitude(newVolunteer.getLatitude());
        res.setLongitude(newVolunteer.getLongitude());
        res.setLocationAddress(newVolunteer.getLocationAddress());
        res.setLocationUpdateTime(newVolunteer.getLocationUpdateTime());


        //设置社区职位
        if(ObjUtil.isNotNull(newVolunteer.getCommunityId())){
            Communitymanager communitymanager = innerCommunitymanagerService.getByVolunteerIdAndCommunityId(
                    newVolunteer.getVolunteerId(),newVolunteer.getCommunityId());
            Long role = communitymanager.getRolePermissionId();
            if(ObjUtil.isNotNull(role)){
                res.setCommunityManager(CommunityRolePermissionEnum.getById(role).getName());
            }
        }

        return res;
    }

    /**
     * 登录成功实现令牌生成与redis储存
     *
     * @param volunteer 盲人信息
     * @return token
     */
    @Override
    public String generateLoginToken(Volunteer volunteer) {
        // 生成token并脱敏返回,token存入redis
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put("volunteerId", volunteer.getVolunteerId());
        jwtMap.put("isBlind", false);
        jwtMap.put("phone", volunteer.getPhone());


        // 获取社区管理员角色(如果加入了社区)
        Long communityId = volunteer.getCommunityId();
        jwtMap.put("role", null);
        if(ObjUtil.isNotNull(communityId)){
            Communitymanager communitymanager = innerCommunitymanagerService.getByVolunteerIdAndCommunityId(
                    volunteer.getVolunteerId(),communityId);
            ThrowUtils.throwIf(ObjUtil.isNull(communitymanager), ErrorCode.NO_AUTH, "无社区管理权限");
            Long role = communitymanager.getRolePermissionId();
            jwtMap.put("role", role);
        }

        String token = JwtUtils.generateToken(jwtMap, TOKEN_SECRETKEY, Duration.of(30, ChronoUnit.DAYS));

        redisUtils.setToRedis(REDIS_SECRETKEY + "-volunteer-" + volunteer.getVolunteerId(), token, 1L);

        return token;
    }

    /**
     * 验证密码是否符合规则：
     * - 至少包含一个字母（大小写均可）
     * - 至少包含一个数字
     * - 仅允许字母和数字
     *
     * @param password 待验证的密码字符串
     * @return 如果符合规则返回 true，否则返回 false
     */
    @Override
    public boolean validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            return false; // 空密码不合法
        }

        Matcher matcher = PASSWORD_REGEX.matcher(password);
        return matcher.matches();
    }


    /**
     * 通过家庭id获取成员信息
     *
     * @param familyId 家庭id
     * @return 志愿者信息
     */
    @Override
    public List<VolunteerVO> getVolunteerVOListByFamilyId(Long familyId) {
        if (ObjUtil.isNull(familyId)) {
            return null;
        }

        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("family_id", familyId);
        List<Volunteer> volunteerList = this.list(volunteerQueryWrapper);

        List<VolunteerVO> volunteerVOList = new ArrayList<>();
        for (Volunteer volunteer : volunteerList) {
            VolunteerVO volunteerVO = this.getVolunteerVO(volunteer);
            volunteerVOList.add(volunteerVO);
        }

        return volunteerVOList;
    }
    // endregion
}




