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
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

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
        if (!PhoneUtil.isPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        }
        synchronized (phone.intern()) {
            // 有账号直接登录
            Blind blind = this.getByPhone(phone);
            if (ObjUtil.isNotNull(blind)) {
                //生成token并存入redis
                String token = this.loginSuccess(blind);
                BlindLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(blind, token);
                return loginSuccessVO;
            }

            // 无账号,查询手机是否注册了另一张表
            Volunteer volunteer = this.getVolunteerByPhone(phone);
            if (ObjUtil.isNotNull(volunteer)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已被注册志愿者");
            }

            // 无账号自动注册
            Blind newBlind = new Blind();
            newBlind.setPhone(phone);
            boolean save = this.save(newBlind);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");
            }

            //生成token并存入redis
            String token = this.loginSuccess(newBlind);
            BlindLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(newBlind, token);
            return loginSuccessVO;
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
        if (!PhoneUtil.isPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        }
        // 密码格式校验
        boolean isMatch = ReUtil.isMatch(PASSWORD_REGEX, password);
        if (!isMatch) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码必须包含字符和数字");
        }
        String md5Password = SecureUtil.md5(password);// 加密

        synchronized (phone.intern()){
            // 有账号直接登录
            Blind blind = this.getByPhone(phone);
            if (ObjUtil.isNotNull(blind)) {
                //校验密码是否正确
                String blindPassword = blind.getPassword();
                if (!md5Password.equals(blindPassword)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码未设置或密码错误");
                }
                //生成token并存入redis
                String token = this.loginSuccess(blind);
                BlindLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(blind, token);
                return loginSuccessVO;
            }

            // 无账号,查询手机是否注册了另一张表
            Volunteer volunteer = this.getVolunteerByPhone(phone);
            if (ObjUtil.isNotNull(volunteer)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已被注册志愿者");
            }

            // 无账号自动注册
            Blind newBlind = new Blind();
            newBlind.setPhone(phone);
            newBlind.setPassword(md5Password);
            boolean save = this.save(newBlind);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙,请稍后再试");
            }


            //生成token并存入redis
            String token = this.loginSuccess(newBlind);
            BlindLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(newBlind, token);
            return loginSuccessVO;
        }
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
        Blind blind = this.getById(newBlind.getBlindId());

        String name = newBlind.getName();
        if(StrUtil.isNotBlank(name)){
            blind.setName(name);
        }
        Integer gender = newBlind.getGender();
        if(gender == GenderEnum.MAN.getContent() || gender == GenderEnum.WOMEN.getContent()){
            blind.setGender(gender);
        }
        String idCard = newBlind.getIdCard();
        if(StrUtil.isNotBlank(idCard)){
            if(IdcardUtil.isValidCard(idCard))
                blind.setIdCard(idCard);
            else
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"身份证格式错误");
        }
        String disabilityCard = newBlind.getDisabilityCard();
        if(StrUtil.isNotBlank(disabilityCard)){
            if(ReUtil.isMatch(BLIND_REGEX,disabilityCard))
                blind.setDisabilityCard(disabilityCard);
            else
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"残疾人证格式错误");
        }
        // todo 修改经纬度地址信息

        boolean b = this.updateById(blind);
        ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);

        return this.getBlindVO(blind);
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
        if (!PhoneUtil.isPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        }
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone", phone);
        Volunteer volunteer = volunteerMapper.selectOne(volunteerQueryWrapper);
        return volunteer;
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
        if (!PhoneUtil.isPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入数据格式错误");
        }
        // 有账号直接登录
        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("phone", phone);
        Blind blind = this.getOne(blindQueryWrapper);
        return blind;
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
        BeanUtils.copyProperties(this.getBlindVO(newBlind),res);
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
        String token = JwtUtils.generateToken(jwtMap, TOKEN_SECRETKEY, Duration.of(30, ChronoUnit.DAYS));

        redisUtils.setToRedis(REDIS_SECRETKEY + "-" + blind.getBlindId(), blind.getBlindId().toString(), 1L);
        return token;
    }


    // endregion
}




