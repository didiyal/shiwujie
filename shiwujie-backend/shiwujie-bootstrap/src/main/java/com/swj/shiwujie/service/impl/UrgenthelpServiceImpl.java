package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallConstant;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Urgenthelp;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.UrgenthelpService;
import com.swj.shiwujie.mapper.UrgenthelpMapper;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.ConverterUtils;
import com.swj.shiwujie.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
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

    @Resource
    private InnerVolunteerService innerVolunteerService;


    @Resource
    private InnerBlindService innerBlindService;


    @Resource
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

        synchronized (loginUserPhone.intern()) {
            //1. 检查是否在求助中（残留记录自动过期，2026-09-14：退出竞态/杀进程会让记录无法归位，
            //   此前会永久卡死"您已经在求助中了"）
            Urgenthelp urgenthelp = this.getWaitingByBlindId(loginBlindId);
            if (ObjUtil.isNotNull(urgenthelp)) {
                // 等待响应超 60 秒（与 App 侧自动取消时长对齐，2026-09-14 由 120s 收紧）→ 视为已过期，
                // 自动取消后放行。App 在 60s 时会调取消接口（家属端收 type=4 收回弹窗），此处兜底
                // 取消请求丢失的场景，保证取消/超时后随时可重新发起，不再撞「您已经在求助中了」
                if (System.currentTimeMillis() - urgenthelp.getStartTime().getTime() > 60_000L) {
                    urgenthelp.setHelpStatus(CallHelpStatusEnum.FALL.getHelpStatus());
                    this.updateById(urgenthelp);
                    urgenthelp = null;
                }
            }
            if (ObjUtil.isNotNull(urgenthelp)) {
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "您已经在求助中了");
            }
            urgenthelp = this.getHelpingByBlindId(loginBlindId);
            if (ObjUtil.isNotNull(urgenthelp)) {
                // 通话中超 30 分钟 → 视为遗留通话（双方均已退出但挂断未送达），自动结束
                if (System.currentTimeMillis() - urgenthelp.getStartTime().getTime() > 1_800_000L) {
                    urgenthelp.setHelpStatus(CallHelpStatusEnum.END_HELP.getHelpStatus());
                    this.updateById(urgenthelp);
                    urgenthelp = null;
                }
            }
            if (ObjUtil.isNotNull(urgenthelp)) {
                ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "您已经在求助中了");
            }
            //2. 查询是否存在家庭
            Blind blind = innerBlindService.getById(loginBlindId);
            Long familyId = blind.getFamilyId();
            ThrowUtils.throwIf(ObjUtil.isNull(familyId), ErrorCode.PARAMS_ERROR, "您没有加入家庭,无法紧急求助");
            //3. 创建数据库表
            urgenthelp = new Urgenthelp();
            urgenthelp.setBlindId(loginBlindId);
            urgenthelp.setStartTime(DateUtil.date());
            urgenthelp.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());
            urgenthelp.setFamilyId(familyId);

            this.save(urgenthelp);

            //4. 向家庭成员发起求助
            //拿到家庭成员信息
            List<Volunteer> volunteerList = innerVolunteerService.getListByFamilyId(familyId);
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
        urgenthelp.setHelpStatus(CallHelpStatusEnum.FALL.getHelpStatus());
        boolean b = this.updateById(urgenthelp);
        ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);

        //4. 向家庭成员发起求助（message 随信令下发，家属端收回弹窗并 TTS 播报）
        SocketData socketData = new SocketData();
        socketData.setBlindPhone(blind.getPhone());
        socketData.setMessage("视障人士已取消紧急求助");
        coordinationSocketHandler.cancelUrgenthelp(volunteerList, socketData);


        return true;
    }

    /**
     * 家属加入帮助
     *
     * @param loginVolunteerId 登录志愿者id
     * @param blindPhone       求助盲人ID
     * @return 是否成功
     */
    @Override
    public boolean joinUrgenthelp(String blindPhone, Long loginVolunteerId, String loginUserPhone) {

        //4. 更新求助表内容
        Blind blind = innerBlindService.getByPhone(blindPhone);
        ThrowUtils.throwIf(ObjUtil.isNull(blind), ErrorCode.PARAMS_ERROR, "求助用户不存在");
        Urgenthelp urgenthelp = this.getWaitingByBlindId(blind.getBlindId());
        ThrowUtils.throwIf(ObjUtil.isNull(urgenthelp), ErrorCode.PARAMS_ERROR, "对方没有在求助");

        // 原子抢单（2026-09-14）：两个家属同时点接听时，仅当记录仍为 WAITING 才置 HELPING，
        // 数据库行级条件更新保证只有一人成功，其余收到「已有家属接通」后收窗播报
        boolean claimed = this.update(new UpdateWrapper<Urgenthelp>()
                .eq("help_id", urgenthelp.getHelpId())
                .eq("help_status", CallHelpStatusEnum.WAITING.getHelpStatus())
                .set("volunteer_id", loginVolunteerId)
                .set("response_time", DateUtil.date())
                .set("channel_id", loginVolunteerId)
                .set("help_status", CallHelpStatusEnum.HELPING.getHelpStatus()));
        ThrowUtils.throwIf(!claimed, ErrorCode.PARAMS_ERROR, "已有家属接通，无需重复响应");

        // 已有家属接通：通知其余在线家属收回弹窗并播报（排除接听人本人）
        List<Volunteer> volunteerList = innerVolunteerService.getListByFamilyId(blind.getFamilyId());
        if (ObjUtil.isNotNull(volunteerList) && !volunteerList.isEmpty()) {
            List<Volunteer> others = new LinkedList<>();
            for (Volunteer member : volunteerList) {
                if (ObjUtil.notEqual(member.getVolunteerId(), loginVolunteerId)) {
                    others.add(member);
                }
            }
            if (!others.isEmpty()) {
                SocketData socketData = new SocketData();
                socketData.setBlindPhone(blind.getPhone());
                socketData.setMessage("已有其他家属接通，本次求助已响应");
                coordinationSocketHandler.cancelUrgenthelp(others, socketData);
            }
        }

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




