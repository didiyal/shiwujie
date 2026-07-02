create database if not exists shiwujieuser;
use shiwujieuser;

create table if not exists Blind
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

create index phone
    on Blind (phone)
    comment '用户手机号唯一索引';

create index familyId
    on Blind (family_id)
    comment '家庭id';


create table if not exists Family
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

create index creatorVolunteerId
    on Family (creator_volunteer_id)
    comment '家庭创建人ID';

create table if not exists FamilyJoinReview
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


create table if not exists Volunteer
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

create index phone
    on Volunteer (phone)
    comment '手机号唯一索引';

create index familyId
    on Volunteer (family_id)
    comment '家庭id';

