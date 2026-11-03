package com.score.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Locale;

/**
 * 成绩实体类，对应数据库中的 score 表。
 * <p>
 * 字段 {@link #studentName} 不来自 score 表，而是 DAO 里用 JOIN user 查询出来顺便填充的，
 * 这样 JSP 页面不用再根据 studentId 二次查询就能直接显示学生姓名。
 * </p>
 *
 * @author 课程设计
 */
public class Score implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 及格线：60 分。抽成常量，统计及格率时全项目只认这一个值 */
    public static final double PASS_LINE = 60.0;

    /** 主键，对应 score.id */
    private Long id;

    /** 学生用户 ID，对应 score.student_id，外键指向 user.id */
    private Long studentId;

    /**
     * 学生姓名，对应 user.real_name。
     * 这不是 score 表的字段，是联表查询的"展示用"字段，所以数据库里没有对应列。
     */
    private String studentName;

    /** 科目名称，对应 score.subject，例如"Java程序设计" */
    private String subject;

    /** 考试分数，对应 score.score，数据库类型 DECIMAL(5,2)，合法范围 0~100 */
    private double score;

    /**
     * 考试日期，对应 score.exam_date。
     * 用 Java 8 的 LocalDate 而不是 java.util.Date：
     * 它是不可变对象、没有时分秒干扰，toString() 直接就是 yyyy-MM-dd，JSP 里 ${s.examDate} 即得标准格式。
     */
    private LocalDate examDate;

    /** 无参构造方法，JavaBean 规范要求 */
    public Score() {
    }

    /** 全参构造方法，DAO 查询和新增时使用 */
    public Score(Long id, Long studentId, String studentName, String subject, double score, LocalDate examDate) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.subject = subject;
        this.score = score;
        this.examDate = examDate;
    }

    /**
     * 判断该成绩是否及格（>= 60 分）。
     * 方法名必须是 isPass()，JSP 里才能写 ${s.pass} 来给表格标红。
     *
     * @return 及格返回 true
     */
    public boolean isPass() {
        return this.score >= PASS_LINE;
    }

    /**
     * 返回保留两位小数的分数字符串，供 JSP 直接输出（例如 92.00）。
     * <p>
     * 为什么不用 JSTL 的 {@code <fmt:formatNumber pattern="0.00"/>}？
     * 实测本项目所用的 JSTL 1.2 实现在 Tomcat 8.5 下 pattern / type 属性会失效，
     * 会把 double 原样输出（显示成 92.0 而不是 92.00）。
     * 放到 Java 里格式化既可靠，也不受页面 locale 影响。
     * </p>
     *
     * @return 形如 "92.00" 的字符串
     */
    public String getScoreText() {
        // Locale.ROOT 保证小数点永远是 "."，不会因为系统区域设置变成逗号
        return String.format(Locale.ROOT, "%.2f", this.score);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    @Override
    public String toString() {
        return "Score{id=" + id
                + ", studentId=" + studentId
                + ", studentName='" + studentName + '\''
                + ", subject='" + subject + '\''
                + ", score=" + score
                + ", examDate=" + examDate
                + '}';
    }
}
