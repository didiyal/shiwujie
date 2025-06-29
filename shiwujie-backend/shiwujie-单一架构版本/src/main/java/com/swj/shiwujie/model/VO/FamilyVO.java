package com.swj.shiwujie.model.VO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.swj.shiwujie.model.domain.Family;
import com.swj.shiwujie.model.domain.User;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 家庭视图
 * @author ldl
 */

@Data
public class FamilyVO  {
    /**
     * 主键 id
     */
    private Long id;

    /**
     * 家庭名字
     */
    private String familyName;

    /**
     * 家庭账号
     */
    private String familyAccount;

    /**
     * 创建人Id
     */
    private Long userId;

    /**
     * 用户列表
     */
    private List<User> userList;

    /**
     * 待加入用户Id
     */
    private String postId;

    /**
     *
     */
    private Date createTime;

    /**
     *
     */
    private Date updateTime;

    /**
     * 逻辑删除   0 - 存在  1 - 删除
     */
    private Integer isDelete;


}
