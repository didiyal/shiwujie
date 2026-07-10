package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.user.InnerFamilyService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户模块业务工具调用
 */
@Component
@Slf4j
public class UserTools {


    @Resource
    private InnerFamilyService innerFamilyService;


    /**
     * 申请加入家庭
     *
     * @param familyVolunteerPhone 家庭创建人手机号
     * @return 申请结果
     */
    @Tool(name = "Apply to join family", description = "Help users complete the process of joining a family. This tool is called after the user provides the family creator's phone number and confirms.")
    public String joinFamily(@ToolParam(description = "Family creator's phone number, used to apply to join the family") String familyVolunteerPhone) {
        try {
            log.info("用户申请加入家庭");
            Blind loginBlind = LoginUtils.getLoginBlind();
            Long familyId = loginBlind.getFamilyId();
            if(ObjUtil.isNotNull(familyId)){
                return "您已加入家庭了,无需重复加入家庭";
            }
            ThrowUtils.throwIf(StrUtil.isBlankIfStr(familyVolunteerPhone), ErrorCode.PARAMS_ERROR, "请输入家庭创建人手机号");
            boolean b = innerFamilyService.joinFamily(familyVolunteerPhone, loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            if (b) return "正在为您申请加入家庭，申请成功,需要等待家庭创始人审核。";
            else return "申请加入家庭失败";
        } catch (Exception e) {
            log.error("申请加入家庭失败", e);
            return "申请加入家庭失败" + e.getMessage();
        }
    }


    /**
     * 退出家庭
     *
     * @return 退出结果
     */
    @Tool(name = "Leave family", description = "Help users leave their current family")
    public String leaveFromFamily() {
        try {
            log.info("用户退出家庭");
            Blind loginBlind = LoginUtils.getLoginBlind();
            boolean b = innerFamilyService.userLeaveFromFamily(loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            if (b) return "退出家庭成功";
            else return "退出家庭失败";
        } catch (Exception e) {
            log.error("退出家庭失败", e);
            return "退出家庭失败" + e.getMessage();
        }
    }


    /**
     * 获取用户的家庭信息
     *
     * @return 家庭信息
     */
    @Tool(name = "Get user's family information", description = "Get detailed information about the user's current family, including family name and member list")
    public String getFamilyInfo() {
        try {
            log.info("获取用户的家庭信息");
            Blind loginBlind = LoginUtils.getLoginBlind();
            FamilyVO familyVO = innerFamilyService.getFamilyVOById(loginBlind.getFamilyId(), LoginUtils.getLoginUserPhone());
            VolunteerVO creatorVolunteer = familyVO.getCreatorVolunteer();
            List<BlindVO> blindVOList = familyVO.getBlindVOList();
            List<VolunteerVO> volunteerVOList = familyVO.getVolunteerVOList();
            StringBuilder sb = new StringBuilder();

            sb.append("获取家庭信息成功！")
                    .append("您加入的家庭信息为")
                    .append("家庭名称：").append(familyVO.getFamilyName()).append("\n")
                    .append("家庭描述：").append(familyVO.getFamilyDescription()).append("\n")
                    .append("家庭创建人：").append(creatorVolunteer.getName()).append("\n")
                    .append("家庭成员列表：").append("\n");
            for (BlindVO blindVO : blindVOList) {
                if (blindVO.getBlindId().equals(loginBlind.getBlindId())) {
                    sb.append("（本人）").append("姓名：").append(blindVO.getName()).append("\n");
                } else {
                    sb.append("姓名：").append(blindVO.getName()).append("\n");
                }
            }
            for (VolunteerVO volunteerVO : volunteerVOList) {
                sb.append("姓名：").append(volunteerVO.getName()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取家庭信息失败", e);
            return "获取家庭信息失败" + e.getMessage();
        }
    }

}