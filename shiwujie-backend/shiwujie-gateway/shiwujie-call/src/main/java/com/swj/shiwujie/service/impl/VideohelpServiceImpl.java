package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallConstant;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.call.Videohelp;
import com.swj.shiwujie.model.enums.call.CallHelpStatusEnum;
import com.swj.shiwujie.model.request.call.SocketData;
import com.swj.shiwujie.service.InnerVolunteerService;
import com.swj.shiwujie.service.VideohelpService;
import com.swj.shiwujie.mapper.VideohelpMapper;
import com.swj.shiwujie.socket.CoordinationSocketHandler;
import com.swj.shiwujie.utils.ConverterUtils;
import com.swj.shiwujie.utils.RedisUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.Queue;

/**
* @author Administrator
* @description 针对表【VideoHelp(视频求助表)】的数据库操作Service实现
* @createDate 2025-07-11 21:26:52
*/
@Service
public class VideohelpServiceImpl extends ServiceImpl<VideohelpMapper, Videohelp>
    implements VideohelpService{


    @DubboReference
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
        if(!hasKey){
            queue = new LinkedList<>();
        }else{
            queue = ConverterUtils.ObjToQueueLong(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS));
        }
        //2. 检查是否在匹配中:检查redis中是否有用用户信息
        ThrowUtils.throwIf(queue.contains(loginVolunteerId), ErrorCode.PARAMS_ERROR,"您已经在匹配中了");
        //3. 检查是否在通话
        Videohelp videohelp = this.getWaitingByVolunteerId(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNotNull(videohelp),ErrorCode.PARAMS_ERROR);
        synchronized (loginUserPhone.intern()){
            //4. 将志愿者信息加入到队列中
            queue.offer(loginVolunteerId);

            //6. 新建匹配表,设置初始信息
            videohelp = new Videohelp();
            videohelp.setVolunteer_id(loginVolunteerId);
            videohelp.setChannel_id(loginVolunteerId);
            videohelp.setStart_time(DateUtil.date());
            videohelp.setHelp_status(CallHelpStatusEnum.WAITING.getHelpStatus());
            boolean b = this.save(videohelp);
            ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);



            //5. 志愿者信息上传redis
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS,queue,30L);
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
        Queue<Long> queue = null;
        if(!hasKey){
            ThrowUtils.throwIf(queue.contains(loginVolunteerId), ErrorCode.PARAMS_ERROR,"您不在匹配之中,无法取消匹配");
        }else{
            queue = ConverterUtils.ObjToQueueLong(redisUtils.getFromRedis(CallConstant.VOLUNTEER_QUEUE_REDIS));
        }
        //2. 检查是否在匹配中:检查redis中是否有用用户信息
        ThrowUtils.throwIf(!queue.contains(loginVolunteerId), ErrorCode.PARAMS_ERROR);

        //3. 删除队列中的信息
        queue.remove(loginVolunteerId);

        Videohelp videohelp = this.getWaitingByVolunteerId(loginVolunteerId);
        ThrowUtils.throwIf(ObjUtil.isNull(videohelp),ErrorCode.PARAMS_ERROR);

        synchronized (loginUserPhone.intern()){

            //5. 修改匹配表信息
            videohelp.setHelp_status(CallHelpStatusEnum.FALL.getHelpStatus());
            boolean b = this.updateById(videohelp);
            ThrowUtils.throwIf(!b,ErrorCode.SYSTEM_ERROR);


            //4. 志愿者信息上传redis
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS,queue,30L);
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
        ThrowUtils.throwIf(ObjUtil.isNull(fromRedis),ErrorCode.PARAMS_ERROR,"没有空闲的志愿者");
        //2. 获取志愿者信息
        Queue<Long> queue = ConverterUtils.ObjToQueueLong(fromRedis);
        Long volunteerId = queue.poll();
        synchronized (loginUserPhone.intern()){
            //4. 更新求助表内容
            Videohelp videohelp = this.getWaitingByVolunteerId(volunteerId);
            ThrowUtils.throwIf(ObjUtil.isNull(videohelp),ErrorCode.PARAMS_ERROR);
            videohelp.setBlind_id(loginBlindId);
            videohelp.setResponse_time(DateUtil.date());
            videohelp.setHelp_status(CallHelpStatusEnum.HELPING.getHelpStatus());
            videohelp.setChannel_id(volunteerId);
            this.updateById(videohelp);



            //5. 向志愿者发送socket消息
            SocketData socketData = new SocketData();
            socketData.setRequestType(2);
            socketData.setBlindPhone(loginUserPhone);
            socketData.setVolunteerPhone(innerVolunteerService.getById(volunteerId).getPhone());
            socketData.setChannelId(volunteerId);
            coordinationSocketHandler.matchSuccess(socketData);


            //3. 更新redis
            redisUtils.setToRedis(CallConstant.VOLUNTEER_QUEUE_REDIS,queue,30L);



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
        if(ObjUtil.isNull(volunteerId)){
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id",volunteerId);
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
        if(ObjUtil.isNull(volunteerId)){
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id",volunteerId);
        queryWrapper.eq("help_status",CallHelpStatusEnum.WAITING.getHelpStatus());
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param volunteerId 志愿者id
     * @return 表信息
     */
    @Override
    public Videohelp getHelpingByVolunteerId(Long volunteerId) {
        if(ObjUtil.isNull(volunteerId)){
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("volunteer_id",volunteerId);
        queryWrapper.eq("help_status",CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.getOne(queryWrapper);
    }




    /**
     * 通过志愿者id查询信息
     *
     * @param blindId 志愿者id
     * @return 表信息
     */
    @Override
    public Videohelp getByBlindId(Long blindId) {
        if(ObjUtil.isNull(blindId)){
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id",blindId);
        return this.getOne(queryWrapper);
    }


    /**
     * 通过志愿者id查询正在通话的信息
     *
     * @param blindId 视障人士id
     * @return 表信息
     */
    @Override
    public Videohelp getHelpingByBlindId(Long blindId) {
        if(ObjUtil.isNull(blindId)){
            return null;
        }
        QueryWrapper<Videohelp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("blind_id",blindId);
        queryWrapper.eq("help_status",CallHelpStatusEnum.HELPING.getHelpStatus());
        return this.getOne(queryWrapper);
    }

    //endregion

}




