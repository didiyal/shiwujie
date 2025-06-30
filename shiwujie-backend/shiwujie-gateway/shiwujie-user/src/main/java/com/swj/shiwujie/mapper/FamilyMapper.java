package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.Family;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【Family(家庭信息表)】的数据库操作Mapper
* @createDate 2025-07-01 00:21:42
* @Entity com.swj.shiwujie.model.domain.Family
*/
@Mapper
public interface FamilyMapper extends BaseMapper<Family> {

}




