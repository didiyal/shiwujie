package com.swj.shiwujie.tools.app;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.model.VO.user.blind.BlindVO;
import com.swj.shiwujie.model.VO.user.family.FamilyVO;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.user.InnerBlindService;
import com.swj.shiwujie.service.user.InnerFamilyService;
import com.swj.shiwujie.utils.LoginUtils;
import com.swj.shiwujie.exception.ThrowUtils;
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

    @Resource
    private InnerBlindService innerBlindService;


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
            Long blindId = LoginUtils.getLoginBlindId();
            ThrowUtils.throwIf(blindId == null, ErrorCode.NO_AUTH, "志愿者身份无法使用AI助手");
            Blind loginBlind = innerBlindService.getById(blindId);
            Long familyId = loginBlind.getFamilyId();
            if(ObjUtil.isNotNull(familyId)){
                return "您已加入家庭了,无需重复加入家庭";
            }
            ThrowUtils.throwIf(StrUtil.isBlankIfStr(familyVolunteerPhone), ErrorCode.PARAMS_ERROR, "请输入家庭创建人手机号");
            boolean b = innerFamilyService.joinFamily(familyVolunteerPhone, loginBlind.getBlindId(), null, LoginUtils.getLoginUserPhone());
            // 2026-09-15：加入家庭已取消家主审核，直连生效，不再播报"等待审核"
            if (b) return "已成功加入家庭。您可以对我说查看家庭信息来确认。";
            else return "加入家庭失败，请稍后再试";
        } catch (com.swj.shiwujie.exception.BusinessException e) {
            // 业务校验拒绝（查无志愿者账号/家属未创建家庭/已在其他家庭等）：
            // 把友好话术原文交给 AI 转述，不再拼接异常堆栈信息
            log.warn("AI加入家庭被拒绝: {}", e.getMessage());
            return e.getMessage();
        } catch (Exception e) {
            log.error("申请加入家庭失败", e);
            return "加入家庭失败，请稍后再试";
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
            Long blindId = LoginUtils.getLoginBlindId();
            ThrowUtils.throwIf(blindId == null, ErrorCode.NO_AUTH, "志愿者身份无法使用AI助手");
            boolean b = innerFamilyService.userLeaveFromFamily(blindId, null, LoginUtils.getLoginUserPhone());
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
            Long blindId = LoginUtils.getLoginBlindId();
            ThrowUtils.throwIf(blindId == null, ErrorCode.NO_AUTH, "志愿者身份无法使用AI助手");
            Blind loginBlind = innerBlindService.getById(blindId);
            ThrowUtils.throwIf(loginBlind.getFamilyId() == null, ErrorCode.PARAMS_ERROR,
                    "您还没有加入家庭。可以对我说帮我加入家庭，并告知家属手机号，我会帮您加入");
            FamilyVO familyVO = innerFamilyService.getFamilyVOById(loginBlind.getFamilyId(), LoginUtils.getLoginUserPhone());

            // 2026-09-15：回执精简为「我的家庭 + 成员（名称用户+手机尾号）+ 身份」，便于语音播报
            // 2026-09-16 修正：家主在 VO 装配时被移出志愿者列表单独存放（creatorVolunteer），必须单独补播
            StringBuilder sb = new StringBuilder();
            int volunteerCount = familyVO.getVolunteerVOList() == null ? 0 : familyVO.getVolunteerVOList().size();
            int blindCount = familyVO.getBlindVOList() == null ? 0 : familyVO.getBlindVOList().size();
            boolean hasCreator = familyVO.getCreatorVolunteer() != null
                    && familyVO.getCreatorVolunteer().getVolunteerId() != null;
            sb.append("已为您查到家庭信息。我的家庭共有")
                    .append(blindCount + volunteerCount + (hasCreator ? 1 : 0))
                    .append("名成员：\n");
            if (familyVO.getBlindVOList() != null) {
                for (BlindVO blindVO : familyVO.getBlindVOList()) {
                    boolean self = blindVO.getBlindId().equals(blindId);
                    sb.append("成员").append(memberDisplayName(blindVO.getName(), blindVO.getPhone()))
                            .append("，身份视障人士").append(self ? "，就是您本人" : "").append("。\n");
                }
            }
            if (hasCreator) {
                VolunteerVO creator = familyVO.getCreatorVolunteer();
                sb.append("成员").append(memberDisplayName(creator.getName(), creator.getPhone()))
                        .append("，身份家主。\n");
            }
            if (familyVO.getVolunteerVOList() != null) {
                for (VolunteerVO volunteerVO : familyVO.getVolunteerVOList()) {
                    sb.append("成员").append(memberDisplayName(volunteerVO.getName(), volunteerVO.getPhone()))
                            .append("，身份家属。\n");
                }
            }
            return sb.toString();
        } catch (com.swj.shiwujie.exception.BusinessException e) {
            log.warn("AI查询家庭信息被拒绝: {}", e.getMessage());
            return e.getMessage();
        } catch (Exception e) {
            log.error("获取家庭信息失败", e);
            return "获取家庭信息失败，请稍后再试";
        }
    }

    /** 成员显示名兜底：姓名为空时默认「用户+手机尾号」（2026-09-15） */
    private String memberDisplayName(String name, String phone) {
        // "无名"（历史默认名）与空值一并兜底为「用户+手机尾号」（2026-09-16）
        if (name != null && !name.trim().isEmpty() && !"无名".equals(name.trim())) {
            return name;
        }
        if (phone != null && phone.length() >= 4) {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return "用户";
    }

}