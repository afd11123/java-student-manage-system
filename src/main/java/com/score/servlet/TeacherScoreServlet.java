package com.score.servlet;

import com.score.dao.ScoreDao;
import com.score.dao.UserDao;
import com.score.entity.Score;
import com.score.entity.SubjectStat;
import com.score.entity.User;
import com.score.filter.AuthFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师端成绩管理 Servlet，映射路径 /teacher/score。
 * <p>
 * 一个 Servlet 通过 action 参数承载教师的所有操作，页面始终是 teacherIndex.jsp：
 * <table border="1">
 *     <tr><th>action</th><th>方法</th><th>作用</th></tr>
 *     <tr><td>list（默认）</td><td>GET</td><td>查询全部成绩 + 按科目统计</td></tr>
 *     <tr><td>edit</td><td>GET</td><td>查出某条成绩，回填到页面表单</td></tr>
 *     <tr><td>save</td><td>POST</td><td>有 id 则修改，无 id 则新增</td></tr>
 *     <tr><td>delete</td><td>POST</td><td>删除一条成绩</td></tr>
 * </table>
 * 所有写操作成功后都使用"重定向"（PRG 模式），防止用户按 F5 重复提交。
 * </p>
 *
 * @author 课程设计
 */
public class TeacherScoreServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 成绩数据访问对象 */
    private final ScoreDao scoreDao = new ScoreDao();

    /** 用户数据访问对象（新增成绩时要查学生下拉列表） */
    private final UserDao userDao = new UserDao();

    /** 教师端唯一视图 */
    private static final String VIEW = "/teacherIndex.jsp";

    /** 列表页地址，写操作完成后重定向到这里 */
    private static final String LIST_URL = "/teacher/score?action=list";

    /**
     * GET 请求：列表（默认）或进入编辑态。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 双保险：AuthFilter 已经拦过一次，这里再判一次，防止将来过滤器配置被改坏
        if (requireTeacher(req, resp) == null) {
            return;
        }
        String action = trim(req.getParameter("action"));
        if ("edit".equals(action)) {
            showEdit(req, resp);
        } else {
            showList(req, resp);
        }
    }

    /**
     * POST 请求：保存（新增/修改）或删除。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (requireTeacher(req, resp) == null) {
            return;
        }
        String action = trim(req.getParameter("action"));
        if ("delete".equals(action)) {
            deleteScore(req, resp);
        } else if ("save".equals(action)) {
            saveScore(req, resp);
        } else {
            // 未知 action，退回列表，不报错
            showList(req, resp);
        }
    }

    /**
     * 展示成绩列表 + 科目统计。
     * <p>统计结果不来自 SQL，而是把明细查出来后在 {@link #buildStats(List)} 里用 Java 累加。</p>
     *
     * @param req  请求
     * @param resp 响应
     */
    private void showList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            // 1. 查全部成绩明细（SQL 里没有任何聚合函数）
            List<Score> scores = scoreDao.findAll();
            // 2. 用 Java 计算按科目的平均分、及格率
            List<SubjectStat> stats = buildStats(scores);

            req.setAttribute("scores", scores);
            req.setAttribute("stats", stats);
            // 3. 查学生列表，供页面上的"学生"下拉框使用
            req.setAttribute("students", userDao.findAllStudents());
            // 4. 汇总数字，给页面顶部的卡片用
            req.setAttribute("totalCount", scores.size());
            req.setAttribute("passTotal", countPass(scores));

            req.getRequestDispatcher(VIEW).forward(req, resp);
        } catch (SQLException e) {
            log("教师端查询成绩列表失败", e);
            forwardError(req, resp, "查询成绩失败：" + e.getMessage());
        }
    }

    /**
     * 进入编辑态：查出指定成绩放进 request，页面表单会自动回填。
     *
     * @param req  请求
     * @param resp 响应
     */
    private void showEdit(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id;
        try {
            id = Long.parseLong(trim(req.getParameter("id")));
        } catch (NumberFormatException e) {
            forwardError(req, resp, "参数错误：成绩编号不合法。");
            return;
        }
        try {
            Score score = scoreDao.findById(id);
            if (score == null) {
                forwardError(req, resp, "要修改的成绩不存在，可能已被删除，请刷新列表后重试。");
                return;
            }
            req.setAttribute("formScore", score);
            // 编辑页与列表页是同一个 JSP，所以还要把列表和统计一并准备好
            showList(req, resp);
        } catch (SQLException e) {
            log("查询单条成绩失败，id=" + id, e);
            forwardError(req, resp, "查询成绩失败：" + e.getMessage());
        }
    }

    /**
     * 保存成绩：id 为空执行新增，否则执行修改。
     * <p>校验顺序：非空 → 格式 → 范围。任何一项不通过都不触碰数据库。</p>
     *
     * @param req  请求
     * @param resp 响应
     */
    private void saveScore(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. 取出全部表单参数并去空格
        String idText = trim(req.getParameter("id"));
        String studentIdText = trim(req.getParameter("studentId"));
        String subject = trim(req.getParameter("subject"));
        String scoreText = trim(req.getParameter("score"));
        String examDateText = trim(req.getParameter("examDate"));

        // 2. 逐项校验，把错误收集起来一次性提示，用户体验比"报一个改一个"好
        List<String> errors = new ArrayList<>();

        Long id = parseLongOrNull(idText);

        Long studentId = null;
        if (studentIdText.isEmpty()) {
            errors.add("请选择学生。");
        } else {
            studentId = parseLongOrNull(studentIdText);
            if (studentId == null) {
                errors.add("学生信息不合法。");
            }
        }

        if (subject.isEmpty()) {
            errors.add("科目名称不能为空。");
        } else if (subject.length() > 100) {
            errors.add("科目名称不能超过 100 个字符。");
        }

        Double scoreValue = null;
        if (scoreText.isEmpty()) {
            errors.add("分数不能为空。");
        } else {
            try {
                scoreValue = Double.valueOf(scoreText);
                // 重点：分数必须 0~100，前端有 min/max，后端必须再校验一次
                if (scoreValue < 0 || scoreValue > 100) {
                    errors.add("分数必须在 0 ~ 100 之间，当前输入为 " + scoreText + "。");
                    scoreValue = null;
                }
            } catch (NumberFormatException e) {
                errors.add("分数必须是数字，当前输入为 " + scoreText + "。");
            }
        }

        LocalDate examDate = null;
        if (examDateText.isEmpty()) {
            errors.add("考试日期不能为空。");
        } else {
            try {
                examDate = LocalDate.parse(examDateText);
            } catch (DateTimeParseException e) {
                errors.add("考试日期格式不正确，正确格式为 yyyy-MM-dd。");
            }
        }

        // 3. 校验不通过：带着错误提示和用户已填内容回到页面
        if (!errors.isEmpty()) {
            req.setAttribute("errorMessage", joinMessages(errors));
            req.setAttribute("formScore", buildFormScore(id, studentId, subject, scoreText, examDateText));
            showList(req, resp);
            return;
        }

        // 4. 组装实体并写库
        Score score = new Score();
        score.setId(id);
        score.setStudentId(studentId);
        score.setSubject(subject);
        score.setScore(scoreValue);
        score.setExamDate(examDate);

        try {
            if (id == null) {
                scoreDao.insert(score);
                flash(req, "success", "新增成绩成功：" + subject + " " + scoreValue + " 分。");
            } else {
                int rows = scoreDao.update(score);
                if (rows > 0) {
                    flash(req, "success", "成绩修改成功。");
                } else {
                    flash(req, "warning", "修改失败：该成绩不存在，可能已被删除。");
                }
            }
        } catch (SQLException e) {
            log("保存成绩失败", e);
            flash(req, "danger", "保存成绩失败：" + e.getMessage()
                    + "（同一学生、同一科目、同一日期只能有一条记录）");
        }
        // 5. 重定向回列表（PRG 模式，防止刷新重复提交）
        resp.sendRedirect(req.getContextPath() + LIST_URL);
    }

    /**
     * 删除一条成绩。
     *
     * @param req  请求
     * @param resp 响应
     */
    private void deleteScore(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id;
        try {
            id = Long.parseLong(trim(req.getParameter("id")));
        } catch (NumberFormatException e) {
            forwardError(req, resp, "参数错误：成绩编号不合法。");
            return;
        }
        try {
            int rows = scoreDao.deleteById(id);
            if (rows > 0) {
                flash(req, "success", "删除成功。");
            } else {
                flash(req, "warning", "删除失败：该成绩不存在。");
            }
        } catch (SQLException e) {
            log("删除成绩失败，id=" + id, e);
            flash(req, "danger", "删除失败：" + e.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + LIST_URL);
    }

    /**
     * 按科目分组统计：人数、平均分、及格人数、及格率、最高分、最低分。
     * <p>
     * 这里是<b>题目要求的核心</b>：不用 SQL 的 AVG/COUNT，全部在 Java 里 for 循环算。
     * 好处是逻辑可读、可单测；数据量大时才应该考虑交给数据库。
     * </p>
     *
     * @param scores 全部成绩明细
     * @return 每个科目一条统计结果，顺序与明细中科目首次出现的顺序一致
     */
    private List<SubjectStat> buildStats(List<Score> scores) {
        // LinkedHashMap 保证统计表格的行顺序稳定，不会每次刷新都变
        Map<String, List<Score>> grouped = new LinkedHashMap<>();
        for (Score score : scores) {
            String subject = score.getSubject() == null ? "未命名科目" : score.getSubject();
            List<Score> bucket = grouped.get(subject);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(subject, bucket);
            }
            bucket.add(score);
        }

        List<SubjectStat> stats = new ArrayList<>();
        for (Map.Entry<String, List<Score>> entry : grouped.entrySet()) {
            List<Score> bucket = entry.getValue();
            int count = bucket.size();      // 参考人数
            double sum = 0;                 // 总分
            int passCount = 0;              // 及格人数
            double max = Double.NEGATIVE_INFINITY;
            double min = Double.POSITIVE_INFINITY;

            for (Score score : bucket) {
                double value = score.getScore();
                sum += value;                                   // 累加总分
                if (value >= Score.PASS_LINE) {                 // 60 分及格
                    passCount++;
                }
                if (value > max) {
                    max = value;
                }
                if (value < min) {
                    min = value;
                }
            }

            SubjectStat stat = new SubjectStat();
            stat.setSubject(entry.getKey());
            stat.setCount(count);
            stat.setAverage(round(sum / count));                      // 平均分 = 总分 / 人数
            stat.setPassCount(passCount);
            stat.setPassRate(round(passCount * 100.0 / count));       // 及格率 = 及格人数 / 人数 × 100
            stat.setMaxScore(max);
            stat.setMinScore(min);
            stats.add(stat);
        }
        return stats;
    }

    /**
     * 统计及格条数。
     *
     * @param scores 成绩列表
     * @return 及格数
     */
    private int countPass(List<Score> scores) {
        int pass = 0;
        for (Score score : scores) {
            if (score.getScore() >= Score.PASS_LINE) {
                pass++;
            }
        }
        return pass;
    }

    /**
     * 把错误信息列表拼成一段话，用中文分号分隔。
     *
     * @param messages 错误列表
     * @return 拼接后的提示文本
     */
    private String joinMessages(List<String> messages) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(i + 1).append(") ").append(messages.get(i));
        }
        return sb.toString();
    }

    /**
     * 校验失败时，把用户已经填的内容重新组装成对象送回页面，避免表单被清空。
     *
     * @param id           成绩 id（新增时为空）
     * @param studentId    学生 id
     * @param subject      科目
     * @param scoreText    分数原文（可能不是合法数字）
     * @param examDateText 日期原文
     * @return 用于回填的表单对象
     */
    private Score buildFormScore(Long id, Long studentId, String subject, String scoreText, String examDateText) {
        Score form = new Score();
        form.setId(id);
        form.setStudentId(studentId);
        form.setSubject(subject);
        try {
            form.setScore(Double.parseDouble(scoreText));
        } catch (NumberFormatException ignored) {
            // 原文不是数字，保持默认 0，页面照样能显示
        }
        try {
            form.setExamDate(LocalDate.parse(examDateText));
        } catch (DateTimeParseException ignored) {
            // 原文不是合法日期，保持 null，页面显示为空
        }
        return form;
    }

    /**
     * 把字符串转成 Long，失败返回 null。
     *
     * @param text 原始字符串
     * @return 解析结果或 null
     */
    private Long parseLongOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 保留两位小数。
     *
     * @param value 原始数值
     * @return 四舍五入到两位小数
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 写入一次性提示信息（存在 Session 里，页面显示后立即删除）。
     * <p>这是"重定向后还能提示成功/失败"的标准做法，叫 Flash 消息。</p>
     *
     * @param req     请求
     * @param type    提示类型：success / warning / danger
     * @param message 提示内容
     */
    private void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession(true);
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    /**
     * 获取当前登录用户并确认是教师。
     *
     * @param req  请求
     * @param resp 响应
     * @return 教师用户；不合法时返回 null（此时已经做了重定向）
     */
    private User requireTeacher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        User loginUser = (User) (session == null ? null : session.getAttribute(AuthFilter.LOGIN_USER));
        if (loginUser == null || !loginUser.isTeacher()) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?expired=1");
            return null;
        }
        return loginUser;
    }

    /**
     * 出错时转发到统一错误页。
     *
     * @param req     请求
     * @param resp    响应
     * @param message 错误信息
     */
    private void forwardError(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute("errorMessage", message);
        req.setAttribute("errorHint", "请返回上一页重试；如果问题持续出现，请检查数据库连接。");
        req.getRequestDispatcher("/error.jsp").forward(req, resp);
    }

    /**
     * 去掉首尾空格，null 安全。
     *
     * @param value 原始字符串
     * @return 处理后的字符串
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
