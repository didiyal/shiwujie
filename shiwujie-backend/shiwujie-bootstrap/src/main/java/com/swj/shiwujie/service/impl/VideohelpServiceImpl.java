package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallConstant;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Videohelp;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;

import com.swj.shiwujie.service.VideohelpService;
import com.swj.shiwujie.mapper.VideohelpMapper;
import com.swj.shiwujie.service.user.InnerVolunteerService;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.ConverterUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @description 针对表【VideoHelp(视频求助表)】的数据库操作Service实现
 * @createDate 2025-07-11 21:26:52
 */
@Service
public class VideohelpServiceImpl extends ServiceImpl<VideohelpMapper, Videohelp>
        implements VideohelpService {


    @Resource
    private InnerVolunteerService innerVolunteerService;

    @Resource
    RedisUtils redisUtils;


    @Autowired
    private CoordinationSocketHandler coordinationSocketHandler;


    /**
     * 志愿者加入匹配
     * redis有加入,无创建
     *
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone   志愿者手机号
     * @return 是否成功
     */
    @Override
    public boolean createVideohelp(Long loginVolunteerId, String loginUserPhone) {

        //1. 检查redis是否有队列,有则使用,无则创建
        Boolean hasKey = (Boolean) redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS);
        Queue<Long> queue = null;
        if (!hasKey) {
            queue = new LinkedList<>();
        } else {
            queue = ConverterUtils.ObjToQueueLong(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS));
        }
        //2. 检查是否在匹配中:检查redis中是否有用用户信息
        ThrowUtils.throwIf(queue.contains(loginVolunteerId), ErrorCode.PARAMS_ERROR, "您已经在匹配中了");
        //3. 检查是否在匹配/通话
        Videohelp videohelp = this.getWaitingByVolunteerId(loginVolunteerId);
        if (ObjUtil.isNotNull(videohelp)) {
            // 2026-09-14：志愿者队列 Redis TTL 仅 30s，DB 的 WAITING 记录不会自动失效——
            // 盲人未接入的残留记录会让志愿者永远无法重新入队（线上已复现）。等待超 60s 的
            // 残留记录视为已过期，自动置为「已取消」后放行重新入队。
            long ageMs = System.currentTimeMillis() - videohelp.getStartTime().getTime();
            if (ageMs <= 60_000L) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已在匹配中，请稍候");
            }
            videohelp.setHelpStatus(CallHelpStatusEnum.FALL.getHelpStatus());
            this.updateById(videohelp);
        }
        synchronized (loginUserPhone.intern()) {
            //4. 将志愿者信息加入到队列中
            queue.offer(loginVolunteerId);

            //6. 新建匹配表,设置初始信息
            videohelp = new Videohelp();
            videohelp.setVolunteerId(loginVolunteerId);
            videohelp.setChannelId(loginVolunteerId);
            videohelp.setStartTime(DateUtil.date());
            videohelp.setHelpStatus(CallHelpStatusEnum.WAITING.getHelpStatus());
            boolean b = this.save(videohelp);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);


            //5. 志愿者信息上传redis
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS, queue, CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return true;
    }

    /**
     * 志愿者退出匹配
     *
     * @param loginVolunteerId 登录志愿者id
     * @param loginUserPhone   志愿者手机号
     * @return 是否成功
     */
    @Override
    public boolean removeVolunteerFromVideohelp(Long loginVolunteerId, String loginUserPhone) {

        //1. 检查redis是否有队列,有则使用,无则报错
        Boolean hasKey = (Boolean) redisUtils.hasKey(CallConstant.VOLUNTEER_QUEUE_REDIS);
        if (!hasKey) {
            // 队列不存在,直接拒绝（修复：原代码对 null queue 调用 contains 必现 NPE）
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "您不在匹配之中,无法取消匹配");
        }
        Queue<Long> queue = ConverterUtils.ObjToQueueLong(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS));
        //2. 检查是否在匹配中:检查redis中是否有用用户信息
        ThrowUtils.throwIf(!queue.contains(loginVolunteerId), ErrorCode.PARAMS_ERROR);

        //3. 删除队列中的信息
        queue.remove(loginVolunteerId);

        Videohelp videohelp = this.getWaitingByVolunteerId(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(videohelp), ErrorCode.PARAMS_ERROR);

        synchronized (loginUserPhone.intern()) {

            //5. 修改匹配表信息
            videohelp.setHelpStatus(CallHelpStatusEnum.FALL.getHelpStatus());
            boolean b = this.updateById(videohelp);
            ThrowUtils.throwIf(!b, ErrorCode.SYSTEM_ERROR);


            //4. 志愿者信息上传redis
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS, queue, CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return true;
    }

    /**
     * 视障人士加入匹配
     *
     * @param loginBlindId   登录视障人士id
     * @param loginUserPhone 登录手机号
     * @return 是否成功
     */
    @Override
    public boolean joinVideohelp(Long loginBlindId, String loginUserPhone) {

        //1. 检查是否有志愿者
        Object fromRedis = redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS);
        ThrowUtils.throwIf(ObjUtil.isNull(fromRedis), ErrorCode.PARAMS_ERROR, "没有空闲的志愿者");
        //2. 获取队列
        Queue<Long> queue = ConverterUtils.ObjToQueueLong(fromRedis);
        synchronized (loginUserPhone.intern()) {
            // 重复发起防护（2026-09-15）：已有进行中的通话 → 拒绝重复匹配，防盲人连点
            // 造成两个志愿者同时进视频（各占一个频道，只对首呼者可见）。遗留 HELPING
            // （双方异常退出未挂断）超 30 分钟自动结束放行，与紧急求助侧同策略。
            List<Videohelp> helpingList = this.getHelpingByBlindId(loginBlindId);
            if (helpingList != null && !helpingList.isEmpty()) {
                for (Videohelp helping : helpingList) {
                    java.util.Date effective = helping.getResponseTime() != null
                            ? helping.getResponseTime() : helping.getStartTime();
                    if (effective != null && System.currentTimeMillis() - effective.getTime() <= 1_800_000L) {
                        ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "您已在通话中，请勿重复发起");
                    }
                    helping.setEndTime(DateUtil.date());
                    helping.setHelpStatus(CallHelpStatusEnum.END_HELP.getHelpStatus());
                    this.updateById(helping);
                }
            }
            // 2026-09-14：原子化匹配——逐个候选尝试，志愿者 WS 不在线时跳过并标记其记录已取消，
            // 继续尝试下一位。此前"先改状态后通知、失败不回滚"会让记录卡在处理中、
            // 队列状态与 DB 脱节，后续匹配全部失败（线上已复现）。
            while (!queue.isEmpty()) {
                Long volunteerId = queue.poll();
                Videohelp videohelp = this.getWaitingByVolunteerId(volunteerId);
                if (ObjUtil.isNull(videohelp)) {
                    continue; // 候选无等待记录（残留/已消费），跳过
                }
                videohelp.setBlindId(loginBlindId);
                videohelp.setResponseTime(DateUtil.date());
                videohelp.setHelpStatus(CallHelpStatusEnum.HELPING.getHelpStatus());
                videohelp.setChannelId(volunteerId);
                this.updateById(videohelp);

                // 向志愿者发送socket消息（type=2 视频初始化通知）
                SocketData socketData = new SocketData();
                socketData.setRequestType(2);
                socketData.setBlindPhone(loginUserPhone);
                socketData.setVolunteerPhone(innerVolunteerService.getById(volunteerId).getPhone());
                socketData.setChannelId(volunteerId);
                try {
                    coordinationSocketHandler.matchSuccess(socketData);
                } catch (Exception e) {
                    // 志愿者不在线：本候选匹配失败——标记记录已取消，尝试队列中的下一位
                    log.warn("志愿者不在线，跳过并尝试下一位: " + e.getMessage());
                    videohelp.setBlindId(null);
                    videohelp.setResponseTime(null);
                    videohelp.setHelpStatus(CallHelpStatusEnum.FALL.getHelpStatus());
                    this.updateById(videohelp);
                    continue;
                }

                // 匹配成功：保存剩余队列
                redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS, queue, CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS, TimeUnit.SECONDS);
                return true;
            }

            // 队列耗尽（全部不在线或为空）：保存剩余队列并如实告知
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS, queue, CallConstant.VOLUNTEER_QUEUE_TTL_SECONDS, TimeUnit.SECONDS);
            ThrowUtils.throwIf(true, ErrorCode.PARAMS_ERROR, "没有空闲的志愿者");
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
    public Videohelp getByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
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
    public Videohelp getWaitingByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
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
    public List<Videohelp> getHelpingByVolunteerId(Long volunteerId) {
        if (ObjUtil.isNull(volunteerId)) {
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id", volunteerId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.list(queryWrapper);
    }


    /**
     * 通过志愿者id查询信息
     *
     * @param blindId 志愿者id
     * @return 表信息
     */
    @Override
    public Videohelp getByBlindId(Long blindId) {
        if (ObjUtil.isNull(blindId)) {
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id", blindId);
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    @Override
    public List<Videohelp> getHelpingByBlindId(Long blindId) {
        if (ObjUtil.isNull(blindId)) {
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id", blindId);
        queryWrapper.eq("help_status", CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.list(queryWrapper);
    }

    //endregion

}




