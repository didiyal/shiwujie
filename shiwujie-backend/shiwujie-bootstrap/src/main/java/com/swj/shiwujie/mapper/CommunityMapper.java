package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.community.Community;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【Community(社区信息表)】的数据库操作Mapper
* @createDate 2025-07-19 00:40:04
* @Entity com.swj.shiwujie.model.domain.community.Community
*/
@Mapper
public interface CommunityMapper extends BaseMapper<Community> {

}




