package com.swj.shiwujie.tools.app;


import cn.hutool.core.util.ObjUtil;
import com.swj.shiwujie.model.domain.community.Community;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.service.community.InnerActivityService;
import com.swj.shiwujie.service.community.InnerCommunityService;
import com.swj.shiwujie.utils.LoginUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 社区模块业务工具调用
 */
public class CommunityTools {


    @DubboReference
    private InnerCommunityService innerCommunityService;



    /**
     * 获取用户的社区信息
     * @return 社区信息
     */
    @Tool(description = "APP功能操作:获取用户的社区信息")
    public String getCommunityInfo()
    {
        Blind loginBlind = LoginUtils.getLoginBlind();
        Long communityId = loginBlind.getCommunityId();
        if(ObjUtil.isNull(communityId)){
            return "您未加入任何社区！";
        }
        Community community = innerCommunityService.getById(communityId);
        StringBuilder sb = new StringBuilder();
        sb.append("获取社区信息成功！")
                .append("您加入的社区信息为")
                .append("社区名称：").append(community.getCommunityName()).append("\n")
                .append("社区描述：").append(community.getCommunityDescription()).append("\n")
                .append("社区地址：").append(community.getAddress()).append("\n")
                .append("社区类型：").append(community.getCommunityTypeId()).append("\n");

        return  sb.toString();
    }






}
