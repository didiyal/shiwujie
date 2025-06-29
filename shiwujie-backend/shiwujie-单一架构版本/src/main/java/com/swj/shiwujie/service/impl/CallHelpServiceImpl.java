package com.swj.shiwujie.service.impl;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.constants.CallHelpConstants;
import com.swj.shiwujie.exception.BusinessException;
import com.swj.shiwujie.model.VO.CallHelpVO;
import com.swj.shiwujie.model.domain.CallHelp;
import com.swj.shiwujie.model.domain.User;
import com.swj.shiwujie.model.domain.Video;
import com.swj.shiwujie.model.enums.CallHelpStatusEnum;
import com.swj.shiwujie.model.enums.UserCallStatusEnum;
import com.swj.shiwujie.model.enums.VideoStatusEnum;
import com.swj.shiwujie.model.request.CallHelpJoinRequest;
import com.swj.shiwujie.service.CallHelpService;
import com.swj.shiwujie.mapper.CallHelpMapper;
import com.swj.shiwujie.service.FamilyService;
import com.swj.shiwujie.service.UserService;
import com.swj.shiwujie.utils.VideoQueueUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.swj.shiwujie.constants.VideoConstant.VIDEO_QUEUE;
import static com.swj.shiwujie.model.enums.VideoStatusEnum.CALLING;
import static com.swj.shiwujie.model.enums.VideoStatusEnum.WAITING_CALL;

/**
 * @author ldl
 * @description 针对表【callHelp(视频通话频道表)】的数据库操作Service实现
 * @createDate 2025-04-12 23:35:39
 */
@Service
public class CallHelpServiceImpl extends ServiceImpl<CallHelpMapper, CallHelp>
        implements CallHelpService {

    @Resource
    private UserService userService;

    @Resource
    private FamilyService familyService;


    /**
     * 盲人紧急求助
     * 向家人求助
     *
     * @param request
     * @return
     */
    @Override
    public CallHelpVO getCallHelpBlindChannel(HttpServletRequest request) {
        User blindUser = userService.getUserByRequest(request);
        //1. 生成channel与uid
        String uid = this.generateUid();
        String channel = this.generateChannel(uid, blindUser.getUserAccount());
        //2. 创建一个CallHelp表的消息
        //   1. 求助人id与求助人uid,channel放入,status为0
        CallHelp callHelpContent = this.createNewCallHelpContent(blindUser, channel, uid);
        //3. 修改user表的callStatus和callChannel内容
        blindUser.setCallStatus(UserCallStatusEnum.WAIT_CALL.getValue());
        blindUser.setCallChannel(channel);
        userService.updateById(blindUser);
        //4. 返回结果
        return new CallHelpVO(channel, uid);
    }

    /**
     * 加入求助者视频
     *
     * @param callHelpJoinRequest 求助者uid
     * @param request
     * @return
     */
    @Override
    public CallHelpVO joinCallHelpBlindChannel(CallHelpJoinRequest callHelpJoinRequest, HttpServletRequest request) {
        //拿到求助人信息
        User helpUser = userService.getUserByRequest(request);
        //1. 在callHelp表里通过求助人uid,status寻找消息
        CallHelp callHelp = this.getCallHelpWaitBlindByBlindUid(callHelpJoinRequest.getBlindUid());
        //2. 根据求助人uid,家庭id,若找不到代表已经有人帮助或者取消
        //   1. 生成uid
        String uid = this.generateUid();
        //   2. 拿到channel
        String channel = callHelp.getChannel();
        //   3. 更新callHelp的帮助家属uid,帮助家属的id,status
        callHelp.setHelpOthersId(helpUser.getId().toString());
        callHelp.setHelpOthersUid(uid);
        callHelp.setStatus(CallHelpStatusEnum.CALL_HELPING.getValue());
        // todo 时间处理
        callHelp.setBeginTime(new Date());
        boolean b = this.updateById(callHelp);
        if(!b){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        //   4. 修改user表自己与盲人的callStatus与callChannel
        User assistedBlind = userService.getByChannel(channel);
        assistedBlind.setCallStatus(UserCallStatusEnum.CALLING.getValue());
        helpUser.setCallStatus(UserCallStatusEnum.CALLING.getValue());
        helpUser.setCallChannel(channel);
        userService.updateById(helpUser);
        userService.updateById(assistedBlind);
        //3. 返回uid与channel
        return new CallHelpVO(channel,uid);

    }

    /**
     * 求助的盲人挂断通话
     * 更新双方状态
     *
     * @param request
     * @return
     */
    @Override
    public Boolean leaveCallHelpByBlind(HttpServletRequest request) {
        //拿到用户信息
        User blindUser = userService.getUserByRequest(request);
        //拿到callHelp的信息
        QueryWrapper<CallHelp> callHelpQueryWrapper = new QueryWrapper<>();
        callHelpQueryWrapper.eq("blindId",blindUser.getId()).in("status",
                CallHelpStatusEnum.CALL_HELPING.getValue(),
                CallHelpStatusEnum.WAITING_CALL_HELP.getValue());
        CallHelp callHelp = this.getOne(callHelpQueryWrapper);
        Integer status = callHelp.getStatus();
        //1. 求助帮助开始前挂断,status为0
        if(Objects.equals(status, CallHelpStatusEnum.WAITING_CALL_HELP.getValue())){
            //   1. 修改callHelp表的内容
            //      1. status
            callHelp.setStatus(CallHelpStatusEnum.CALL_HELP_CANCEL.getValue());
            this.updateById(callHelp);
            //   2. 修改user表的内容
            blindUser.setCallStatus(UserCallStatusEnum.NO_CALLING.getValue());
            blindUser.setCallChannel("");
            userService.updateById(blindUser);
        }
        //2. 求助帮助结束挂断
        else if (Objects.equals(status, CallHelpStatusEnum.CALL_HELPING.getValue())) {
            //   1. 修改callHelp表的内容
            //      1. status
            //      2. beginTime
            //      3. endTime
            callHelp.setStatus(CallHelpStatusEnum.CALL_HELP_END.getValue());
            callHelp.setEndTime(new Date());
            callHelp.setCallTime(DateUtil.between(callHelp.getBeginTime(),callHelp.getEndTime(), DateUnit.MINUTE));
            this.updateById(callHelp);
            //   2. 修改自己与求助人的user表内容
            //修改盲人与志愿者的信息
            userService.updateCallUsersInformation(callHelp.getChannel());
        }

        //3. 返回true或者fasle
        //创建返回类
        return true;
    }

    /**
     * 协助家属主动挂断通信
     *
     * @param request
     * @return
     */
    @Override
    public Boolean leaveCallHelpByHelpUser(HttpServletRequest request) {
        //拿到用户信息
        User helpUser = userService.getUserByRequest(request);
        //拿到callHelp的信息
        QueryWrapper<CallHelp> callHelpQueryWrapper = new QueryWrapper<>();
        callHelpQueryWrapper.eq("helpOthersId",helpUser.getId().toString()).eq("status",CallHelpStatusEnum.CALL_HELPING.getValue());
        CallHelp callHelp = this.getOne(callHelpQueryWrapper);
        //2. 求助帮助结束挂断
        if (callHelp != null) {
            //   1. 修改callHelp表的内容
            //      1. status
            //      2. beginTime
            //      3. endTime
            callHelp.setStatus(CallHelpStatusEnum.CALL_HELP_END.getValue());
            callHelp.setEndTime(new Date());
            callHelp.setCallTime(DateUtil.between(callHelp.getBeginTime(),callHelp.getEndTime(), DateUnit.MINUTE));
            this.updateById(callHelp);
            //   2. 修改自己与求助人的user表内容
            //修改盲人与志愿者的信息
            userService.updateCallUsersInformation(callHelp.getChannel());
        }

        //3. 返回true或者fasle
        //创建返回类
        return true;
    }


    //-----------------------------------------------------------------------------------------------------------------


    /**
     * 创建一个初始的callHelp表信息
     *
     * @param blindUser 盲人信息
     * @param channel   频道
     * @param uid       uid
     * @return
     */
    @Override
    public CallHelp createNewCallHelpContent(User blindUser, String channel, String uid) {
        CallHelp callHelp = new CallHelp();
        //家庭校验
        Long familyId = blindUser.getFamilyId();
        if (familyId == null || familyId == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "需要加入家庭才可以紧急求助");
        }
        if (!familyService.familyUsersVerify(familyId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您的家庭没有可以求助的用户");
        }
        callHelp.setFamilyId(familyId);
        callHelp.setBlindId(blindUser.getId());
        callHelp.setBlindUid(uid);
        callHelp.setChannel(channel);
        callHelp.setStatus(0);
        boolean save = this.save(callHelp);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return callHelp;
    }

    /**
     * 生成一个uid
     *
     * @return
     */
    @Override
    public String generateUid() {
        //生成的是不带-的字符串，类似于：b17f24ff026d40949c85a24f4f375d42
        return IdUtil.simpleUUID();
    }

    /**
     * 生成channel
     *
     * @param uid
     * @param userAccount
     * @return
     */
    @Override
    public String generateChannel(String uid, String userAccount) {
        return CallHelpConstants.CHANNEL_KEY + uid + userAccount;
    }

    /**
     * 获取正在求助未被接通的盲人信息
     * 若已经被接通或者取消了求助,爆异常
     * @param blindUid
     * @return
     */
    @Override
    public CallHelp getCallHelpWaitBlindByBlindUid(String blindUid) {
        QueryWrapper<CallHelp> callHelpQueryWrapper = new QueryWrapper<>();
        callHelpQueryWrapper.eq("blindUid", blindUid)
                .in("status",
                        CallHelpStatusEnum.WAITING_CALL_HELP.getValue(),
                        CallHelpStatusEnum.CALL_HELPING.getValue(),
                        CallHelpStatusEnum.CALL_HELP_CANCEL.getValue());
        CallHelp callHelp = this.getOne(callHelpQueryWrapper);
        if (callHelp == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        Integer status = callHelp.getStatus();
        if(Objects.equals(status, CallHelpStatusEnum.CALL_HELP_CANCEL.getValue())){
            throw new BusinessException(ErrorCode.USER_ERROR,"对面已经取消了求助");
        }
        if(Objects.equals(status, CallHelpStatusEnum.CALL_HELPING.getValue())){
            throw new BusinessException(ErrorCode.USER_ERROR,"对方正在通话中");
        }
        return callHelp;
    }
}




