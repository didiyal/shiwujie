package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.community.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【Activity(活动信息表)】的数据库操作Mapper
* @createDate 2025-07-26 22:31:08
* @Entity com.swj.shiwujie.model.domain.community.Activity
*/
@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

}




