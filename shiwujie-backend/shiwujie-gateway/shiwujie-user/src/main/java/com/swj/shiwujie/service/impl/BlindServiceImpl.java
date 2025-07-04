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
import com.swj.shiwujie.model.VO.user.blind.BlindLoginSuccessVO;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.enums.user.GenderEnum;
import com.swj.shiwujie.model.request.user.blind.BlindLARRequest;
import com.swj.shiwujie.model.request.user.blind.BlindUpdatePasswordRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
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
 * @description 针对表【Blind(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@Service
public class BlindServiceImpl extends ServiceImpl<BlindMapper, Blind>
        implements BlindService {


    @Resource
    private VolunteerMapper volunteerMapper;


    @Resource
    private RedisUtils redisUtils;


    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    @Override
    public BlindLoginSuccessVO loginAndRegisterQuickly(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        // 加锁
        synchronized (phone.intern()) {
            // 有账号直接登录
            Blind blind = this.getByPhone(phone);
            if (ObjUtil.isNotNull(blind)) {
                //生成token并存入redis
                String token = this.loginSuccess(blind);
                return this.getLoginSuccessVO(blind, token);
            }

            // 无账号,查询手机是否注册了另一张表
            Volunteer volunteer = this.getVolunteerByPhone(phone);
            ThrowUtils.throwIf(ObjUtil.isNotNull(volunteer), ErrorCode.PARAMS_ERROR, "该手机号已被注册志愿者");

            // 无账号自动注册
            Blind newBlind = new Blind();
            newBlind.setPhone(phone);
            boolean save = this.save(newBlind);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");

            //生成token并存入redis
            String token = this.loginSuccess(newBlind);
            return this.getLoginSuccessVO(newBlind, token);
        }

    }

    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     *
     * @param blindLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @Override
    public BlindLoginSuccessVO loginAndRegister(BlindLARRequest blindLARRequest) {
        String phone = blindLARRequest.getPhone();
        String password = blindLARRequest.getPassword();

        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");

        // 密码格式校验
        boolean isMatch = this.validatePassword(password);
        ThrowUtils.throwIf(!isMatch, ErrorCode.PARAMS_ERROR, "密码必须包含字符和数字");
        String md5Password = SecureUtil.md5(password);// 加密

        synchronized (phone.intern()) {

            // 有账号直接登录
            Blind blind = this.getByPhone(phone);
            if (ObjUtil.isNotNull(blind)) {

                //校验密码是否正确
                String blindPassword = blind.getPassword();
                ThrowUtils.throwIf(!md5Password.equals(blindPassword), ErrorCode.PARAMS_ERROR, "密码未设置或密码错误");

                //生成token并存入redis
                String token = this.loginSuccess(blind);
                return this.getLoginSuccessVO(blind, token);
            }

            // 无账号,查询手机是否注册了另一张表
            Volunteer volunteer = this.getVolunteerByPhone(phone);
            ThrowUtils.throwIf(ObjUtil.isNotNull(volunteer), ErrorCode.PARAMS_ERROR, "该手机号已被注册志愿者");

            // 无账号自动注册
            Blind newBlind = new Blind();
            newBlind.setPhone(phone);
            newBlind.setPassword(md5Password);
            boolean save = this.save(newBlind);
            ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");


            //生成token并存入redis
            String token = this.loginSuccess(newBlind);
            return this.getLoginSuccessVO(newBlind, token);
        }
    }

    /**
     * 修改密码
     *
     * @param blindUpdatePassword 原密码与要修改的密码
     * @param loginUserPhone 登录用户手机号
     * @return 是否成功
     */
    @Override
    public boolean updateBlindPassword(BlindUpdatePasswordRequest blindUpdatePassword,  String loginUserPhone) {

        // 校验密码格式
        String newPassword = blindUpdatePassword.getNewPassword();
        String originPassword = blindUpdatePassword.getOriginPassword();

        boolean isOriginMatch = this.validatePassword(originPassword);
        boolean isNewMatch = this.validatePassword(newPassword);
        ThrowUtils.throwIf(!(isOriginMatch && isNewMatch), ErrorCode.PARAMS_ERROR, "密码必须包含字符和数字");


        synchronized (loginUserPhone.intern()) {
            Blind blind = this.getById(blindUpdatePassword.getBlindId());
            ThrowUtils.throwIf(StrUtil.isBlank(blind.getPassword()), ErrorCode.PARAMS_ERROR, "原密码未设置");
            //检查原密码
            String md5OriginPassword = SecureUtil.md5(originPassword);
            ThrowUtils.throwIf(!md5OriginPassword.equals(blind.getPassword()), ErrorCode.PARAMS_ERROR, "原密码输入错误");


            // 密码加密更新
            blind.setPassword(SecureUtil.md5(newPassword));


            boolean result = this.updateById(blind);
            ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR);
        }


        return true;
    }

    /**
     * 更新用户
     * 修改用户名,性别,身份证号,残疾人证
     * 后期可以修改经纬度与位置信息
     *
     * @param newBlind 用户更新信息
     * @return 脱敏后的用户信息
     */
    @Override
    public BlindVO updateBlind(Blind newBlind) {


        synchronized (newBlind.getPhone().intern()){
            Blind blind = this.getById(newBlind.getBlindId());
            ThrowUtils.throwIf(ObjUtil.isNull(blind),ErrorCode.PARAMS_ERROR,"修改用户不存在");

            String name = newBlind.getName();
            if (StrUtil.isNotBlank(name)) {
                blind.setName(name);
            }
            Integer gender = newBlind.getGender();
            if (gender == GenderEnum.MAN.getContent() || gender == GenderEnum.WOMEN.getContent()) {
                blind.setGender(gender);
            }
            String idCard = newBlind.getIdCard();
            if (StrUtil.isNotBlank(idCard)) {
                if (IdcardUtil.isValidCard(idCard))
                    blind.setIdCard(SecureUtil.md5(idCard));
                else
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "身份证格式错误");
            }
            String disabilityCard = newBlind.getDisabilityCard();
            if (StrUtil.isNotBlank(disabilityCard)) {
                if (ReUtil.isMatch(BLIND_REGEX, disabilityCard))
                    blind.setDisabilityCard(SecureUtil.md5(disabilityCard));
                else
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "残疾人证格式错误");
            }

            // todo 修改经纬度地址信息

            boolean b = this.updateById(blind);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

            return this.getBlindVO(blind);
        }


    }


    // region 工具方法

    /**
     * 通过手机号查询志愿者信息
     *
     * @param phone 志愿者手机号
     * @return 志愿者信息
     */
    @Override
    public Volunteer getVolunteerByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone", phone);
        return volunteerMapper.selectOne(volunteerQueryWrapper);
    }

    /**
     * 通过手机号查询用户(视障人士)信息
     *
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    @Override
    public Blind getByPhone(String phone) {
        // 手机号合法校验
        ThrowUtils.throwIf(!PhoneUtil.isPhone(phone), ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        // 有账号直接登录
        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("phone", phone);
        return this.getOne(blindQueryWrapper);
    }


    /**
     * 用户注册登录返回信息脱敏
     *
     * @param newBlind 返回的盲人信息
     * @param token    登录token
     * @return 脱敏后的对象
     */
    @Override
    public BlindLoginSuccessVO getLoginSuccessVO(Blind newBlind, String token) {
        BlindLoginSuccessVO res = new BlindLoginSuccessVO();
        BeanUtils.copyProperties(this.getBlindVO(newBlind), res);
        res.setToken(token);
        return res;
    }


    /**
     * 用户信息脱敏(不含token)
     *
     * @param newBlind 盲人信息
     * @return 脱敏后的信息
     */
    @Override
    public BlindVO getBlindVO(Blind newBlind) {
        BlindVO res = new BlindVO();
        res.setBlindId(newBlind.getBlindId());
        res.setCommunityId(newBlind.getCommunityId());
        res.setIsActivelyJoined(newBlind.getIsActivelyJoined());
        res.setFamilyId(newBlind.getFamilyId());
        res.setName(newBlind.getName());
        res.setPhone(newBlind.getPhone());
        // 若有密码则脱敏,无密码不管
        if (StrUtil.isBlankIfStr(newBlind.getPassword())) {
            res.setPassword(null);
        } else {
            res.setPassword("********");
        }
        res.setGender(newBlind.getGender());
        res.setWechatId(newBlind.getWechatId());
        res.setQqId(newBlind.getQqId());
        // 若实名后将信息隐藏
        if (StrUtil.isBlankIfStr(newBlind.getIdCard())) {
            res.setIsIdCard(false);
        } else {
            res.setIsIdCard(true);
        }

        if (StrUtil.isBlankIfStr(newBlind.getDisabilityCard())) {
            res.setIsDisabilityCard(false);
        } else {
            res.setIsDisabilityCard(true);
        }

        res.setOtherInfo(newBlind.getOtherInfo());
        res.setHelpRequestCount(newBlind.getHelpRequestCount());
        res.setLatitude(newBlind.getLatitude());
        res.setLongitude(newBlind.getLongitude());
        res.setLocationAddress(newBlind.getLocationAddress());
        res.setLocationUpdateTime(newBlind.getLocationUpdateTime());
        return res;
    }

    /**
     * 登录成功实现令牌生成与redis储存
     *
     * @param blind 盲人信息
     * @return token
     */
    @Override
    public String loginSuccess(Blind blind) {
        // 生成token并脱敏返回,token存入redis
        Map<String, Object> jwtMap = new HashMap<>();
        jwtMap.put("blindId", blind.getBlindId());
        jwtMap.put("isBlind", true);
        jwtMap.put("phone",blind.getPhone());
        String token = JwtUtils.generateToken(jwtMap, TOKEN_SECRETKEY, Duration.of(30, ChronoUnit.DAYS));

        redisUtils.setToRedis(REDIS_SECRETKEY + "-" + blind.getBlindId(), token, 1L);
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
     * @return 盲人信息
     */
    @Override
    public List<BlindVO> getBlindListByFamilyId(Long familyId) {
        if(ObjUtil.isNull(familyId)){
            return null;
        }

        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("family_id",familyId);
        List<Blind> blindList = this.list(blindQueryWrapper);

        List<BlindVO> blindVOList = new ArrayList<>();
        for (Blind blind : blindList) {
            BlindVO blindVO = this.getBlindVO(blind);
            blindVOList.add(blindVO);
        }

        return blindVOList;
    }
    // endregion
}




