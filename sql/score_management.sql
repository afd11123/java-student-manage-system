-- ============================================================
--  班级学生成绩管理系统 —— 数据库建表脚本
--  适用：MySQL 8.0.16 及以上（CHECK 约束从 8.0.16 开始真正生效）
--  执行方式（任选其一）：
--    1) Navicat / SQLyog / DataGrip 里打开本文件，全选执行
--    2) 命令行：mysql -u root -p < score_management.sql
--  执行后即可得到：数据库 score_management + 两张表 + 演示数据
-- ============================================================

-- 建库：utf8mb4 才能存中文姓名、中文科目名，以及 emoji 等 4 字节字符
CREATE DATABASE IF NOT EXISTS score_management
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE score_management;

-- 如果需要"推倒重来"，先执行下面两行再跑本脚本（注意：会清空全部数据）
-- DROP TABLE IF EXISTS score;
-- DROP TABLE IF EXISTS `user`;


-- ============================================================
--  表 1：user —— 用户表（教师和学生共用一张表）
-- ============================================================
--  设计说明：
--    1. 学生不单独建表，用 role='STUDENT' 区分，成绩表通过 student_id 关联到本表
--    2. username 加唯一索引，避免出现两个一样的学号
--    3. password 在课程设计中使用明文，生产环境必须改为加盐哈希
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT           COMMENT '用户主键，自增',
    `username`   VARCHAR(50)  NOT NULL                          COMMENT '登录账号；学生建议直接使用学号',
    `password`   VARCHAR(120) NOT NULL                          COMMENT '登录密码；课程演示为明文，生产环境应存加密摘要',
    `real_name`  VARCHAR(50)  NOT NULL                          COMMENT '真实姓名，页面显示用',
    `role`       VARCHAR(20)  NOT NULL                          COMMENT '角色：TEACHER=教师，STUDENT=学生',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    -- 限定角色只能取这两个值，防止写错成 "teacher" 小写导致登录后无法识别
    CONSTRAINT `chk_user_role` CHECK (`role` IN ('TEACHER', 'STUDENT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '系统用户表（教师 + 学生）';


-- ============================================================
--  表 2：score —— 学生成绩表
-- ============================================================
--  设计说明：
--    1. student_id 外键指向 user.id，并用 ON DELETE CASCADE：删学生账号时成绩自动清理
--    2. score 用 DECIMAL(5,2) 而不是 FLOAT：分数必须精确到两位小数，
--       FLOAT 会出现 85.4999999 这类精度问题
--    3. 唯一键 (student_id, subject, exam_date) 防止同一场考试重复录入
--    4. CHECK 约束兜底分数范围；Java 代码里还会再校验一次（双保险）
-- ============================================================
CREATE TABLE IF NOT EXISTS `score` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT             COMMENT '成绩主键，自增',
    `student_id` BIGINT       NOT NULL                            COMMENT '学生用户ID，外键关联 user.id',
    `subject`    VARCHAR(100) NOT NULL                            COMMENT '科目名称，如：Java程序设计',
    `score`      DECIMAL(5,2) NOT NULL                            COMMENT '考试分数，范围 0.00 ~ 100.00',
    `exam_date`  DATE         NOT NULL                            COMMENT '考试日期',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '录入时间',
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP                               COMMENT '最后修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_score_student_id` (`student_id`)                     COMMENT '按学生查成绩的高频条件',
    KEY `idx_score_subject` (`subject`)                           COMMENT '按科目统计的高频条件',
    UNIQUE KEY `uk_score_student_subject_date` (`student_id`, `subject`, `exam_date`),
    CONSTRAINT `fk_score_student` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT `chk_score_range` CHECK (`score` BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '学生成绩表';


-- ============================================================
--  演示数据
--  教师：teacher / 123456
--  学生：20230001（李小明）、20230002（王小华）、20230003（赵小雨）/ 123456
-- ============================================================
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `real_name`, `role`) VALUES
    (1, 'teacher',  '123456', '张老师', 'TEACHER'),
    (2, '20230001', '123456', '李小明', 'STUDENT'),
    (3, '20230002', '123456', '王小华', 'STUDENT'),
    (4, '20230003', '123456', '赵小雨', 'STUDENT');

-- 三个科目刻意设计了不同的及格率，方便演示"按科目统计"功能：
--   Java程序设计：3 人全部及格       → 及格率 100.00%
--   数据库原理　：3 人中 2 人及格     → 及格率  66.67%
--   高等数学　　：3 人中 1 人及格     → 及格率  33.33%
INSERT IGNORE INTO `score` (`id`, `student_id`, `subject`, `score`, `exam_date`) VALUES
    (1, 2, 'Java程序设计', 92.00, '2026-10-12'),
    (2, 3, 'Java程序设计', 64.00, '2026-10-12'),
    (3, 4, 'Java程序设计', 88.00, '2026-10-12'),

    (4, 2, '数据库原理',   85.50, '2026-10-15'),
    (5, 3, '数据库原理',   58.00, '2026-10-15'),
    (6, 4, '数据库原理',   95.00, '2026-10-15'),

    (7, 2, '高等数学',     78.00, '2026-10-18'),
    (8, 3, '高等数学',     42.00, '2026-10-18'),
    (9, 4, '高等数学',     55.00, '2026-10-18');


-- ============================================================
--  验证脚本执行结果（可选，执行后看输出对不对）
-- ============================================================
-- SELECT * FROM `user`;
-- SELECT s.id, u.real_name AS 学生, s.subject AS 科目, s.score AS 分数, s.exam_date AS 考试日期
-- FROM score s JOIN `user` u ON s.student_id = u.id
-- ORDER BY u.username, s.subject;
