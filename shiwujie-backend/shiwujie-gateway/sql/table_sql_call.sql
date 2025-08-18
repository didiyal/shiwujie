create database if not exists `shiwujiecall`;
use shiwujiecall;
create table if not exists UrgentHelp
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

create index blindId_status
    on UrgentHelp (blind_id, help_status)
    comment '视障人士id与状态';

create table if not exists VideoHelp
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

create index blindId_status
    on VideoHelp (blind_id, help_status)
    comment '视障人士id与状态';

create index volunteerId_status
    on VideoHelp (volunteer_id, help_status)
    comment '志愿者id与状态';


