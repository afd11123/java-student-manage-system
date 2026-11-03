package com.score.entity;

import java.io.Serializable;
import java.util.Locale;

/**
 * 科目统计结果对象（按科目汇总）。
 * <p>
 * 说明：题目要求"统计逻辑写在 Java 代码，不能用 SQL 聚合函数"。
 * 因此 SQL 只负责把成绩明细查出来，平均值、及格率都由
 * {@code com.score.servlet.TeacherScoreServlet} 用 for 循环计算，
 * 算完的结果装进这个对象，再交给 JSP 用 EL + JSTL 渲染成统计表格。
 * </p>
 *
 * @author 课程设计
 */
public class SubjectStat implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 科目名称，例如"Java程序设计" */
    private String subject;

    /** 该科目参加考试的人数（记录条数） */
    private int count;

    /** 平均分，保留两位小数 */
    private double average;

    /** 及格人数（分数 >= 60） */
    private int passCount;

    /** 及格率，单位为百分比（0~100），例如 66.67 表示 66.67% */
    private double passRate;

    /** 最高分 */
    private double maxScore;

    /** 最低分 */
    private double minScore;

    /** 无参构造方法，JavaBean 规范要求 */
    public SubjectStat() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    @Override
    public String toString() {
        return "SubjectStat{subject='" + subject + '\''
                + ", count=" + count
                + ", average=" + average
                + ", passCount=" + passCount
                + ", passRate=" + passRate
                + '}';
    }

    // ============================================================
    // 下面是给 JSP 直接显示用的"格式化字符串"方法。
    // 之所以不交给 JSTL 的 fmt:formatNumber，是因为本项目所用的 JSTL 实现
    // 在 Tomcat 8.5 下会忽略 pattern 属性，把 79.50 显示成 79.5。
    // 在 Java 侧格式化，结果稳定且与页面 locale 无关。
    // ============================================================

    /**
     * @return 平均分，两位小数，例如 "81.33"
     */
    public String getAverageText() {
        return String.format(Locale.ROOT, "%.2f", this.average);
    }

    /**
     * @return 及格率，两位小数（不带百分号），例如 "66.67"
     */
    public String getPassRateText() {
        return String.format(Locale.ROOT, "%.2f", this.passRate);
    }

    /**
     * @return 最高分，两位小数
     */
    public String getMaxScoreText() {
        return String.format(Locale.ROOT, "%.2f", this.maxScore);
    }

    /**
     * @return 最低分，两位小数
     */
    public String getMinScoreText() {
        return String.format(Locale.ROOT, "%.2f", this.minScore);
    }
}
