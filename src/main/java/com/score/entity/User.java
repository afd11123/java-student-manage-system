package com.score.entity;

import java.io.Serializable;

/**
 * 用户实体类，对应数据库中的 user 表。
 * <p>
 * 设计要点：教师和学生共用一个实体、一张表，通过 {@link #role} 字段区分身份。
 * 这样登录流程只有一条，权限判断只需比对 role 值，代码最简洁。
 * </p>
 *
 * @author 课程设计
 */
public class User implements Serializable {

    /** 序列化版本号：实体可能被放入 Session，实现 Serializable 是规范做法 */
    private static final long serialVersionUID = 1L;

    /** 角色常量：教师 */
    public static final String ROLE_TEACHER = "TEACHER";

    /** 角色常量：学生 */
    public static final String ROLE_STUDENT = "STUDENT";

    /** 主键，对应 user.id */
    private Long id;

    /** 登录账号，对应 user.username；学生建议使用学号，全局唯一 */
    private String username;

    /**
     * 登录密码，对应 user.password。
     * 注意：课程设计为便于演示使用明文存储，生产环境必须改成加盐哈希（如 BCrypt）。
     */
    private String password;

    /** 真实姓名，对应 user.real_name，页面上显示"张老师""李小明" */
    private String realName;

    /** 角色，对应 user.role，取值只能是 ROLE_TEACHER 或 ROLE_STUDENT */
    private String role;

    /** 无参构造方法：JavaBean 规范要求，JDBC 反射和 JSP 取值都需要 */
    public User() {
    }

    /** 全参构造方法：方便 DAO 里一行代码组装对象 */
    public User(Long id, String username, String password, String realName, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
    }

    /**
     * 判断当前用户是不是教师。
     * 注意：方法名必须是 isTeacher()，在 JSP 里才能用 ${loginUser.teacher} 取到。
     *
     * @return 是教师返回 true
     */
    public boolean isTeacher() {
        return ROLE_TEACHER.equals(this.role);
    }

    /**
     * 判断当前用户是不是学生，JSP 中用 ${loginUser.student} 取值。
     *
     * @return 是学生返回 true
     */
    public boolean isStudent() {
        return ROLE_STUDENT.equals(this.role);
    }

    /**
     * 把英文角色值翻译成中文，供页面直接显示。
     *
     * @return 角色中文名
     */
    public String getRoleText() {
        if (isTeacher()) {
            return "教师";
        }
        if (isStudent()) {
            return "学生";
        }
        return "未知角色";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    /**
     * 调试用输出。这里故意不输出密码，避免日志泄露账号信息。
     */
    @Override
    public String toString() {
        return "User{id=" + id
                + ", username='" + username + '\''
                + ", realName='" + realName + '\''
                + ", role='" + role + '\''
                + '}';
    }
}
