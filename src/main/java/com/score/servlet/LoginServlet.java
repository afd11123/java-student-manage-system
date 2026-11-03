package com.score.servlet;

import com.score.dao.UserDao;
import com.score.entity.User;
import com.score.filter.AuthFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * 登录 Servlet，映射路径 /login（在 web.xml 中配置）。
 * <p>
 * GET 请求：转发到登录页面。
 * POST 请求：接收账号密码 → 非空校验 → 查库比对 → 写入 Session → 按角色分流。
 * </p>
 *
 * @author 课程设计
 */
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** 数据访问对象：无状态，可以安全地作为 Servlet 的成员被多线程共享 */
    private final UserDao userDao = new UserDao();

    /**
     * GET 请求：直接打开登录页。
     * 如果已经登录过，就不再重复登录，直接跳转到对应首页。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        User loginUser = (User) (session == null ? null : session.getAttribute(AuthFilter.LOGIN_USER));
        if (loginUser != null) {
            // 已登录，按角色跳首页，避免重复登录造成 Session 混乱
            resp.sendRedirect(req.getContextPath()
                    + (loginUser.isTeacher() ? "/teacher/score?action=list" : "/student/score"));
            return;
        }
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    /**
     * POST 请求：执行登录。
     * 任何一步失败都不会直接抛异常给用户，而是带着提示信息回到登录页。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 取参并去空格：手滑输入前后空格是很常见的，这里统一容错
        String username = trim(req.getParameter("username"));
        String password = trim(req.getParameter("password"));

        // 1. 非空校验（前端有 required，后端仍然要校验，因为前端可以被绕过）
        if (username.isEmpty() || password.isEmpty()) {
            fail(req, resp, username, "账号和密码不能为空，请重新输入。");
            return;
        }

        try {
            // 2. 查询用户
            User user = userDao.findByUsername(username);

            // 3. 校验：用户不存在和密码错误给同一个提示，避免被用来枚举账号
            if (user == null || !user.getPassword().equals(password)) {
                fail(req, resp, username, "账号或密码错误，请重新输入。");
                return;
            }

            // 4. 登录成功：先销毁旧 Session 再创建新的，防止会话固定攻击（Session Fixation）
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            // 保存登录用户，后续所有权限判断都依赖这个属性
            session.setAttribute(AuthFilter.LOGIN_USER, user);
            // 30 分钟无操作自动失效
            session.setMaxInactiveInterval(30 * 60);

            // 5. 按角色分流到不同首页
            String target = user.isTeacher() ? "/teacher/score?action=list" : "/student/score";
            resp.sendRedirect(req.getContextPath() + target);

        } catch (SQLException e) {
            // 数据库异常：记录日志（写进 Tomcat 的 localhost 日志），页面只给友好提示
            log("登录时查询数据库失败，username=" + username, e);
            fail(req, resp, username, "数据库访问失败，请确认 MySQL 已启动且 db.properties 配置正确。<br>"
                    + "技术细节：" + escapeHtml(e.getMessage()));
        }
    }

    /**
     * 登录失败：把提示信息和用户已输入的账号放回 request，转发回登录页。
     * 转发（forward）而不是重定向，这样能保留 request 中的错误信息。
     *
     * @param req      请求
     * @param resp     响应
     * @param username 用户已输入的账号，用于回填输入框
     * @param message  错误提示
     */
    private void fail(HttpServletRequest req, HttpServletResponse resp, String username, String message)
            throws ServletException, IOException {
        req.setAttribute("errorMessage", message);
        req.setAttribute("username", username);
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    /**
     * 去掉首尾空格，null 安全。
     *
     * @param value 原始字符串
     * @return 去除首尾空格后的字符串，null 转为空串
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 极简 HTML 转义，防止把异常信息里的特殊字符直接拼进页面造成标记错乱。
     *
     * @param value 原始文本
     * @return 转义后的文本
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
