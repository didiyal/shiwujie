package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.community.CommunityLoginSuccessVO;
import com.swj.shiwujie.model.VO.community.community.CommunityVO;
import com.swj.shiwujie.model.domain.community.Community;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.user.Volunteer;
import com.swj.shiwujie.model.request.community.community.CommunityRegisterRequest;
import com.swj.shiwujie.model.request.community.community.CommunityUpdateRequest;
import com.swj.shiwujie.model.request.user.volunteer.VolunteerLARRequest;

import java.util.List;

/**
* @author Administrator
* @description 针对表【Community(社区信息表)】的数据库操作Service
* @createDate 2025-07-18 14:44:37
*/
public interface CommunityService extends IService<Community> {

    //region 社区注册登录

    /**
     * 测试登录
     * @param loginVolunteerId
     */
    void checkLogin(Long loginVolunteerId);

    /**
     * 社区入驻
     *
     * @param communityRegisterRequest 社区注册信息
     * @return 脱敏后的社区信息
     */
    CommunityLoginSuccessVO communityRegister(CommunityRegisterRequest communityRegisterRequest);


    /**
     * 社区登录
     * @param volunteerLARRequest 登录人手机号与密码
     * @return 脱敏后的登录数据
     */
    CommunityLoginSuccessVO communityLogin(VolunteerLARRequest volunteerLARRequest);

    //endregion

    /**
     * 修改社区信息
     * @param request 修改请求
     * @param volunteerId 操作人ID
     * @return 更新后的社区信息
     */
    CommunityVO updateCommunity(CommunityUpdateRequest request, Long volunteerId);

    /**
     * 删除社区
     * @param communityId 社区ID
     * @param volunteerId 操作人ID
     * @return 是否删除成功
     */
    boolean deleteCommunity(Long communityId, Long volunteerId);

    /**
     * 分页查询社区下的子社区
     * @param communityId 父社区ID
     * @param current 页码
     * @param size 每页条数
     * @param volunteerId 操作人ID
     * @return 子社区VO分页对象
     */
    Page<CommunityVO> getSubCommunities(Long communityId, long current, long size, Long volunteerId);


    // region 工具方法


    /**
     * 通过社区名查询社区
     * @param communityName 社区名
     * @return 社区
     */
    Community getByName(String communityName);


    /**
     * 构建社区登录注册返回结果
     */
    CommunityLoginSuccessVO buildCommunityLoginResult(Community community, Volunteer volunteer);


    /**
     * 封装脱敏
     * @param community
     * @return
     */
    CommunityVO getCommunityVO(Community community);




    // endregion
}
