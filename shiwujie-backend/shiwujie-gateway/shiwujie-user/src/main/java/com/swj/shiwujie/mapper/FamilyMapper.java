package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.Family;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author ldl
* @description 针对表【family(家庭表)】的数据库操作Mapper
* @createDate 2024-12-15 23:26:43
* @Entity com.swj.shiwujie.model.domain.Family
*/

@Mapper
public interface FamilyMapper extends BaseMapper<Family> {

}




