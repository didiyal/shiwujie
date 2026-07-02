package com.swj.shiwujie.service.community;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.model.domain.community.Communityjoinreview;
import com.swj.shiwujie.model.domain.user.Volunteer;

/**
* @author Administrator
* @description 针对表【CommunityJoinReview(社区加入审核表)】的数据库操作Service实现
* @createDate 2025-07-25 18:15:48
*/
public interface InnerCommunityjoinreviewService{

    /**
     * 插入数据
     *
     * @param communityjoinreview 志愿者加入信息
     * @return 信息
     */
    boolean save(Communityjoinreview communityjoinreview);



    Communityjoinreview getById(Long id);




    Communityjoinreview getOne(QueryWrapper<Communityjoinreview> queryWrapper);
}




