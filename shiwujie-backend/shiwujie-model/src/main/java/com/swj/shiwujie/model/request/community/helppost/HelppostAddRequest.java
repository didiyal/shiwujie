package com.swj.shiwujie.model.request.community.helppost;

import lombok.Data;

import java.io.Serializable;

/**
 * 求助帖创建请求
 *
 * @author swj
 */
@Data
public class HelppostAddRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String helpContent;

    private String helpLocation;

}