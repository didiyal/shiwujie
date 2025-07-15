DROP DATABASE IF EXISTS shiwujie;
CREATE DATABASE IF NOT EXISTS shiwujie;

use shiwujie;

DROP TABLE IF EXISTS Community;
CREATE TABLE Community
(
    community_id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区ID',
    community_type_id     BIGINT COMMENT '社区类型ID',
    community_level_id    BIGINT COMMENT '社区级别ID',
    is_default_community  BOOLEAN  DEFAULT FALSE COMMENT '是否是默认社区',
    parent_community_id   BIGINT COMMENT '上级社区ID',
    community_name        VARCHAR(100)                       NOT NULL COMMENT '社区名字',
    province              CHAR(20)                           NOT NULL COMMENT '省',
    city                  CHAR(20)                           NOT NULL COMMENT '市',
    district              CHAR(20)                           NOT NULL COMMENT '区',
    address               VARCHAR(200) COMMENT '具体地址',
    registration_info     TEXT COMMENT '社区注册信息',
    register_volunteer_id BIGINT                             NOT NULL COMMENT '社区注册人ID（关联志愿者表）',
    community_status      tinyint  DEFAULT 0 COMMENT '社区状态  0-未审核, 1-已审核, 2-已停用',
    create_time           datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time           datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete             tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区信息表';



DROP TABLE IF EXISTS CommunityType;
CREATE TABLE CommunityType
(
    community_type_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区类型ID',
    type_name         VARCHAR(50)                        NOT NULL COMMENT '社区类型名字',
    type_description  TEXT COMMENT '社区类型介绍',
    create_time       datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete         tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区类型字典表';


DROP TABLE IF EXISTS CommunityLevel;
CREATE TABLE CommunityLevel
(
    community_level_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区级别ID',
    level_name         VARCHAR(50)                        NOT NULL COMMENT '社区级别名字',
    level_description  TEXT COMMENT '社区级别介绍',
    create_time        datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete          tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区级别字典表';



DROP TABLE IF EXISTS CommunityManager;
CREATE TABLE CommunityManager
(
    manager_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区管理人员ID',
    community_id       BIGINT                             NOT NULL COMMENT '社区ID',
    volunteer_id       BIGINT                             NOT NULL COMMENT '志愿者ID',
    role_permission_id BIGINT                             NOT NULL COMMENT '社区角色权限ID',
    create_time        datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete          tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区管理人员表';



DROP TABLE IF EXISTS CommunityRolePermission;
CREATE TABLE CommunityRolePermission
(
    role_permission_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区角色权限ID',
    role_name              VARCHAR(50)                        NOT NULL COMMENT '角色权限名字',
    permission_description TEXT COMMENT '角色权限介绍',
    create_time            datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time            datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete              tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区角色权限表';



DROP TABLE IF EXISTS Family;
CREATE TABLE Family
(
    family_id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '家庭ID',
    family_name          VARCHAR(100)                       NOT NULL COMMENT '家庭名字',
    family_description   TEXT COMMENT '家庭详细介绍',
    creator_volunteer_id BIGINT                             NOT NULL COMMENT '家庭创建人ID（关联志愿者表）',
    create_time          datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time          datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete            tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '家庭信息表';



DROP TABLE IF EXISTS Blind;
CREATE TABLE Blind
(
    blind_id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视障人士ID',
    community_id         BIGINT COMMENT '社区ID',
    is_actively_joined   BOOLEAN  DEFAULT FALSE COMMENT '是否主动加入社区',
    family_id            BIGINT COMMENT '家庭ID',
    name                 VARCHAR(50)                        COMMENT '名字',
    phone                CHAR(11)                           NOT NULL COMMENT '手机号',
    password             VARCHAR(100)                       COMMENT '密码',
	gender				 tinyint  							NOT NULL COMMENT '性别 0-男 1-女',
    wechat_id            VARCHAR(50) COMMENT '微信账号',
    qq_id                VARCHAR(20) COMMENT 'QQ账号',
    id_card              CHAR(100) COMMENT '身份证号',
    disability_card      CHAR(100) COMMENT '残疾人证件号',
    other_info           TEXT COMMENT '其它信息',
    help_request_count   BIGINT   DEFAULT 0 COMMENT '求助次数',
    latitude             DECIMAL(10, 6) COMMENT '纬度坐标',
    longitude            DECIMAL(10, 6) COMMENT '经度坐标',
    location_address     VARCHAR(200) COMMENT '位置地址（省市区+详细地址）',
    location_update_time DATETIME on update CURRENT_TIMESTAMP COMMENT '位置更新时间',
    create_time          datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time          datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete            tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '视障人士信息表';


DROP TABLE IF EXISTS Volunteer;
CREATE TABLE Volunteer
(
    volunteer_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '志愿者ID',
    community_id         BIGINT COMMENT '社区ID',
    is_actively_joined   BOOLEAN       DEFAULT FALSE COMMENT '是否主动加入社区',
    family_id            BIGINT COMMENT '家庭ID',
    name                 VARCHAR(50)                             COMMENT '名字',
    phone                CHAR(11)                                NOT NULL COMMENT '手机号',
    password             VARCHAR(100)                            COMMENT '密码',
	gender				 tinyint  								 NOT NULL COMMENT '性别 0-男 1-女',
    wechat_id            VARCHAR(50) COMMENT '微信账号',
    qq_id                VARCHAR(20) COMMENT 'QQ账号',
    id_card              CHAR(100) COMMENT '身份证号',
    other_info           TEXT COMMENT '其它信息',
    online_status        tinyint       DEFAULT 0 COMMENT '在线状态（用于区分是否匹配） 0-离线 1-在线 2-忙碌',
    help_count           BIGINT        DEFAULT 0 COMMENT '帮助次数',
    rating               DECIMAL(3, 1) DEFAULT 0.0 COMMENT '志愿者评分',
    latitude             DECIMAL(10, 6) COMMENT '纬度坐标',
    longitude            DECIMAL(10, 6) COMMENT '经度坐标',
    location_address     VARCHAR(200) COMMENT '位置地址（省市区+详细地址）',
    location_update_time DATETIME on update CURRENT_TIMESTAMP COMMENT '位置更新时间',
    create_time          datetime      default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time          datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete            tinyint                                 not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '志愿者信息表';



DROP TABLE IF EXISTS Activity;
CREATE TABLE Activity
(
    activity_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    community_id      BIGINT                             NOT NULL COMMENT '社区ID',
    manager_id        BIGINT                             NOT NULL COMMENT '社区管理人员ID',
    activity_name     VARCHAR(100)                       NOT NULL COMMENT '活动名字',
    activity_content  TEXT COMMENT '活动内容',
    activity_location VARCHAR(300)                       NOT NULL COMMENT '活动地点',
    max_participants  BIGINT COMMENT '活动限定人数',
    activity_status   tinyint  DEFAULT 0 COMMENT '活动状态 0-未开始 1-进行中 2-已结束 3-已取消',
    start_time        DATETIME                           NOT NULL COMMENT '活动开始时间',
    end_time          DATETIME COMMENT '活动结束时间',
    create_time       datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete         tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '活动信息表';



DROP TABLE IF EXISTS VolunteerActivitySign;
CREATE TABLE VolunteerActivitySign
(
    sign_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动报名签到ID',
    volunteer_id      BIGINT                             NOT NULL COMMENT '志愿者ID',
    sign_up_time      DATETIME                           NOT NULL COMMENT '活动报名时间',
    check_in_time     DATETIME COMMENT '活动签到时间',
    check_in_location VARCHAR(300) COMMENT '活动签到地点',
    check_out_time    DATETIME COMMENT '活动签退时间',
    create_time       datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete         tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '志愿者活动报名签到表';



DROP TABLE IF EXISTS BlindActivitySign;
CREATE TABLE BlindActivitySign
(
    sign_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动报名签到ID',
    blind_id          BIGINT                             NOT NULL COMMENT '视障人士ID',
    sign_up_time      DATETIME                           NOT NULL COMMENT '活动报名时间',
    check_in_time     DATETIME COMMENT '活动签到时间',
    check_in_location VARCHAR(300) COMMENT '活动签到地点',
    check_out_time    DATETIME COMMENT '活动签退时间',
    create_time       datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete         tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '视障人士活动报名签到表';



DROP TABLE IF EXISTS HelpPost;
CREATE TABLE HelpPost
(
    post_id       BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '求助帖ID',
    community_id  BIGINT COMMENT '社区ID',
    blind_id      BIGINT                             NOT NULL COMMENT '视障人士ID',
    volunteer_id  BIGINT COMMENT '响应志愿者ID',
    help_level    INT COMMENT '求助级别',
    help_content  TEXT                               NOT NULL COMMENT '求助内容',
    help_location VARCHAR(300) COMMENT '求助地点',
    post_status   tinyint  DEFAULT 0 COMMENT '求助帖状态 0-待响应 1-处理中 2-已完成 3-已取消',
    evaluation    TEXT COMMENT '求助评价',
    create_time   datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete     tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '求助帖表';


DROP TABLE IF EXISTS VideoHelp;
CREATE TABLE VideoHelp
(
    help_id                        BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '视频求助ID',
    channel_id                     BIGINT comment '频道id',
    blind_id                       BIGINT                     COMMENT '视障人士ID',
    blind_latitude                 DECIMAL(10, 6) COMMENT '视障人士纬度坐标',
    blind_longitude                DECIMAL(10, 6) COMMENT '视障人士经度坐标',
    blind_location_address         VARCHAR(200) COMMENT '视障人士位置地址（省市区+详细地址）',
    volunteer_id                   BIGINT COMMENT '响应志愿者ID',
    volunteer_latitude             DECIMAL(10, 6) COMMENT '响应志愿者纬度坐标',
    volunteer_longitude            DECIMAL(10, 6) COMMENT '响应志愿者经度坐标',
    volunteer_location_address     VARCHAR(200) COMMENT '响应志愿者位置地址（省市区+详细地址）',
    help_status                    tinyint  DEFAULT 0 COMMENT '求助状态 0-待响应 1-处理中 2-已完成 3-已取消',
    start_time                     DATETIME                    COMMENT '求助开始时间',
    response_time                  DATETIME COMMENT '求助响应时间',
    end_time                       DATETIME COMMENT '求助结束时间',
    duration                       TIME COMMENT '求助耗时',
    video_path                     VARCHAR(200) COMMENT '视频储存地址',
    evaluation                     TEXT COMMENT '求助评价',
    create_time                    datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time                    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete                      tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '视频求助表';



DROP TABLE IF EXISTS UrgentHelp;
CREATE TABLE UrgentHelp
(
    help_id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '紧急求助ID',
    channel_id                     BIGINT comment '频道id',
    family_id                  BIGINT                              COMMENT '家庭ID',
    blind_id                   BIGINT                              COMMENT '视障人士ID',
    blind_latitude             DECIMAL(10, 6) COMMENT '视障人士纬度坐标',
    blind_longitude            DECIMAL(10, 6) COMMENT '视障人士经度坐标',
    blind_location_address     VARCHAR(200) COMMENT '视障人士位置地址（省市区+详细地址）',
    volunteer_id               BIGINT COMMENT '响应家属(志愿者)ID',
    volunteer_latitude                   DECIMAL(10, 6) COMMENT '响应家属纬度坐标',
    volunteer_longitude        DECIMAL(10, 6) COMMENT '响应家属经度坐标',
    volunteer_location_address VARCHAR(200) COMMENT '响应家属位置地址（省市区+详细地址）',
    help_status                tinyint  DEFAULT 0 COMMENT '求助状态 0-待响应 1-处理中 2-已完成 3-已取消',
    start_time                 DATETIME                            COMMENT '求助开始时间',
    response_time              DATETIME COMMENT '求助响应时间',
    end_time                   DATETIME COMMENT '求助结束时间',
    duration                   TIME COMMENT '求助耗时',
    video_path                 VARCHAR(200) COMMENT '视频储存地址',
    create_time                datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time                datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete                  tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '紧急求助表';



DROP TABLE IF EXISTS AILogs;
CREATE TABLE AILogs
(
    log_id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'AI操作日志ID',
    operator_id    BIGINT COMMENT '操作人ID',
    content        TEXT COMMENT '发送内容',
    operation_time DATETIME                           NOT NULL COMMENT '发送时间',
    log_type       VARCHAR(50) COMMENT '类型',
    create_time    datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete      tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT 'AI操作日志表';



DROP TABLE IF EXISTS UserLogs;
CREATE TABLE UserLogs
(
    log_id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户操作日志ID',
    blind_id         BIGINT COMMENT '视障人士ID',
    volunteer_id     BIGINT COMMENT '志愿者ID',
    request_type     VARCHAR(50) COMMENT '请求类型',
    request_content  TEXT COMMENT '请求内容',
    request_time     DATETIME                           NOT NULL COMMENT '请求时间',
    request_address  VARCHAR(100) COMMENT '请求地址',
    response_content TEXT COMMENT '响应内容',
    response_time    DATETIME COMMENT '响应时间',
    create_time      datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete        tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '用户操作日志表';



CREATE TABLE Navigation
(
    navigation_id   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '导航ID',
    user_id         BIGINT                             not null COMMENT '用户ID（视障人士/志愿者）',
    user_type       tinyint                            not null COMMENT '用户类型 0-blind 1-volunteer',
    help_id         BIGINT COMMENT '求助ID（可选，关联VideoHelp/UrgentHelp）',
    navigation_type tinyint                            not null COMMENT '导航类型： 0-视频求助 1-家属紧急求助 2-独立使用 3-其他',
    start_latitude  DECIMAL(10, 6) COMMENT '起点纬度',
    start_longitude DECIMAL(10, 6) COMMENT '起点经度',
    start_address   VARCHAR(200) COMMENT '起点地址',
    end_latitude    DECIMAL(10, 6) COMMENT '终点纬度',
    end_longitude   DECIMAL(10, 6) COMMENT '终点经度',
    end_address     VARCHAR(200) COMMENT '终点地址',
    route_data      TEXT COMMENT '导航路线数据（JSON格式）',
    distance        DECIMAL(10, 2) COMMENT '距离（米）',
    duration        INT COMMENT '预计耗时（秒）',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_delete       TINYINT  DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除',

    -- 索引设计
    KEY idx_nav_user_type (user_type, user_id),
    KEY idx_nav_help_id (help_id),
    KEY idx_nav_type (navigation_type)
) COMMENT '通用导航记录表';


--   关联表

DROP TABLE IF EXISTS CommunityJoinReview;
CREATE TABLE CommunityJoinReview
(
    review_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '社区审核ID',
	family_id	  BIGINT COMMENT '家庭id',
    blind_id      BIGINT COMMENT '视障人士ID',
    volunteer_id  BIGINT COMMENT '志愿者ID',
    apply_time    DATETIME                           NOT NULL COMMENT '请求加入时间',
    review_time   DATETIME COMMENT '审核时间',
    review_status tinyint  DEFAULT 0 COMMENT '审核状态 0-待审核 1-已通过 2-已拒绝',
    reviewer_id   BIGINT COMMENT '审核志愿者ID',
    create_time   datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete     tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '社区加入审核表';



DROP TABLE IF EXISTS FamilyJoinReview;
CREATE TABLE FamilyJoinReview
(
    review_id     BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '家庭审核ID',
    blind_id      BIGINT COMMENT '视障人士ID',
    volunteer_id  BIGINT COMMENT '志愿者ID',
    apply_time    DATETIME                           NOT NULL COMMENT '请求加入时间',
    review_time   DATETIME COMMENT '审核时间',
    review_status tinyint  DEFAULT 0 COMMENT '审核状态 0-待审核 1-已通过 2-已拒绝',
    reviewer_id   BIGINT COMMENT '审核志愿者ID',
    create_time   datetime default CURRENT_TIMESTAMP not null COMMENT '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP COMMENT '信息更新时间',
    is_delete     tinyint                            not null DEFAULT 0 COMMENT '逻辑删除 0-存在 1-删除'
) COMMENT '家庭加入审核表';


-- 社区表索引
CREATE INDEX idx_community_type_level ON Community (community_type_id, community_level_id);
CREATE INDEX idx_community_default_status ON Community (is_default_community, community_status);
CREATE INDEX idx_community_address ON Community (province, city, district);

-- 志愿者表索引
CREATE INDEX idx_volunteer_community_status ON Volunteer (community_id, online_status);
CREATE INDEX idx_volunteer_family_rating ON Volunteer (family_id, rating);
ALTER TABLE Volunteer
    ADD UNIQUE INDEX uk_volunteer_phone (phone);

-- 活动表索引
CREATE INDEX idx_activity_community_status_time ON Activity (community_id, activity_status, start_time);
CREATE INDEX idx_activity_status_participants ON Activity (activity_status, max_participants);

-- 求助帖表索引
CREATE INDEX idx_helppost_blind_status_level ON HelpPost (blind_id, post_status, help_level);
CREATE INDEX idx_helppost_volunteer ON HelpPost (volunteer_id);

-- 日志表索引
CREATE INDEX idx_userlogs_time_type ON UserLogs (request_time, request_type);
CREATE INDEX idx_ailogs_operator_time ON AILogs (operator_id, operation_time);

-- 审核表索引
CREATE INDEX idx_review_user_status ON CommunityJoinReview (blind_id, volunteer_id, review_status);