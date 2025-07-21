package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallConstant;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.InnerBlindService;
import com.swj.shiwujie.service.InnerVolunteerService;
import com.swj.shiwujie.service.UrgenthelpService;
import com.swj.shiwujie.mapper.UrgenthelpMapper;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.ConverterUtils;
import com.swj.shiwujie.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * @author Administrator
 * @description 针对表【UrgentHelp(紧急求助表)】的数据库操作Service实现
 * @createDate 2025-07-11 21:26:52
 */
@Service
@Slf4j
public class UrgenthelpServiceImpl extends ServiceImpl<UrgenthelpMapper, Urgenthelp>
        implements UrgenthelpService {

    @DubboReference
    private InnerVolunteerService innerVolunteerService;


    @DubboReference
    private InnerBlindService innerBlindService;



    @Autowired
    private CoordinationSocketHandler coordinationSocketHandler;


    /**
     * 盲人发起求助
     * redis有加入,无创建
     *
     * @param loginBlindId   登录盲人id
     * @param loginUserPhone 盲人手机号
     * @return 是否成功
     */
    @Override
    public boolean createUrgenthelp(Long loginBlindId, String loginUserPhone) {

        //1. 检查是否在求助中
        Urgenthelp urgenthelp = this.getWaitingByBlindId(loginBlindId);
        ThrowUtils.throwIf(ObjUtil.isNotNull(urgenthelp), ErrorCode.PARAMS_ERROR, "您已经在求助中了");
        urgenthelp = this.getHelpingByBlindId(loginBlindId);
        ThrowUtils.throwIf(ObjUtil.isNotNull(urgenthelp), ErrorCode.PARAMS_ERROR, "您已经在求助中了");
        //2. 查询是否存在家庭
        Blind blind = innerBlindService.getById(loginBlindId);
        Long familyId = blind.getFamilyId();
        ThrowUtils.throwIf(ObjUtil.isNull(familyId), ErrorCode.PARAMS_ERROR, "您没有加入家庭,无法紧急求助");

        //拿到家庭成员信息
        List<Volunteer> volunteerList = innerVolunteerService.getListByFamilyId(familyId);
        synchronized (loginUserPhone.intern()) {
            //3. 创建数据库表
            urgenthelp = new Urgenthelp();
            urgenthelp.setBlind_id(loginBlindId);
            urgenthelp.setStart_time(DateUtil.date());
            urgenthelp.setHelp_status(CallHelpStatusEnum.WAITING.getHelpStatus());
            urgenthelp.setFamily_id(familyId);

            this.save(urgenthelp);

            //4. 向家庭成员发起求助
            SocketData socketData = new SocketData();
            socketData.setBlindPhone(blind.getPhone());
            coordinationSocketHandler.urgenthelpToFamily(volunteerList, socketData);

            return true;
        }
    }

    /**
     * 视障人士取消求助
     *
     * @param loginBlindId   登录盲人id
     * @param loginUserPhone 盲人手机号
     * @return 是否成功
     */
    @Override
    public boolean removeFromUrgenthelp(Long loginBlindId, String loginUserPhone) {

        Urgenthelp urgenthelp = this.getWaitingByBlindId(loginBlindId);
        ThrowUtils.throwIf(ObjUtil.isNull(urgenthelp), ErrorCode.PARAMS_ERROR, "您并未求助");

        //2. 查询是否存在家庭
        Blind blind = innerBlindService.getById(loginBlindId);
        Long familyId = blind.getFamilyId();
        ThrowUtils.throwIf(ObjUtil.isNull(familyId), ErrorCode.PARAMS_ERROR, "您没有加入家庭");
        //拿到家庭成员信息
        List<Volunteer> volunteerList = innerVolunteerService.getListByFamilyId(familyId);

        //修改匹配表信息
        urgenthelp.setHelp_status(CallHelpStatusEnum.FALL.getHelpStatus());
        boolean b = this.updateById(urgenthelp);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

        //4. 向家庭成员发起求助
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(blind.getPhone());
        coordinationSocketHandler.cancelUrgenthelp(volunteerList, socketData);


        return true;
    }

    /**
     * 家属加入帮助
     * @param loginVolunteerId 登录志愿者id
     * @param blindPhone 求助盲人ID
     * @return 是否成功
     */
    @Override
    public boolean joinUrgenthelp(String blindPhone,Long loginVolunteerId,String loginUserPhone) {

        //4. 更新求助表内容
        Blind blind = innerBlindService.getByPhone(blindPhone);
        Urgenthelp urgenthelp = this.getWaitingByBlindId(blind.getBlindId());
        ThrowUtils.throwIf(ObjUtil.isNull(urgenthelp), ErrorCode.PARAMS_ERROR,"对方没有在求助");

        urgenthelp.setVolunteer_id(loginVolunteerId);
        urgenthelp.setResponse_time(DateUtil.date());
        urgenthelp.setHelp_status(CallHelpStatusEnum.HELPING.getHelpStatus());
        urgenthelp.setChannel_id(loginVolunteerId);
        this.updateById(urgenthelp);

        return true;
    }


    //region 工具方法


    /**
     * 通过志愿者id查询信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    @Override
    public Urgenthelp getByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId);
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询未通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    @Override
    public Urgenthelp getWaitingByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.WAITING.getHelpStatus());
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    @Override
    public Urgenthelp getHelpingByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询信息
     *
     * @param blindId 志愿者id
     * @return 表信息
     */
    @Override
    public Urgenthelp getByBlindId(Long blindId) {
        if (ObjUtil.isNull(blindId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id", blindId);
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在等待的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    @Override
    public Urgenthelp getWaitingByBlindId(Long blindId) {
        if (ObjUtil.isNull(blindId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id", blindId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.WAITING.getHelpStatus());
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    @Override
    public Urgenthelp getHelpingByBlindId(Long blindId) {
        if (ObjUtil.isNull(blindId)) {
            return null;
        }
        QueryWrapper<Urgenthelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id", blindId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.getOne(queryWrapper);
    }

    //endregion

}




