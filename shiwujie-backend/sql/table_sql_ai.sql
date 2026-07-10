create database if not exists `shiwujieai`;
use `shiwujieai`;

create table if not exists AiLogs
(
    log_id      bigint auto_increment comment 'AI操作日志ID'
        primary key,
    operator_id bigint                             null comment '操作人ID',
    content     longtext                           null comment '发送内容',
    log_type    varchar(50)                        null comment '类型',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '信息更新时间',
    is_delete   tinyint  default 0                 not null comment '逻辑删除 0-存在 1-删除'
)
    comment 'AI操作日志表';

create index idx_ailogs_operator_time
    on AiLogs (operator_id);
