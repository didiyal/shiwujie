create database if not exists shiwujieGatewaty;

use shiwujieGatewaty;



create table callHelp
(
    id            bigint auto_increment comment '主键id'
        primary key,
    familyId      bigint                             not null comment '家庭id',
    blindId       bigint                             not null comment '求助盲人id',
    blindUid      varchar(256)                       not null comment '求助盲人视频通话uid',
    channel       varchar(256)                       not null comment '通话频道',
    helpOthersId  varchar(1024)                      null comment '帮助家属id(后续可以多人)',
    helpOthersUid varchar(1024)                      null comment '帮助家属视频通话uid(后续可以多人)',
    status        tinyint  default 0                 null comment '状态 0 - 求助人等待帮助中   1 - 求助人与帮助家属正在通话	2 - 求助人主动取消求助	3 - 求助正常结束',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    beginTime     datetime                           null comment '视频通话开始时间',
    endTime       datetime                           null comment '视频通话截止时间',
    callTime      bigint                             null comment '视频通话时长(小时:分钟)',
    isDelete      tinyint  default 0                 null comment '逻辑删除,0 - 存在  1 - 删除'
)
    comment '视频通话频道表';

create table family
(
    id            bigint auto_increment comment '主键 id'
        primary key,
    familyName    varchar(256)                       null comment '家庭名字',
    familyAccount varchar(512)                       null comment '家庭账号',
    userId        bigint                             null comment '创建人Id',
    addId         varchar(512)                       null comment '加入人的Id  json   ',
    postId        varchar(1024)                      null comment '待加入用户Id',
    createTime    datetime default CURRENT_TIMESTAMP null,
    updateTime    datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    isDelete      tinyint  default 0                 null comment '逻辑删除   0 - 存在  1 - 删除',
    constraint family_familyAccount_uindex
        unique (familyAccount)
)
    comment '家庭表';

create table user
(
    id              bigint auto_increment comment '主键 自增'
        primary key,
    userName        varchar(256)                       null comment '用户昵称',
    userAccount     varchar(256)                       not null comment '用户账号',
    userPassword    varchar(512)                       not null comment '用户密码',
    userUrl         varchar(1024)                      null comment '用户头像',
    userPhone       varchar(256)                       null comment '用户手机号',
    userEmail       varchar(1024)                      null comment '用户邮箱',
    gender          tinyint  default 0                 not null comment '用户性别 0 - 男 1 - 女',
    status          tinyint  default 0                 not null comment '用户状态 0 - 盲人 1 - 志愿者 2 - 待选择',
    isOnline        tinyint  default 0                 not null comment '用户是否在线',
    familyId        bigint                             null comment '用户加入的家庭Id',
    userRole        tinyint  default 0                 not null comment '用户权限 0 - 默认 1 - 管理员',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    isDelete        tinyint  default 0                 not null comment '是否删除 0 - 存在 1 - 删除',
    callStatus      tinyint  default 0                 not null comment '信令状态 0 - 空闲，1 - 等待接通，2 - 正在通话',
    callChannel     varchar(255)                       null comment '当前通话的 Call ID',
    userCertificate varchar(255)                       null comment '残疾人证件',
    constraint userAccount
        unique (userAccount),
    constraint userCertificate
        unique (userCertificate),
    constraint user_pk
        unique (userAccount)
)
    comment '用户表';

create table video
(
    id           bigint auto_increment comment '主键id'
        primary key,
    channel      varchar(256)                       not null comment '频道号',
    blindUid     varchar(256)                       null comment '盲人uid',
    volunteerUid varchar(256)                       not null comment '志愿者uid',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    beginTime    datetime                           null comment '视频通话开始时间',
    endTime      datetime                           null comment '视频通话截止时间',
    callTime     time                               null comment '视频通话时长(小时:分钟)',
    status       tinyint  default 0                 null comment '频道状态  0 - 志愿者等待中  1 - 正在通话   2 - 通话结束   3  -  志愿者取消通话',
    isDelete     tinyint  default 0                 null comment '逻辑删除,0 - 存在  1 - 删除'
)
    comment '视频通话频道表';

