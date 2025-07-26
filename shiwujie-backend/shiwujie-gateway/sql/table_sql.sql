create table AILogs
(
    log_id         bigint auto_increment comment 'AI操作日志ID'
        primary key,
    operator_id    bigint                             null comment '操作人ID',
    content        text                               null comment '发送内容',
    operation_time datetime                           not null comment '发送时间',
    log_type       varchar(50)                        null comment '类型',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete      tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment 'AI操作日志表';

create index idx_ailogs_operator_time
    on AILogs (operator_id, operation_time);

create table Activity
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

create index idx_activity_community_status_time
    on Activity (community_id, activity_status, start_time);

create index idx_activity_status_participants
    on Activity (activity_status, max_participants);

create table ActivitySign
(
    sign_id           bigint auto_increment comment '活动报名签到ID'
        primary key,
    activity_id       bigint                             null comment '活动id',
    blind_id          bigint                             not null comment '视障人士ID',
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

create table Blind
(
    blind_id             bigint auto_increment comment '视障人士ID'
        primary key,
    community_id         bigint                                null comment '社区ID',
    is_actively_joined   tinyint(1)  default 0                 null comment '是否主动加入社区',
    family_id            bigint                                null comment '家庭ID',
    name                 varchar(50) default '无名'            not null comment '名字',
    phone                char(11)                              not null comment '手机号',
    password             varchar(100)                          null comment '密码',
    gender               tinyint                               null comment '性别 0-男 1-女',
    wechat_id            varchar(50)                           null comment '微信账号',
    qq_id                varchar(20)                           null comment 'QQ账号',
    id_card              char(100)                             null comment '身份证号',
    disability_card      char(100)                             null comment '残疾人证件号',
    other_info           text                                  null comment '其它信息',
    help_request_count   bigint      default 0                 null comment '求助次数',
    latitude             decimal(10, 6)                        null comment '纬度坐标',
    longitude            decimal(10, 6)                        null comment '经度坐标',
    location_address     varchar(200)                          null comment '位置地址（省市区+详细地址）',
    location_update_time datetime                              null on update CURRENT_TIMESTAMP comment '位置更新时间',
    create_time          datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time          datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete            tinyint     default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '视障人士信息表';

create table Community
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
    on Community (province, city, district);

create index idx_community_default_status
    on Community (is_default_community, community_status);

create index idx_community_type_level
    on Community (community_type_id, community_level_id);

create table CommunityJoinReview
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

create index idx_review_user_status
    on CommunityJoinReview (blind_id, volunteer_id, review_status);

create table CommunityLevel
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

create table CommunityManager
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

create table CommunityRolePermission
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

create table CommunityType
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

create table Family
(
    family_id            bigint auto_increment comment '家庭ID'
        primary key,
    family_name          varchar(100)                       null comment '家庭名字',
    family_description   text                               null comment '家庭详细介绍',
    creator_volunteer_id bigint                             not null comment '家庭创建人ID（关联志愿者表）',
    create_time          datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time          datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete            tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '家庭信息表';

create table FamilyJoinReview
(
    review_id     bigint auto_increment comment '家庭审核ID'
        primary key,
    family_id     bigint                             null comment '家庭id',
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
    comment '家庭加入审核表';

create table HelpPost
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

create index idx_helppost_blind_status_level
    on HelpPost (blind_id, post_status, help_level);

create index idx_helppost_volunteer
    on HelpPost (volunteer_id);

create table Navigation
(
    navigation_id   bigint auto_increment comment '导航ID'
        primary key,
    user_id         bigint                             not null comment '用户ID（视障人士/志愿者）',
    user_type       tinyint                            not null comment '用户类型 0-blind 1-volunteer',
    help_id         bigint                             null comment '求助ID（可选，关联VideoHelp/UrgentHelp）',
    navigation_type tinyint                            not null comment '导航类型： 0-视频求助 1-家属紧急求助 2-独立使用 3-其他',
    start_latitude  decimal(10, 6)                     null comment '起点纬度',
    start_longitude decimal(10, 6)                     null comment '起点经度',
    start_address   varchar(200)                       null comment '起点地址',
    end_latitude    decimal(10, 6)                     null comment '终点纬度',
    end_longitude   decimal(10, 6)                     null comment '终点经度',
    end_address     varchar(200)                       null comment '终点地址',
    route_data      text                               null comment '导航路线数据（JSON格式）',
    distance        decimal(10, 2)                     null comment '距离（米）',
    duration        int                                null comment '预计耗时（秒）',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete       tinyint  default 0                 null comment '逻辑删除 0-存在 1-删除'
)
    comment '通用导航记录表';

create index idx_nav_help_id
    on Navigation (help_id);

create index idx_nav_type
    on Navigation (navigation_type);

create index idx_nav_user_type
    on Navigation (user_type, user_id);

create table UrgentHelp
(
    help_id                    bigint auto_increment comment '紧急求助ID'
        primary key,
    channel_id                 bigint                             null comment '频道id',
    family_id                  bigint                             null comment '家庭ID',
    blind_id                   bigint                             null comment '视障人士ID',
    blind_latitude             decimal(10, 6)                     null comment '视障人士纬度坐标',
    blind_longitude            decimal(10, 6)                     null comment '视障人士经度坐标',
    blind_location_address     varchar(200)                       null comment '视障人士位置地址（省市区+详细地址）',
    volunteer_id               bigint                             null comment '响应家属(志愿者)ID',
    volunteer_latitude         decimal(10, 6)                     null comment '响应家属纬度坐标',
    volunteer_longitude        decimal(10, 6)                     null comment '响应家属经度坐标',
    volunteer_location_address varchar(200)                       null comment '响应家属位置地址（省市区+详细地址）',
    help_status                tinyint  default 0                 null comment '求助状态 0-待响应 1-处理中 2-已完成 3-已取消',
    start_time                 datetime                           null comment '求助开始时间',
    response_time              datetime                           null comment '求助响应时间',
    end_time                   datetime                           null comment '求助结束时间',
    duration                   bigint                             null comment '求助耗时',
    video_path                 varchar(200)                       null comment '视频储存地址',
    create_time                datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time                datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete                  tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '紧急求助表';

create table UserLogs
(
    log_id           bigint auto_increment comment '用户操作日志ID'
        primary key,
    blind_id         bigint                             null comment '视障人士ID',
    volunteer_id     bigint                             null comment '志愿者ID',
    request_type     varchar(50)                        null comment '请求类型',
    request_content  text                               null comment '请求内容',
    request_time     datetime                           not null comment '请求时间',
    request_address  varchar(100)                       null comment '请求地址',
    response_content text                               null comment '响应内容',
    response_time    datetime                           null comment '响应时间',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete        tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '用户操作日志表';

create index idx_userlogs_time_type
    on UserLogs (request_time, request_type);

create table VideoHelp
(
    help_id                    bigint auto_increment comment '视频求助ID'
        primary key,
    channel_id                 bigint                             null comment '频道id',
    blind_id                   bigint                             null comment '视障人士ID',
    blind_latitude             decimal(10, 6)                     null comment '视障人士纬度坐标',
    blind_longitude            decimal(10, 6)                     null comment '视障人士经度坐标',
    blind_location_address     varchar(200)                       null comment '视障人士位置地址（省市区+详细地址）',
    volunteer_id               bigint                             null comment '响应志愿者ID',
    volunteer_latitude         decimal(10, 6)                     null comment '响应志愿者纬度坐标',
    volunteer_longitude        decimal(10, 6)                     null comment '响应志愿者经度坐标',
    volunteer_location_address varchar(200)                       null comment '响应志愿者位置地址（省市区+详细地址）',
    help_status                tinyint  default 0                 null comment '求助状态 0-待响应 1-处理中 2-已完成 3-已取消',
    start_time                 datetime                           null comment '求助开始时间',
    response_time              datetime                           null comment '求助响应时间',
    end_time                   datetime                           null comment '求助结束时间',
    duration                   bigint                             null comment '求助耗时',
    video_path                 varchar(200)                       null comment '视频储存地址',
    evaluation                 text                               null comment '求助评价',
    create_time                datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time                datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete                  tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '视频求助表';

create table Volunteer
(
    volunteer_id         bigint auto_increment comment '志愿者ID'
        primary key,
    community_id         bigint                                  null comment '社区ID',
    is_actively_joined   tinyint(1)    default 0                 null comment '是否主动加入社区',
    family_id            bigint                                  null comment '家庭ID',
    name                 varchar(50)   default '无名'            null comment '名字',
    phone                char(11)                                not null comment '手机号',
    password             varchar(100)                            null comment '密码',
    gender               tinyint                                 null comment '性别 0-男 1-女',
    wechat_id            varchar(50)                             null comment '微信账号',
    qq_id                varchar(20)                             null comment 'QQ账号',
    id_card              char(100)                               null comment '身份证号',
    other_info           text                                    null comment '其它信息',
    online_status        tinyint       default 0                 null comment '在线状态（用于区分是否匹配） 0-离线 1-在线 2-忙碌',
    help_count           bigint        default 0                 null comment '帮助次数',
    rating               decimal(3, 1) default 0.0               null comment '志愿者评分',
    latitude             decimal(10, 6)                          null comment '纬度坐标',
    longitude            decimal(10, 6)                          null comment '经度坐标',
    location_address     varchar(200)                            null comment '位置地址（省市区+详细地址）',
    location_update_time datetime                                null on update CURRENT_TIMESTAMP comment '位置更新时间',
    create_time          datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time          datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete            tinyint       default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment '志愿者信息表';

create index idx_volunteer_community_status
    on Volunteer (community_id, online_status);

create index idx_volunteer_family_rating
    on Volunteer (family_id, rating);

