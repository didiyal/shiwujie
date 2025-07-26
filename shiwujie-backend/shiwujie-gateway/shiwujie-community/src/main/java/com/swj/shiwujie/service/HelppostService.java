package com.swj.shiwujie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.swj.shiwujie.model.domain.community.Helppost;
import com.swj.shiwujie.model.VO.community.helppost.HelppostVO;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;

import java.util.List;

/**
 * 求助帖服务接口
 *
 * @author swj
 */
public interface HelppostService extends IService<Helppost> {

    /**
     * 视障人士发出求助帖
     * @param helppostAddRequest 求助帖创建请求
     * @return 求助帖VO
     */
    HelppostVO addHelppost(HelppostAddRequest helppostAddRequest);

    /**
     * 通过id查询求助帖VO
     * @param helppostId 求助帖id
     * @return 求助帖VO
     */
    HelppostVO getHelppostVOById(Long helppostId);

    /**
     * 分页选择查询社区下的求助帖
     * @param helppostQueryRequest 求助帖查询请求
     * @return 求助帖VO列表
     */
    Page<HelppostVO> listHelppostsByCommunity(HelppostQueryRequest helppostQueryRequest);

    /**
     * 删除求助帖
     * @param helppostId 求助帖id
     * @param loginBlindId 登录用户id
     * @return 是否删除成功
     */
    boolean deleteHelppost(Long helppostId, Long loginBlindId,Long loginVolunteerId);

    /**
     * 修改求助帖信息
     * @param helppostUpdateRequest 求助帖更新请求
     * @param loginBlindId 登录用户id
     * @return 是否修改成功
     */
    boolean updateHelppost(HelppostUpdateRequest helppostUpdateRequest, Long loginBlindId,Long loginVolunteerId);

    /**
     * 封装求助帖VO
     * @param helppost 求助帖实体
     * @return 求助帖VO
     */
    HelppostVO getHelppostVO(Helppost helppost);

}
