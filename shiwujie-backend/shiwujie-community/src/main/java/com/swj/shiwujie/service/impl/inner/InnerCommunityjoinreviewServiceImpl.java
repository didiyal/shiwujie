package com.swj.shiwujie.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.swj.shiwujie.model.domain.community.Communityjoinreview;
import com.swj.shiwujie.service.CommunityjoinreviewService;
import com.swj.shiwujie.service.community.InnerCommunityjoinreviewService;
import org.apache.dubbo.config.annotation.DubboService;

import javax.annotation.Resource;

/**
* 社区加入接口调用
*/
@DubboService
public class InnerCommunityjoinreviewServiceImpl implements InnerCommunityjoinreviewService {


    @Resource
    private CommunityjoinreviewService communityjoinreviewService;




    /**
     * 插入数据
     * @param communityjoinreview 志愿者加入信息
     * @return 信息
     */
    @Override
    public boolean save(Communityjoinreview communityjoinreview) {
        return communityjoinreviewService.save(communityjoinreview);
    }



    @Override
    public Communityjoinreview getById(Long id) {
        return communityjoinreviewService.getById(id);
    }

    @Override
    public Communityjoinreview getOne(QueryWrapper<Communityjoinreview> queryWrapper) {
        return communityjoinreviewService.getOne(queryWrapper);
    }


}




