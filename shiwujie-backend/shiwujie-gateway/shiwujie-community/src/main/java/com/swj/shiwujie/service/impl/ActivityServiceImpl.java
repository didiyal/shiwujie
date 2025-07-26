package com.swj.shiwujie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.swj.shiwujie.model.domain.community.Activity;
import com.swj.shiwujie.service.ActivityService;
import com.swj.shiwujie.mapper.ActivityMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【Activity(活动信息表)】的数据库操作Service实现
* @createDate 2025-07-26 22:31:08
*/
@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity>
    implements ActivityService{

}




