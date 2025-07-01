package com.swj.shiwujie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swj.shiwujie.model.domain.Volunteer;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【Volunteer(志愿者信息表)】的数据库操作Mapper
* @createDate 2025-07-01 21:54:40
* @Entity com.swj.shiwujie.model.domain.Volunteer
*/
@Mapper
public interface VolunteerMapper extends BaseMapper<Volunteer> {

}




