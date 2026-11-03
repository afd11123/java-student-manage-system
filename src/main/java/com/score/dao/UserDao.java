package com.score.dao;

import com.score.entity.User;
import com.score.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问对象（DAO）。
 * <p>
 * 只负责和 user 表打交道，把结果集转换成 {@link User} 对象返回，
 * 不包含任何业务判断（比如"密码对不对"是 Servlet 的事）。
 * 所有 SQL 一律使用 PreparedStatement 占位符，杜绝 SQL 注入。
 * </p>
 *
 * @author 课程设计
 */
public class UserDao {

    /**
     * 查询用户表中除主键外的公共字段。
     * 抽成常量是为了三个查询方法复用，避免字段写错。
     */
    private static final String COLUMNS = "id, username, password, real_name, role";

    /**
     * 根据登录账号查询用户（登录时使用）。
     *
     * @param username 登录账号，学生一般为学号
     * @return 查到的用户；不存在时返回 null（不是抛异常，由调用方判断）
     * @throws SQLException 数据库访问异常
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `user` WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * 根据主键查询用户（学生端拿到 Session 里的 id 后回查用）。
     *
     * @param id 用户主键
     * @return 查到的用户；不存在时返回 null
     * @throws SQLException 数据库访问异常
     */
    public User findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `user` WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * 查询全部学生，按账号（学号）升序。
     * <p>用途：教师新增/修改成绩时，"学生"一栏用下拉框选择，需要这份数据。</p>
     *
     * @return 学生列表，无数据时返回空集合（不会是 null，页面可直接 forEach）
     * @throws SQLException 数据库访问异常
     */
    public List<User> findAllStudents() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM `user` WHERE role = ? ORDER BY username ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, User.ROLE_STUDENT);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 把当前行数据映射成 User 对象。
     * 每个 DAO 都有这样一个私有方法，是"避免重复代码"的常见写法。
     *
     * @param rs 已定位到某一行的结果集
     * @return 用户对象
     * @throws SQLException 取列失败时抛出
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));
        user.setRole(rs.getString("role"));
        return user;
    }
}
