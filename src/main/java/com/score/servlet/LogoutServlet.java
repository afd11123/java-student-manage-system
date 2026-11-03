package com.score.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 退出登录 Servlet，映射路径 /logout（在 web.xml 中配置）。
 * <p>
 * 关键点：必须调用 {@link HttpSession#invalidate()} 销毁 Session，
 * 否则用户点了"退出"后按浏览器后退键，还能看到之前的页面。
 * </p>
 *
 * @author 课程设计
 */
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * 处理退出请求。
     * 用 GET 是因为页面上是一个普通链接；它不修改业务数据，所以不违反"增删改用 POST"的原则。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 传 false：没有 Session 就不新建，避免为未登录用户凭空创建会话
        HttpSession session = req.getSession(false);
        if (session != null) {
            // 销毁 Session，Session 里的 loginUser 随之消失
            session.invalidate();
        }
        // 重定向到登录页，带上 logout=1 让页面提示"已安全退出"
        resp.sendRedirect(req.getContextPath() + "/login.jsp?logout=1");
    }

    /**
     * 如果用户用 POST 访问，也走同一套逻辑，避免出现 405 错误。
     *
     * @param req  请求
     * @param resp 响应
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doGet(req, resp);
    }
}
