package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.Volunteer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Mapper
* @createDate 2025-07-01 00:21:42
* @Entity com.swj.shiwujie.model.domain.Volunteer
*/
@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

}




