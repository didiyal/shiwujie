package com.swj.shiwujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.swj.shiwujie.model.domain.ai.AiLogs;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【AiLogs(AI操作日志表)】的数据库操作Mapper
* @createDate 2025-08-08 14:33:35
* @Entity com.swj.shiwujie.model.domain.shiwujie.AiLogs
*/
@Mapper
public interface AiLogsMapper extends BaseMapper<AiLogs> {

}




