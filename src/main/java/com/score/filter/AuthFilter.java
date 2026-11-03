package com.score.filter;

import com.score.entity.User;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 登录与角色权限过滤器（基于 Session 的访问控制）。
 * <p>
 * 为什么需要它？
 * 只在 Servlet 里判断登录是不够的 —— 学生完全可以在地址栏直接输入
 * {@code http://localhost:8080/score-management/teacherIndex.jsp} 绕过 Servlet
 * 看到全班成绩。本过滤器在 web.xml 中映射到 /*，对所有请求统一拦截：
 * <ul>
 *     <li>未登录：一律踢回登录页</li>
 *     <li>已登录但访问 /teacher* 却不是教师：返回 403 错误页</li>
 *     <li>已登录但访问 /student* 却不是学生：返回 403 错误页</li>
 * </ul>
 * 白名单：登录页、登录接口、错误页、静态资源，这些不拦（否则登录页自己也进不去）。
 * </p>
 *
 * @author 课程设计
 */
public class AuthFilter implements Filter {

    /** Session 中保存当前登录用户的键名，全项目统一使用这一个常量字符串 */
    public static final String LOGIN_USER = "loginUser";

    /**
     * 过滤器初始化，无需参数。
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // 无需初始化参数
    }

    /**
     * 核心方法：先判白名单，再判登录，最后判角色。
     *
     * @param request  请求
     * @param response 响应
     * @param chain    过滤器链
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // getServletPath() 返回的是去掉上下文路径后的路径，例如 /teacher/score、/login.jsp
        String path = req.getServletPath();

        // 1. 白名单直接放行
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 取 Session 中的登录用户（传 false 表示不复创建新 Session）
        HttpSession session = req.getSession(false);
        User loginUser = (User) (session == null ? null : session.getAttribute(LOGIN_USER));

        // 未登录：重定向到登录页，并带一个提示参数
        if (loginUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login.jsp?expired=1");
            return;
        }

        // 3. 角色校验：/teacher 开头的路径只允许教师，/student 开头的只允许学生
        if (path.startsWith("/teacher") && !loginUser.isTeacher()) {
            forbidden(req, resp, "当前账号是学生，无权访问教师功能。");
            return;
        }
        if (path.startsWith("/student") && !loginUser.isStudent()) {
            forbidden(req, resp, "当前账号是教师，无权访问学生功能。");
            return;
        }

        // 4. 校验通过，放行
        chain.doFilter(request, response);
    }

    /**
     * 判断请求路径是否属于"无需登录即可访问"的白名单。
     *
     * @param path 去掉了上下文路径的请求路径
     * @return 属于白名单返回 true
     */
    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        // 登录页、登录接口、错误页
        if ("/login.jsp".equals(path) || "/login".equals(path) || "/error.jsp".equals(path)) {
            return true;
        }
        // 静态资源：样式、脚本、图片，不拦（否则登录页没有样式）
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || "/favicon.ico".equals(path);
    }

    /**
     * 统一处理"已登录但越权"的请求：返回 403 状态码并转发到错误页。
     *
     * @param req     请求
     * @param resp    响应
     * @param message 展示给用户的提示
     */
    private void forbidden(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        // 403 Forbidden：服务器理解请求但拒绝执行，比 404 更准确地表达"越权"
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute("errorMessage", message);
        req.setAttribute("errorHint", "如果你认为这是误判，请退出后使用正确的账号重新登录。");
        req.getRequestDispatcher("/error.jsp").forward(req, resp);
    }

    /**
     * 过滤器销毁，无需释放资源。
     */
    @Override
    public void destroy() {
        // 无需释放资源
    }
}
