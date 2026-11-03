package com.score.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 统一编码过滤器：解决中文乱码（第一处防护）。
 * <p>
 * 乱码问题在 JavaEE 里一共要治三处，缺一处都可能出问题：
 * <ol>
 *     <li><b>本过滤器</b>：把请求体和响应都设为 UTF-8，解决"表单提交的中文到后台变问号"</li>
 *     <li><b>JSP 的 pageEncoding</b>：解决"页面输出的中文变问号"</li>
 *     <li><b>JDBC 的 characterEncoding</b>：解决"存进数据库的中文变问号"</li>
 * </ol>
 * 在 web.xml 中映射到 /*，并且必须排在权限过滤器之前。
 * </p>
 *
 * @author 课程设计
 */
public class EncodingFilter implements Filter {

    /** 全站统一使用的字符集 */
    private static final String CHARSET = "UTF-8";

    /**
     * 过滤器初始化，本项目不需要读取任何参数，留空即可。
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // 无需初始化参数
    }

    /**
     * 核心方法：在请求进入 Servlet / JSP 之前设置好编码。
     *
     * @param request  请求
     * @param response 响应
     * @param chain    过滤器链，必须调用 doFilter 放行，否则请求会"卡死"在这里
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 设置请求参数编码：只对 POST 请求体有效（GET 的查询串由 Tomcat 的 URIEncoding 决定）
        request.setCharacterEncoding(CHARSET);
        // 设置响应编码，保证直接往外写字符串时也是 UTF-8
        response.setCharacterEncoding(CHARSET);
        // 放行，交给下一个过滤器或目标资源
        chain.doFilter(request, response);
    }

    /**
     * 过滤器销毁，无需释放资源。
     */
    @Override
    public void destroy() {
        // 无需释放资源
    }
}
