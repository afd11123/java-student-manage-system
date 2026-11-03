package com.score.servlet;

import com.score.dao.ScoreDao;
import com.score.entity.Score;
import com.score.entity.User;
import com.score.filter.AuthFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * 学生端成绩查询 Servlet，映射路径 /student/score。
 * <p>
 * <b>本类最重要的安全设计：查询条件只来自 Session，绝不读取浏览器传来的 studentId。</b>
 * 学生 A 就算把地址改成 {@code /student/score?studentId=3}，也只能看到自己的成绩，
 * 因为 Servlet 根本不解析这个参数。这是"不能访问他人数据"这条要求的根本保障，
 * 配合 AuthFilter 的 URL 拦截构成双保险。
 * </p>
 *
 * @author 课程设计
 */
public class StudentScoreServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 成绩数据访问对象 */
    private final ScoreDao scoreDao = new ScoreDao();

    /**
     * 查询当前登录学生的成绩并展示。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 1. 从 Session 取当前登录用户
        HttpSession session = req.getSession(false);
        User loginUser = (User) (session == null ? null : session.getAttribute(AuthFilter.LOGIN_USER));

        // 2. 未登录或不是学生：踢回登录页
        if (loginUser == null || !loginUser.isStudent()) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?expired=1");
            return;
        }

        try {
            // 3. 只用自己 Session 里的 id 查询，忽略一切外部参数
            List<Score> scores = scoreDao.findByStudentId(loginUser.getId());

            // 4. 个人统计同样用 Java 计算，不写 SQL 聚合
            int count = scores.size();
            int passCount = 0;
            double sum = 0;
            for (Score score : scores) {
                sum += score.getScore();
                if (score.getScore() >= Score.PASS_LINE) {
                    passCount++;
                }
            }

            req.setAttribute("scores", scores);
            req.setAttribute("totalCount", count);
            req.setAttribute("passCount", passCount);
            req.setAttribute("failCount", count - passCount);

            // 统计值同时提供"数值"和"两位小数字符串"两种形式：
            // 数值供后续可能的逻辑判断，字符串直接给页面显示（不依赖 JSTL 的格式化标签）
            double average = count == 0 ? 0.0 : round(sum / count);
            double passRate = count == 0 ? 0.0 : round(passCount * 100.0 / count);
            req.setAttribute("average", average);
            req.setAttribute("passRate", passRate);
            req.setAttribute("averageText", String.format(Locale.ROOT, "%.2f", average));
            req.setAttribute("passRateText", String.format(Locale.ROOT, "%.2f", passRate));

            // 把学生本人信息也传给页面，用于显示"李小明（20230001）"
            req.setAttribute("student", loginUser);

            req.getRequestDispatcher("/studentIndex.jsp").forward(req, resp);
        } catch (SQLException e) {
            log("学生端查询成绩失败，studentId=" + loginUser.getId(), e);
            req.setAttribute("errorMessage", "查询成绩失败：" + e.getMessage());
            req.setAttribute("errorHint", "请稍后重试；如果持续失败，请联系老师检查数据库。");
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }
    }

    /**
     * 学生端没有写操作，POST 也按查询处理，避免出现 405。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
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
}
