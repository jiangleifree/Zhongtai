create table data_manage
(
    id          bigint auto_increment
        primary key,
    table_id    bigint           null comment '模型(表)id',
    table_name  varchar(30)      null comment '模型(表)名称',
    file_name   varchar(100)     null comment '文件名称',
    file_path   varchar(100)     null comment '上传文件的路径',
    create_time datetime         null comment '上传时间(创建时间)',
    status      char default '0' null comment '有效标志（0代表存在 1代表无效）',
    file_size   varchar(50)      null comment '文件大小',
    source      char             null comment '数据来源(0代表收割，1代表手动导入)',
    data_type   varchar(30)      null comment '数据产生类型',
    data_name   varchar(100)     null comment '接入名称',
    progress    varchar(100)     null comment '任务进度',
    update_time datetime         null comment '更新时间'
);

CREATE TABLE interface (
id INT PRIMARY KEY auto_increment COMMENT 'id',
title VARCHAR ( 52 ) DEFAULT NULL COMMENT 'title',
summary VARCHAR ( 512 ) DEFAULT NULL COMMENT '详细描述',
url VARCHAR ( 128 ) NOT NULL COMMENT 'url',
type INT ( 1 ) NOT NULL DEFAULT 1 COMMENT '接口类型,1为GET,2为POST',
consumes VARCHAR ( 30 ) DEFAULT NULL COMMENT '接口的contentType',
produces VARCHAR ( 128 ) DEFAULT "*/*" COMMENT '',
topic_name VARCHAR ( 128 ) NOT NULL COMMENT '主题库名称',
parametersJsonStr VARCHAR ( 512 ) DEFAULT NULL COMMENT '入参',
responsesJsonStr VARCHAR ( 512 ) DEFAULT NULL COMMENT '出参',
sql_str text NOT NULL COMMENT 'sql',
create_date date default now() comment '创建时间',
) ENGINE = INNODB DEFAULT CHARSET = utf8mb4 COMMENT = '主题库接口'


-- auto-generated definition
create table topic
(
    topic_id      bigint auto_increment
        primary key,
    topic_name    varchar(256)                    not null,
    create_time   datetime                        not null,
    topic_comment varchar(128)                    not null,
    db_name       varchar(128)                    not null,
    user_name     varchar(30)                     null,
    password      varchar(128)                    null,
    attarch_id    varchar(256) default 'timg.jpg' null
)
    comment '主题库信息表';


-- auto-generated definition
create table gen_table
(
    table_id        bigint auto_increment comment '编号'
        primary key,
    table_name      varchar(200) default ''     null comment '表名称',
    table_comment   varchar(500) default ''     null comment '表描述',
    class_name      varchar(100) default ''     null comment '实体类名称',
    tpl_category    varchar(200) default 'crud' null comment '使用的模板（crud单表操作 tree树表操作）',
    package_name    varchar(100)                null comment '生成包路径',
    module_name     varchar(30)                 null comment '生成模块名',
    business_name   varchar(30)                 null comment '生成业务名',
    function_name   varchar(50)                 null comment '生成功能名',
    function_author varchar(50)                 null comment '生成功能作者',
    options         varchar(1000)               null comment '其它生成选项',
    create_by       varchar(64)  default ''     null comment '创建者',
    create_time     datetime                    null comment '创建时间',
    update_by       varchar(64)  default ''     null comment '更新者',
    update_time     datetime                    null comment '更新时间',
    remark          varchar(500)                null comment '备注',
    status          char         default '0'    null comment '有效标志（0代表存在 1代表无效）'
)
    comment '代码生成业务表';

-- auto-generated definition
create table gen_table_column
(
    column_id      bigint auto_increment comment '编号'
        primary key,
    table_id       varchar(64)               null comment '归属表编号',
    column_name    varchar(200)              null comment '列名称',
    column_comment varchar(500)              null comment '列描述',
    column_type    varchar(100)              null comment '列类型',
    column_length  varchar(30)               null comment '列长度',
    java_type      varchar(500)              null comment 'JAVA类型',
    java_field     varchar(200)              null comment 'JAVA字段名',
    is_pk          char                      null comment '是否主键（1是）',
    is_increment   char                      null comment '是否自增（1是）',
    is_required    char                      null comment '是否必填（1是）',
    is_insert      char                      null comment '是否为插入字段（1是）',
    is_edit        char                      null comment '是否编辑字段（1是）',
    is_list        char                      null comment '是否列表字段（1是）',
    is_query       char                      null comment '是否查询字段（1是）',
    query_type     varchar(200) default 'EQ' null comment '查询方式（等于、不等于、大于、小于、范围）',
    html_type      varchar(200)              null comment '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
    dict_type      varchar(200) default ''   null comment '字典类型',
    sort           int                       null comment '排序',
    create_by      varchar(64)  default ''   null comment '创建者',
    create_time    datetime                  null comment '创建时间',
    update_by      varchar(64)  default ''   null comment '更新者',
    update_time    datetime                  null comment '更新时间',
    status         char         default '0'  null comment '有效标志（0代表存在 1代表无效）'
)
    comment '代码生成业务表字段';
-- auto-generated definition
create table log_info
(
    log_id      int auto_increment
        primary key,
    log_info    varchar(256) null,
    create_time datetime     null,
    task_id     int          null
);

-- auto-generated definition
create table map_storage
(
    map_id      int auto_increment
        primary key,
    map_name    varchar(30)                    null,
    create_time datetime                       null,
    map_comment varchar(256)                   null,
    status      varchar(20) default 'not init' null,
    attarch_id  varchar(128)                   null,
    map_name_en varchar(64)                    null
);

-- auto-generated definition
create table model_info
(
    id          varchar(32)          not null
        primary key,
    model_name  varchar(100)         null comment '模型名称',
    create_date datetime             null comment '创建时间',
    modify_date datetime             null comment '更新时间',
    del_flag    char(10) default '0' null comment '有效标志（0代表存在 1代表无效）',
    remark      varchar(255)         null comment '备注'
);
-- auto-generated definition
create table org
(
    org_id   int auto_increment
        primary key,
    org_name varchar(12) null
);

-- auto-generated definition
create table people
(
    id          int(20)      null,
    name        varchar(128) null,
    destination varchar(128) null
);
-- auto-generated definition
create table per
(
    per_id   int auto_increment
        primary key,
    per_name varchar(12) null
);

-- auto-generated definition
create table per_org
(
    id     int auto_increment
        primary key,
    per_id int null,
    org_id int null
);

-- auto-generated definition
create table person
(
    id     varchar(20) null comment '30',
    NAME   varchar(50) null comment '30',
    sex    varchar(40) null comment '40',
    ewqsss varchar(40) null comment '60',
    age    varchar(20) null comment '60',
    hahah  varchar(74) null comment '70'
)
    comment '7777777';

-- auto-generated definition
create table port
(
    id         int auto_increment
        primary key,
    bolt_port  int          null,
    http_port  int          null,
    https_port int          null,
    map_name   varchar(128) null
);

-- auto-generated definition
create table table_info
(
    table_info_id   bigint auto_increment
        primary key,
    table_info_name varchar(64)  null,
    columns         varchar(256) null,
    topic_name      varchar(256) null
);

-- auto-generated definition
create table task
(
    task_id       int auto_increment
        primary key,
    task_name     varchar(128)  null,
    create_time   datetime      null,
    status        varchar(20)   null,
    task_type     varchar(30)   null,
    interval_time int default 0 null,
    next_time     datetime      null,
    json_data     text          null
)
    comment 'task';

-- auto-generated definition
create table map_storage
(
    map_id      int auto_increment
        primary key,
    map_name    varchar(30)                    null,
    create_time datetime                       null,
    map_comment varchar(256)                   null,
    status      varchar(20) default 'not init' null,
    attarch_id  varchar(128)                   null,
    map_name_en varchar(64)                    null
);



-- auto-generated definition
create table topic_interface
(
    id         int auto_increment
        primary key,
    url        varchar(128) null,
    type       varchar(12)  null,
    topic_name varchar(128) null,
    table_name varchar(128) null
);

-- auto-generated definition
create table topic_interface_statistics
(
    id                 int auto_increment
        primary key,
    topic_interface_id int          null,
    param              varchar(128) null,
    call_time          datetime     null,
    user_ip            varchar(30)  null
);


