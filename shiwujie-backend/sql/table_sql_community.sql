create database if not exists `shiwujiecommunity`;
use `shiwujiecommunity`;
create table if not exists Activity
(
    activity_id       bigint auto_increment comment '活动ID'
        primary key,
    community_id      bigint                             not null comment '社区ID',
    manager_id        bigint                             not null comment '社区管理人员ID',
    activity_name     varchar(100)                       not null comment '活动名字',
    activity_content  text                               null comment '活动内容',
    activity_location varchar(300)                       not null comment '活动地点',
    max_participants  bigint                             null comment '活动限定人数',
    activity_status   tinyint  default 0                 null comment '活动状态 0-未开始 1-进行中 2-已结束 3-已取消',
    start_time        datetime                           not null comment '活动开始时间',
    end_time          datetime                           null comment '活动结束时间',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete         tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '活动信息表';

create index communityId_status_startTime
    on Activity (community_id, activity_status, start_time)
    comment '社区id,活动状态,开始时间';

create table if not exists ActivitySign
(
    sign_id           bigint auto_increment comment '活动报名签到ID'
        primary key,
    activity_id       bigint                             null comment '活动id',
    blind_id          bigint                             null comment '视障人士ID',
    volunteer_id      bigint                             null comment '志愿者id',
    sign_up_time      datetime                           not null comment '活动报名时间',
    check_in_time     datetime                           null comment '活动签到时间',
    check_in_location varchar(300)                       null comment '活动签到地点',
    check_out_time    datetime                           null comment '活动签退时间',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete         tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '活动报名签到表';

create index activityId_volunteerId
    on ActivitySign (activity_id, volunteer_id)
    comment '活动id,志愿者id';

create index blindId_activityId
    on ActivitySign (activity_id, blind_id)
    comment '活动id,视障人士id';


create table if not exists Community
(
    community_id          bigint auto_increment comment '社区ID'
        primary key,
    community_type_id     bigint                               null comment '社区类型ID',
    community_level_id    bigint                               null comment '社区级别ID',
    is_default_community  tinyint(1) default 0                 null comment '是否是默认社区',
    parent_community_id   bigint                               null comment '上级社区ID',
    community_name        varchar(100)                         not null comment '社区名字',
    community_description tinytext                             null comment '社区介绍',
    province              char(20)                             not null comment '省',
    city                  char(20)                             null comment '市',
    district              char(20)                             null comment '区',
    address               varchar(200)                         null comment '具体地址',
    registration_info     text                                 null comment '社区注册信息',
    register_volunteer_id bigint                               null comment '社区注册人ID（关联志愿者表）',
    community_status      tinyint    default 0                 null comment '社区状态  0-未审核, 1-已审核, 2-已停用',
    create_time           datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time           datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete             tinyint    default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区信息表';

create index idx_community_address
    on Community (province, city, district)
comment '社区地址索引';

create index idx_community_type_level
    on Community (community_type_id, community_level_id)
comment '社区类型级别索引';

create table if not exists CommunityJoinReview
(
    review_id     bigint auto_increment comment '社区审核ID'
        primary key,
    community_id  bigint                             null comment '社区id',
    blind_id      bigint                             null comment '视障人士ID',
    volunteer_id  bigint                             null comment '志愿者ID',
    apply_time    datetime                           not null comment '请求加入时间',
    review_time   datetime                           null comment '审核时间',
    review_status tinyint  default 0                 null comment '审核状态 0-待审核 1-已通过 2-已拒绝',
    reviewer_id   bigint                             null comment '审核志愿者ID',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete     tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区加入审核表';

create table if not exists CommunityLevel
(
    community_level_id bigint auto_increment comment '社区级别ID'
        primary key,
    level_name         varchar(50)                        not null comment '社区级别名字',
    level_description  text                               null comment '社区级别介绍',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete          tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区级别字典表';

create table if not exists CommunityManager
(
    manager_id         bigint auto_increment comment '社区管理人员ID'
        primary key,
    community_id       bigint                             not null comment '社区ID',
    volunteer_id       bigint                             not null comment '志愿者ID',
    role_permission_id bigint                             not null comment '社区角色权限ID',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete          tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区管理人员表';

create table if not exists CommunityRolePermission
(
    role_permission_id     bigint auto_increment comment '社区角色权限ID'
        primary key,
    role_name              varchar(50)                        not null comment '角色权限名字',
    permission_description text                               null comment '角色权限介绍',
    create_time            datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time            datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete              tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区角色权限表';

create table if not exists CommunityType
(
    community_type_id bigint auto_increment comment '社区类型ID'
        primary key,
    type_name         varchar(50)                        not null comment '社区类型名字',
    type_description  text                               null comment '社区类型介绍',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete         tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '社区类型字典表';


create table if not exists HelpPost
(
    helppost_id   bigint auto_increment comment '求助帖ID'
        primary key,
    community_id  bigint                             null comment '社区ID',
    blind_id      bigint                             not null comment '视障人士ID',
    volunteer_id  bigint                             null comment '响应志愿者ID',
    help_level    int                                null comment '求助级别',
    help_content  text                               not null comment '求助内容',
    help_location varchar(300)                       null comment '求助地点',
    post_status   tinyint  default 0                 null comment '求助帖状态 0-待响应 1-处理中 2-已完成 3-已取消',
    evaluation    text                               null comment '求助评价',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete     tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '求助帖表';

create index blindId_status
    on HelpPost (blind_id, post_status)
comment '视障人士id-状态索引';

create index volunteerId_status
    on HelpPost (volunteer_id, post_status)
comment '志愿者id-状态索引';

