package com.swj.shiwujie.model.request.community.helppost;

import lombok.Data;

import java.io.Serializable;

/**
 * 求助帖更新请求
 *
 * @author swj
 */
@Data
public class HelppostUpdateRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private Long helppostId;

    /**
     * 响应志愿者
     */
    private Long volunteerId;


    private String helpContent;


    private String helpLocation;
    
    
    private String postStatus;

}