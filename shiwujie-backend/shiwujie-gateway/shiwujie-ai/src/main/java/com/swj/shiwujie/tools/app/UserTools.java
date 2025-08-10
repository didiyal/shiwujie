package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.user.InnerFamilyService;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户模块业务工具调用
 */
@Component
@Slf4j
public class UserTools {


    @DubboReference
    private InnerFamilyService innerFamilyService;


    /**
     * 申请加入家庭
     * @param familyVolunteerPhone 家庭创建人手机号
     * @return 申请结果
     */
    @Tool(name = "申请加入家庭", description = "当用户提供了家庭创建人手机号并说\"我确认加入家庭\"时调用此工具。")
    public String joinFamily(@ToolParam (description = "家庭创建人手机号") String familyVolunteerPhone){
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            ThrowUtils.throwIf(StrUtil.isBlankIfStr(familyVolunteerPhone), ErrorCode.PARAMS_ERROR,"请输入家庭创建人手机号");
            boolean b = innerFamilyService.joinFamily(familyVolunteerPhone, loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            if(b) return "正在为您申请加入家庭，请稍候。申请成功后会通知您结果。";
            else return "申请加入家庭失败";
        } catch (Exception e) {
            log.error("申请加入家庭失败",e);
            return "申请加入家庭失败"+e.getMessage();
        }
    }


    /**
     * 退出家庭
     * @return 退出结果
     */
    @Tool(description = "APP功能操作:退出家庭(需要让用户确认)")
    public String leaveFromFamily(){
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            boolean b = innerFamilyService.userLeaveFromFamily(loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            if(b) return "退出家庭成功";
            else return "退出家庭失败";
        } catch (Exception e) {
            log.error("退出家庭失败",e);
            return "退出家庭失败"+e.getMessage();
        }
    }


    /**
     * 获取用户的家庭信息
     * @return 家庭信息
     */
    @Tool(description = "APP功能操作:获取用户的家庭信息")
    public String getFamilyInfo(){
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            FamilyVO familyVO = innerFamilyService.getFamilyVOById(loginBlind.getFamilyId(), LoginUtils.getLoginUserPhone());
            List<BlindVO> blindVOList = familyVO.getBlindVOList();
            List<VolunteerVO> volunteerVOList = familyVO.getVolunteerVOList();
            StringBuilder sb = new StringBuilder();
            sb.append("家庭名称：").append(familyVO.getFamilyName()).append("\n")
                    .append("家庭描述：").append(familyVO.getFamilyDescription()).append("\n")
                    .append("家庭创建人：").append(familyVO.getCreatorVolunteer().getName()).append("\n")
                    .append("家庭成员数量：").append(blindVOList.size()+volunteerVOList.size());
                    // 家庭视障人士
            for (BlindVO blindVO : blindVOList){
                sb.append("\n").append("家庭成员：").append(blindVO.getName()).append("，手机号：").append(blindVO.getPhone());
            }
            for (VolunteerVO volunteerVO : volunteerVOList){
                sb.append("\n").append("家庭成员：").append(volunteerVO.getName()).append("，手机号：").append(volunteerVO.getPhone());
            }

            return "当前家庭信息为：" + sb.toString();
        } catch (Exception e) {
            log.error("获取家庭信息失败",e);
            return "获取家庭信息失败"+e.getMessage();
        }
    }



    @Tool(description = "APP功能操作:加入社区")
    public String joinCommunity(){
        //todo 页面通知前端页面跳转
        return "社区只能手动加入,正在帮您跳转页面";
    }


}
