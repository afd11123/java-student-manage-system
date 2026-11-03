package com.score.dao;

import com.score.entity.Score;
import com.score.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 成绩数据访问对象（DAO）。
 * <p>
 * 重要设计说明：
 * <ul>
 *     <li>这里的所有查询都是"查明细"，<b>绝不使用 COUNT/AVG/SUM 等 SQL 聚合函数</b>，
 *         因为题目要求统计逻辑必须写在 Java 代码里，由 Servlet 用 for 循环完成。</li>
 *     <li>查询时用 JOIN 带出学生姓名（u.real_name），避免 JSP 里二次查询。</li>
 *     <li>新增/修改/删除全部使用 PreparedStatement 占位符，防止 SQL 注入。</li>
 * </ul>
 * </p>
 *
 * @author 课程设计
 */
public class ScoreDao {

    /**
     * 公共查询语句：成绩表左连接用户表，顺带取出学生姓名。
     * 用 JOIN（内连接）即可，因为 student_id 有外键约束，不会出现找不到学生的情况。
     */
    private static final String BASE_SELECT =
            "SELECT s.id, s.student_id, u.real_name AS student_name, s.subject, s.score, s.exam_date "
                    + "FROM score s JOIN `user` u ON s.student_id = u.id ";

    /**
     * 查询全部成绩（教师端使用）。
     * 按学生姓名、科目排序，页面表格看起来更整齐。
     *
     * @return 成绩列表，无数据时为空集合（不是 null）
     * @throws SQLException 数据库访问异常
     */
    public List<Score> findAll() throws SQLException {
        String sql = BASE_SELECT + "ORDER BY u.username ASC, s.subject ASC, s.exam_date DESC";
        List<Score> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 根据学生 ID 查询该生全部成绩（学生端使用）。
     * <p>学生端只会传入 Session 中自己的 id，绝不接受浏览器传来的 studentId。</p>
     *
     * @param studentId 学生用户主键
     * @return 该生成绩列表，无数据时为空集合
     * @throws SQLException 数据库访问异常
     */
    public List<Score> findByStudentId(long studentId) throws SQLException {
        String sql = BASE_SELECT + "WHERE s.student_id = ? ORDER BY s.exam_date DESC, s.subject ASC";
        List<Score> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 根据成绩主键查询单条记录（教师点"编辑"时回填表单）。
     *
     * @param id 成绩主键
     * @return 查到的成绩；不存在时返回 null
     * @throws SQLException 数据库访问异常
     */
    public Score findById(long id) throws SQLException {
        String sql = BASE_SELECT + "WHERE s.id = ?";
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
     * 新增一条成绩。
     *
     * @param score 成绩对象，id 字段会被忽略（由数据库自增）
     * @return 受影响行数，1 表示成功
     * @throws SQLException 数据库访问异常（如外键不存在、唯一键冲突、分数越界）
     */
    public int insert(Score score) throws SQLException {
        String sql = "INSERT INTO score (student_id, subject, score, exam_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, score.getStudentId());
            ps.setString(2, score.getSubject());
            ps.setDouble(3, score.getScore());
            // LocalDate 转 java.sql.Date 才能被 JDBC 正确写入 DATE 列
            ps.setDate(4, Date.valueOf(score.getExamDate()));
            return ps.executeUpdate();
        }
    }

    /**
     * 修改一条成绩。
     *
     * @param score 成绩对象，必须携带 id
     * @return 受影响行数，1 表示成功，0 表示记录不存在
     * @throws SQLException 数据库访问异常
     */
    public int update(Score score) throws SQLException {
        String sql = "UPDATE score SET student_id = ?, subject = ?, score = ?, exam_date = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, score.getStudentId());
            ps.setString(2, score.getSubject());
            ps.setDouble(3, score.getScore());
            ps.setDate(4, Date.valueOf(score.getExamDate()));
            ps.setLong(5, score.getId());
            return ps.executeUpdate();
        }
    }

    /**
     * 根据主键删除成绩。
     *
     * @param id 成绩主键
     * @return 受影响行数，1 表示成功，0 表示记录不存在
     * @throws SQLException 数据库访问异常
     */
    public int deleteById(long id) throws SQLException {
        String sql = "DELETE FROM score WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        }
    }

    /**
     * 把结果集的一行映射成 Score 对象。
     *
     * @param rs 已定位到某一行的结果集
     * @return 成绩对象
     * @throws SQLException 取列失败时抛出
     */
    private Score mapRow(ResultSet rs) throws SQLException {
        Score score = new Score();
        score.setId(rs.getLong("id"));
        score.setStudentId(rs.getLong("student_id"));
        score.setStudentName(rs.getString("student_name"));
        score.setSubject(rs.getString("subject"));
        score.setScore(rs.getDouble("score"));
        // java.sql.Date → LocalDate，JDK 8 提供的方法
        Date examDate = rs.getDate("exam_date");
        score.setExamDate(examDate == null ? null : examDate.toLocalDate());
        return score;
    }
}
