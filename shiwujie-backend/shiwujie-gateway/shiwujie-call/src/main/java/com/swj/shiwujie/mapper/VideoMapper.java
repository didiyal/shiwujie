package com.swj.shiwujie.mapper;

import com.swj.shiwujie.model.domain.Video;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Administrator
* @description 针对表【channels(视频通话频道表)】的数据库操作Mapper
* @createDate 2025-03-23 05:29:41
* @Entity com.swj.shiwujie.model.domain.Channels
*/
@Mapper
public interface VideoMapper extends BaseMapper<Video> {

}




