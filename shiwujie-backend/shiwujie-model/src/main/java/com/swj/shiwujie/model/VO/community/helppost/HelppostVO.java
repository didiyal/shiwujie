package com.swj.shiwujie.model.VO.community.helppost;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.swj.shiwujie.model.enums.community.PostStatusEnum;
import lombok.Data;

import java.util.Date;

/**
 * 求助帖VO
 *
 * @author swj
 */
@Data
public class HelppostVO  implements java.io.Serializable{

    private static final long serialVersionUID = 1L;



    private Long helppostId;

    /**
     * 社区ID
     */
    private Long communityId;

    /**
     * 视障人士ID
     */
    private Long blindId;

    /**
     * 响应志愿者ID
     */
    private Long volunteerId;


    private String helpContent;

    
    private String helpLocation;

    /**
     * 求助帖状态 0-待响应 1-处理中 2-已完成 3-已取消
     */
    private String postStatus;



    /**
     * 将状态码转换为状态描述
     * @param postStatus 状态码
     */
    public void setPostStatus(Integer postStatus) {
        this.postStatus = PostStatusEnum.getById(postStatus).getName();
    }

}