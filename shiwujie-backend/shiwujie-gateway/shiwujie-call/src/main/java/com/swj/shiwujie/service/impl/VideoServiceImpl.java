package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.VideoVO;
import com.swj.shiwujie.model.domain.Video;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.enums.UserCallStatusEnum;
import com.swj.shiwujie.model.enums.VideoStatusEnum;
import com.swj.shiwujie.service.InnerUserService;
import com.swj.shiwujie.service.VideoService;
import com.swj.shiwujie.mapper.VideoMapper;
import com.swj.shiwujie.utils.VideoQueueUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

import static com.swj.shiwujie.constants.VideoConstants.CHANNEL_KEY;
import static com.swj.shiwujie.constants.VideoConstants.VIDEO_QUEUE;
import static com.swj.shiwujie.model.enums.VideoStatusEnum.CALLING;
import static com.swj.shiwujie.model.enums.VideoStatusEnum.WAITING_CALL;

/**
 * @author Administrator
 * @description 针对表【channels(视频通话频道表)】的数据库操作Service实现
 * @createDate 2025-03-23 05:29:41
 */
@Service
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video>
        implements VideoService {

    @DubboReference
    private InnerUserService innerUserService;


    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    /**
     * 线程池
     */
    private ExecutorService executorService = new ThreadPoolExecutor(40, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));


    /**
     * 通过用户信息生成频道号
     *
     * @param uid
     * @return
     */
    @Override
    public String generateChannelByUid(String uid) {
        if (uid == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return CHANNEL_KEY + uid;
    }

    /**
     *盲人进入通话
     *获取盲人视频通话的频道
     * @param currentUserId
     * @return
     */
    @Override
    public VideoVO getBlindChannelByUserId(Long currentUserId) {

        //1. 拿到队列获取是否有志愿者,有就拿到channel,同步删掉那个队列的channel信息
        if(!redisTemplate.hasKey(VIDEO_QUEUE)){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"没有空闲的志愿者");
        }
        Queue<String> queue = VideoQueueUtils.convertToQueue(redisTemplate.opsForValue().get(VIDEO_QUEUE));

        //  从队列中拿到channel
        log.debug("在线的志愿者数量:" + queue.size());
        if (queue.size() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "没有空闲的志愿者");
        }
        String channel = queue.poll();
        //  更新redis,设置过期时间为120小时
        redisTemplate.opsForValue().set(VIDEO_QUEUE, queue, 432000L, TimeUnit.SECONDS);

        //2. 生成自己的uid
        String uid = this.generateUid(currentUserId);


        //3. 数据库表更新信息

        //开启线程
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            log.debug("开始使用线程运行" + Thread.currentThread().getName());
            //  3.1. video表中的视频通话字段修改状态为正在通话
            //更新频道表状态,寻找频道表
            QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("channel", channel).eq("status", WAITING_CALL.getValue());
            Video video = this.getOne(queryWrapper);
            if (video == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "频道状态错误");
            }
            //更新频道表,开始计时视频通话
            video.setStatus(1);
            video.setBeginTime(this.formatDate(LocalDateTime.now()));
            video.setBlindUid(uid);
            boolean b = this.updateById(video);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "频道表更新错误");
            }
            //  3.2. user表修改自己与志愿者的状态
            User user = innerUserService.getById(currentUserId);
            //先通过channel拿到并修改志愿者信息
            User volunteer = innerUserService.getByChannel(channel);
            volunteer.setCallStatus(UserCallStatusEnum.CALLING.getValue());
            innerUserService.updateById(volunteer);
            //修改自己的信息
            user.setCallChannel(channel);
            user.setCallStatus(UserCallStatusEnum.CALLING.getValue());
            innerUserService.updateById(user);
            log.debug("线程运行结束" + Thread.currentThread().getName());
        }, executorService);
        CompletableFuture.allOf(future).join();

        //4. 返回channel与uid给前端
        VideoVO res = new VideoVO();
        res.setChannel(channel);
        res.setUid(uid);
        return res;

    }

    /**
     * 志愿者退出通话
     *更新状态,修改志愿者与盲人表的状态
     * @param currentUserId
     * @return
     */
    @Override
    public Boolean leaveVolunteerChannelByUserId(Long currentUserId) {
        //拿到用户信息
        User user = innerUserService.getById(currentUserId);
        //先获取video表的频道数据
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("channel", user.getCallChannel()).in("status", WAITING_CALL.getValue(),CALLING.getValue());
        Video video = this.getOne(queryWrapper);

        //判断类型1. 接听前离开
        if (video != null && WAITING_CALL.getValue().equals(video.getStatus())) {
            //   1. 将队列中自己的channel删除并更新队列
            //拿到queue
            Queue<String> queue = VideoQueueUtils.convertToQueue(redisTemplate.opsForValue().get(VIDEO_QUEUE));
            String queueChannel = this.generateChannelByUid(this.generateUid(currentUserId));
            queue.remove(queueChannel);
            //设置过期时间为120小时
            redisTemplate.opsForValue().set(VIDEO_QUEUE, queue, 432000L, TimeUnit.SECONDS);
            //   2. 更新数据库表信息
            //      1. video表的视频通话信息改状态
            video.setStatus(VideoStatusEnum.CALL_CANCEL.getValue());
            this.updateById(video);
            //      2. user表修改自己的通话状态
            user.setCallStatus(UserCallStatusEnum.NO_CALLING.getValue());
            user.setCallChannel("");
            innerUserService.updateById(user);

        } else if (video != null &&  CALLING.getValue().equals(video.getStatus())) {
            //2. 接听后离开
            //   1. 更新数据库表信息
            //      1. video表的视频通话信息修改
            video.setStatus(VideoStatusEnum.CALL_END.getValue());
            video.setEndTime(this.formatDate(LocalDateTime.now()));
            //todo 视频耗时计算
            boolean b = this.updateById(video);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "频道表更新错误");
            }
            //      2. user表的盲人与志愿者状态修改
            //拿到通道信息
            String channel = video.getChannel();
            //修改盲人与志愿者的信息
            innerUserService.updateCallUsersInformation(channel);
        }

        //创建返回类
        return true;
    }

    /**
     * 盲人退出通话
     * 更新状态
     *
     * @param currentUserId
     * @return
     */
    @Override
    public Boolean leaveBlindChannelByUserId(Long currentUserId) {
        //拿到用户信息
        User user = innerUserService.getById(currentUserId);
        //1. 更新数据库表信息
        //   1. video表的视频通话信息修改
        //如果频道表没修改更新频道表状态,寻找频道表
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("channel", user.getCallChannel()).eq("status", CALLING.getValue());
        Video video = this.getOne(queryWrapper);
        if (video != null) {
            //频道表还没被修改
            //更新频道表,停止计时视频通话
            video.setStatus(2);
            video.setEndTime(this.formatDate(LocalDateTime.now()));
            //todo 视频耗时计算
            boolean b = this.updateById(video);
            if (!b) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "频道表更新错误");
            }
        }
        //   2. user表的盲人与志愿者状态修改
        //拿到通道信息
        String channel = video.getChannel();
        //修改盲人与志愿者的信息
        innerUserService.updateCallUsersInformation(channel);

        //2. 返回true与fasle
        return true;

    }

    /**
     * 志愿者进入视频通话,获取uid与channel
     *
     * @param currentUserId
     * @return
     */
    @Override
    public VideoVO getVolunteerChannelByUserId(Long currentUserId) {
        // 1. 生成channel与uid
        String uid = this.generateUid(currentUserId);
        String channel = this.generateChannelByUid(uid);
        // 2. 将视频通话队列中添加自己的channel,并更新
        if (Boolean.FALSE.equals(redisTemplate.hasKey(VIDEO_QUEUE))) {//拿到队列信息
            Queue<String> queue = new LinkedList<>();
            //将视频通话的队列存入redis,12h
            redisTemplate.opsForValue().set(VIDEO_QUEUE, queue, 432000L, TimeUnit.SECONDS);
        }
        Queue<String> queue = VideoQueueUtils.convertToQueue(redisTemplate.opsForValue().get(VIDEO_QUEUE));
        //将频道信息添加到队列之中
        queue.offer(channel);
        //设置过期时间为120小时,并更新队列到redis
        redisTemplate.opsForValue().set(VIDEO_QUEUE, queue, 432000L, TimeUnit.SECONDS);


        // 3. 数据库更新数据

        //开启线程,向数据库中添加video消息,更新用户表的状态信息
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            log.debug("开始使用线程运行" + Thread.currentThread().getName());

//   3.1. video创建新的消息
            Video video = new Video();
            video.setChannel(channel);
            video.setVolunteerUid(uid);
            boolean save = this.save(video);
            if (!save) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "频道新建失败");
            }
            //   3.2. user表更新自己的状态信息
            User user = innerUserService.getById(currentUserId);
            user.setCallStatus(UserCallStatusEnum.WAIT_CALL.getValue());
            user.setCallChannel(channel);
            innerUserService.updateById(user);
            log.debug("线程运行结束" + Thread.currentThread().getName());
        }, executorService);
        CompletableFuture.allOf(future).join();

        // 4. 返回channel与uid给前端
        VideoVO res = new VideoVO();
        res.setChannel(channel);
        res.setUid(uid);
        return res;
    }


    /**
     * 获取一个针对数据库datetime的字符串
     *
     * @param date
     * @return
     */
    public String formatDate(LocalDateTime date) {
        String format = LocalDateTimeUtil.format(date, "yyyy-MM-dd");
        return format;
    }

    /**
     * 生成uid
     *
     * @param id
     * @return
     */
    @Override
    public String generateUid(Long id) {
        //目前使用一个八位数字,id其余用0来补齐
        String s = Long.toString(id);
        StringBuilder ans = new StringBuilder(s);
        for (int i = 0; i < (8 - s.length()); i++) {
            ans.append("0");
        }
        return ans.toString();
    }
}




