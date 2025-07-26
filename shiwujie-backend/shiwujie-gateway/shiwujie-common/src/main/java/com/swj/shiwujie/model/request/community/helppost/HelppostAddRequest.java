package com.swj.shiwujie.model.request.community.helppost;

import lombok.Data;


/**
 * 求助帖创建请求
 *
 * @author swj
 */
@Data
public class HelppostAddRequest {

    private Long blindId;

    private String helpContent;

    private String helpLocation;

}