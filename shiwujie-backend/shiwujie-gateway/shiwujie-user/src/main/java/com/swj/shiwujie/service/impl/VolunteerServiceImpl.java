package com.swj.shiwujie.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.mapper.BlindMapper;
import com.swj.shiwujie.mapper.VolunteerMapper;
import com.swj.shiwujie.model.VO.user.VolunteerLoginSuccessVO;
import com.swj.shiwujie.model.domain.Blind;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.domain.Volunteer;
import com.swj.shiwujie.model.request.user.VolunteerLARRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.VolunteerService;
import com.swj.shiwujie.utils.JwtUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.swj.shiwujie.constants.UserConstants.*;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Service实现
* @createDate 2025-07-01 00:21:42
*/
@Service
public class VolunteerServiceImpl extends ServiceImpl<VolunteerMapper, Volunteer>
    implements VolunteerService{




    @Resource
    private BlindMapper blindMapper;


    @Resource
    private RedisUtils redisUtils;


    /**
     * 手机号一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param phone 手机号
     * @return 脱敏后的用户信息
     */
    @Override
    public VolunteerLoginSuccessVO loginAndRegisterQuickly(String phone) {
        // 手机号合法校验
        if(!PhoneUtil.isPhone(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }

        synchronized (phone.intern()){
            // 有账号直接登录
            Volunteer volunteer = this.getByPhone(phone);
            if(ObjUtil.isNotNull(volunteer)){
                //生成token并存入redis
                String token = this.loginSuccess(volunteer);
                VolunteerLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(volunteer,token);
                return loginSuccessVO;
            }

            // 无账号,查询手机是否注册了另一张表
            Blind blind = this.getBlindByPhone(phone);
            if(ObjUtil.isNotNull(blind)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"该手机号已被注册视障人士");
            }

            // 无账号自动注册
            Volunteer newVolunteer = new Volunteer();
            newVolunteer.setPhone(phone);
            boolean save = this.save(newVolunteer);
            if(!save){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统繁忙,请稍后再试");
            }

            //生成token并存入redis
            String token = this.loginSuccess(newVolunteer);
            VolunteerLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(newVolunteer,token);
            return loginSuccessVO;
        }
    }

    /**
     * 手机号,密码一键登录注册
     * 账号存在,登录
     * 账号不存在,注册
     * @param volunteerLARRequest 用户的手机号与密码
     * @return 脱敏后的用户信息
     */
    @Override
    public VolunteerLoginSuccessVO loginAndRegister(VolunteerLARRequest volunteerLARRequest) {
        String phone = volunteerLARRequest.getPhone();
        String password = volunteerLARRequest.getPassword();
        // 手机号合法校验
        if(!PhoneUtil.isPhone(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }
        // 密码格式校验
        boolean isMatch = ReUtil.isMatch(PASSWORD_REGEX, password);
        if(!isMatch){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }
        String md5Password = SecureUtil.md5(password);// 加密

        synchronized (phone.intern()){
            // 有账号直接登录
            Volunteer volunteer = this.getByPhone(phone);
            if(ObjUtil.isNotNull(volunteer)){
                //校验密码是否正确
                String volunteerPassword = volunteer.getPassword();
                if(!md5Password.equals(volunteerPassword)){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码未设置或密码错误");
                }
                //生成token并存入redis
                String token = this.loginSuccess(volunteer);
                VolunteerLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(volunteer,token);
                return loginSuccessVO;
            }

            // 无账号,查询手机是否注册了另一张表
            Blind blind = this.getBlindByPhone(phone);
            if(ObjUtil.isNotNull(blind)){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"该手机号已被注册视障人士");
            }

            // 无账号自动注册
            Volunteer newVolunteer = new Volunteer();
            newVolunteer.setPhone(phone);
            newVolunteer.setPassword(md5Password);
            boolean save = this.save(newVolunteer);
            if(!save){
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"系统繁忙,请稍后再试");
            }


            //生成token并存入redis
            String token = this.loginSuccess(newVolunteer);
            VolunteerLoginSuccessVO loginSuccessVO = this.getLoginSuccessVO(newVolunteer,token);
            return loginSuccessVO;
        }
    }


    // region 工具方法

    /**
     * 通过手机号查询用户(志愿者)信息
     * @param phone 志愿者手机号
     * @return 志愿者信息
     */
    @Override
    public Volunteer getByPhone(String phone) {
        // 手机号合法校验
        if(!PhoneUtil.isPhone(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }
        // 有账号直接登录
        QueryWrapper<Volunteer> volunteerQueryWrapper = new QueryWrapper<>();
        volunteerQueryWrapper.eq("phone",phone);
        Volunteer volunteer = this.getOne(volunteerQueryWrapper);
        return volunteer;
    }

    /**
     * 通过手机号查询盲人信息
     * @param phone 盲人手机号
     * @return 盲人信息
     */
    @Override
    public Blind getBlindByPhone(String phone) {
        // 手机号合法校验
        if(!PhoneUtil.isPhone(phone)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入数据格式错误");
        }
        // 有账号直接登录
        QueryWrapper<Blind> blindQueryWrapper = new QueryWrapper<>();
        blindQueryWrapper.eq("phone",phone);
        Blind blind = blindMapper.selectOne(blindQueryWrapper);
        return blind;
    }

    /**
     * 用户注册登录返回信息脱敏
     * @param newVolunteer 返回的志愿者信息
     * @param token 登录token
     * @return 脱敏后的对象
     */
    @Override
    public VolunteerLoginSuccessVO getLoginSuccessVO(Volunteer newVolunteer, String token) {
        VolunteerLoginSuccessVO res = new VolunteerLoginSuccessVO();
        res.setVolunteerId(newVolunteer.getVolunteerId());
        res.setCommunityId(newVolunteer.getCommunityId());
        res.setIsActivelyJoined(newVolunteer.getIsActivelyJoined());
        res.setFamilyId(newVolunteer.getFamilyId());
        res.setName(newVolunteer.getName());
        res.setPhone(newVolunteer.getPhone());
        // 若有密码则脱敏,无密码不管
        if(StrUtil.isBlankIfStr(newVolunteer.getPassword())){
            res.setPassword(null);
        }else{
            res.setPassword("********");
        }
        res.setGender(newVolunteer.getGender());
        res.setWechatId(newVolunteer.getWechatId());
        res.setQqId(newVolunteer.getQqId());
        // 若实名后将信息隐藏
        if(StrUtil.isBlankIfStr(newVolunteer.getIdCard())){
            res.setIsIdCard(false);
        }else{
            res.setIsIdCard(true);
        }

        res.setOtherInfo(newVolunteer.getOtherInfo());
        res.setHelpCount(newVolunteer.getHelpCount());
        res.setLatitude(newVolunteer.getLatitude());
        res.setLongitude(newVolunteer.getLongitude());
        res.setLocationAddress(newVolunteer.getLocationAddress());
        res.setLocationUpdateTime(newVolunteer.getLocationUpdateTime());
        res.setToken(token);
        return res;
    }


    /**
     * 登录成功实现令牌生成与redis储存
     * @param volunteer 志愿者信息
     * @return token
     */
    @Override
    public String loginSuccess(Volunteer volunteer){
        // 生成token并脱敏返回,token存入redis
        Map<String,Object> jwtMap = new HashMap<>();
        jwtMap.put("volunteerId",volunteer.getVolunteerId());
        jwtMap.put("isVolunteer",true);
        String token = JwtUtils.generateToken(jwtMap, TOKEN_SECRETKEY, Duration.of(30, ChronoUnit.DAYS));

        redisUtils.setToRedis(REDIS_SECRETKEY+"-"+volunteer.getVolunteerId(),volunteer.getVolunteerId().toString(),1L);
        return token;
    }


    // endregion

}




