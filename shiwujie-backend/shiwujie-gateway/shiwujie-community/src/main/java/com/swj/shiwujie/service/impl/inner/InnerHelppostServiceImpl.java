package com.swj.shiwujie.service.impl.inner;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.swj.shiwujie.model.VO.community.helppost.HelppostVO;
import com.swj.shiwujie.model.request.community.helppost.HelppostAddRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostQueryRequest;
import com.swj.shiwujie.model.request.community.helppost.HelppostUpdateRequest;
import com.swj.shiwujie.service.HelppostService;
import com.swj.shiwujie.service.community.InnerHelppostService;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * 内部求求助帖服务实现类
 *
 * @author swj
 */
@DubboService
public class InnerHelppostServiceImpl implements InnerHelppostService {


    @Resource
    private HelppostService helppostService;

    /**
     * 视障人士发出求助帖
     */
    @Override
    public HelppostVO addHelppost(HelppostAddRequest helppostAddRequest,Long loginBlindId) {
        return helppostService.addHelppost(helppostAddRequest,loginBlindId);
    }


    /**
     * 分页选择查询社区下的求助帖
     */
    @Override
    public Page<HelppostVO> listQueryHelpposts(HelppostQueryRequest helppostQueryRequest) {
        return helppostService.listHelppostsByCommunity(helppostQueryRequest);
    }

    /**
     * 删除求助帖
     */
    @Override
    public boolean deleteHelppost(Long helppostId, Long loginBlindId,Long loginVolunteerId) {
        return helppostService.deleteHelppost(helppostId, loginBlindId,loginVolunteerId);
    }

    /**
     * 修改求助帖信息
     */
    @Override
    public boolean updateHelppost(HelppostUpdateRequest helppostUpdateRequest, Long loginBlindId,Long loginVolunteerId) {
        return helppostService.updateHelppost(helppostUpdateRequest, loginBlindId,loginVolunteerId);
    }



}




