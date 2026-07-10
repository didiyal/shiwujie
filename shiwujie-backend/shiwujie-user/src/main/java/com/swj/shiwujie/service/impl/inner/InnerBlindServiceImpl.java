package com.swj.shiwujie.service.impl.inner;

import cn.hutool.core.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.common.ErrorCode;
import com.swj.shiwujie.exception.ThrowUtils;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.request.community.CommunityJoinRequest;
import com.swj.shiwujie.service.BlindService;
import com.swj.shiwujie.service.user.InnerBlindService;
import org.apache.dubbo.config.annotation.DubboService;

import jakarta.annotation.Resource;


/**
 * @author Administrator
 * @description 针对表【Blind(视障人士信息表)】的数据库操作Service实现
 * @createDate 2025-07-01 00:21:42
 */
@DubboService
public class InnerBlindServiceImpl implements InnerBlindService {



    @Resource
    private BlindService blindService;


    /**
     * 通过手机号查询信息
     *
     * @param id id
     * @return 信息
     */
    @Override
    public Blind getById(Long id) {
        return blindService.getById(id);
    }


    /**
     * 通过手机号查询用户(视障人士)信息
     *
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    @Override
    public Blind getByPhone(String phone) {
        return blindService.getByPhone(phone);
    }

    @Override
    public boolean updateById(Blind blind) {
        return blindService.updateById(blind);
    }


    /**
     * 删除社区后关联的所有用户信息
     */
    @Override
    public boolean removeCommunityId(Long communityId) {
        return blindService.removeCommunityId(communityId);
    }

//    /**
//     * 加入社区
//     * @param blindId 视障人士ID
//     * @param request 加入社区请求
//     * @return 是否成功
//     */
//    public boolean joinCommunity(Long blindId, CommunityJoinRequest request){
//        return blindService.joinCommunity(blindId, request);
//    }

}




