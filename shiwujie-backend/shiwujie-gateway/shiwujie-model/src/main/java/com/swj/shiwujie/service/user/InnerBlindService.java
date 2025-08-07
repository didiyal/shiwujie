package com.swj.shiwujie.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.user.Blind;
import com.swj.shiwujie.model.domain.user.Volunteer;

/**
 * dubbo 服务提供接口
 */
public interface InnerBlindService  {

    /**
     * 通过id查询信息
     * @param id
     * @return
     */
    Blind getById(Long id);
    


    /**
     * 通过手机号查询用户(视障人士)信息
     * @param phone 视障人士手机号
     * @return 视障人士信息
     */
    Blind getByPhone(String phone);

    /**
     * 更新信息
     * @param blind 要更新的
     * @return 是否成功
     */
    boolean updateById(Blind blind);


    /**
     * 删除社区后关联的所有用户信息
     */
    boolean removeCommunityId(Long communityId);
}
