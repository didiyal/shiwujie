package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.model.domain.community.Communitymanager;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.VO.user.volunteer.VolunteerVO;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityEmployeeQueryRequest;
import com.swj.shiwujie.model.request.community.communitymanager.CommunityManagerRequest;

import java.util.List;

/**
* @author Administrator
* @description 针对表【CommunityManager(社区管理人员表)】的数据库操作Service
* @createDate 2025-07-19 01:31:15
*/
public interface CommunitymanagerService extends IService<Communitymanager> {





    /**
     * 查询社区下的员工(志愿者)
     */
    List<VolunteerVO> queryCommunityEmployees(CommunityEmployeeQueryRequest request);

    /**
     * 添加社区管理成员(志愿者)
     */
    boolean addCommunityManager(CommunityManagerRequest request, Long loginVolunteerId);

    /**
     * 修改社区管理成员信息(志愿者)
     */
    boolean updateCommunityManager(CommunityManagerRequest request, Long loginVolunteerId);


    //region 工具方法

    /**
     * 通过志愿者id,社区id查询信息
     */
    Communitymanager getByVolunteerIdAndCommunityId(Long volunteerId, Long communityId);


    /**
     * 删除信息
     * @return 删除数量
     */
    int removeByVolunteerIdAndCommunityId(Long volunteerId,Long communityId);

    /**
     * 根据社区ID删除管理记录
     * @param communityId 社区ID
     * @return 删除数量
     */
    int removeByCommunityId(Long communityId);


    //endregion

}
