package com.swj.shiwujie.model.enums.community;


import lombok.Getter;


/**
 * 社区管理人类别枚举
 */
@Getter
public enum CommunityRolePermissionEnum {


    REGISTRANT("注册人","社区注册人,可以分配管理员",1),
    ADMIN("管理员","社区管理员,可以审核社区加入,管理活动",2),
    EMPLOYEE("员工","社区员工,可以发起活动签到",3);

    private String name;

    private String description;

    private long roleId;

    CommunityRolePermissionEnum(String name, String description, long roleId) {
        this.name = name;
        this.description = description;
        this.roleId = roleId;
    }
}
