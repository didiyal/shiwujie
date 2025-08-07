package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.user.InnerFamilyService;
import com.swj.shiwujie.utils.LoginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 用户模块业务工具调用
 */
@Component
@Slf4j
public class UserTools {


    @Lazy
    @DubboReference
    private InnerFamilyService innerFamilyService;


    /**
     * 申请加入家庭
     * @param familyVolunteerPhone 家庭创建人手机号
     * @return 申请结果
     */
    @Tool(description = "APP功能操作:申请加入家庭(需要让用户确认)")
    public String joinFamily(@ToolParam (description = "家庭创建人手机号") String familyVolunteerPhone){
        try {
            Blind loginBlind = LoginUtils.getLoginBlind();
            ThrowUtils.throwIf(StrUtil.isBlankIfStr(familyVolunteerPhone), ErrorCode.PARAMS_ERROR,"请输入家庭创建人手机号");
            boolean b = innerFamilyService.joinFamily(familyVolunteerPhone, loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            if(b) return "申请加入家庭成功,请等待返回结果";
            else return "申请加入家庭失败";
        } catch (Exception e) {
            log.error("申请加入家庭失败",e);
            return "申请加入家庭失败"+e.getMessage();
        }
    }



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


}
