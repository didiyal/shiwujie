package com.swj.shiwujie.model.request.community.helppost;

import com.swj.shiwujie.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 求助帖更新请求
 *
 * @author swj
 */
@Data
public class HelppostUpdateRequest{

    private Long helppostId;

    /**
     * 响应志愿者
     */
    private Long volunteerId;


    private String helpContent;


    private String helpLocation;
    
    
    private String postStatus;

}