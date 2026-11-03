<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%--
  ============================================================
  登录页 login.jsp
  说明：pageEncoding="UTF-8" 是中文乱码的第二处防护（第一处是 EncodingFilter）
  本页是唯一不需要登录就能访问的业务页面
  ============================================================
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>登录 - 班级学生成绩管理系统</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="login-page">

<div class="login-box">
    <h1>班级学生成绩管理系统</h1>
    <p class="subtitle">原生 JavaEE MVC · Servlet + JSP + JDBC</p>

    <%-- 登录失败提示：由 LoginServlet 通过 request.setAttribute("errorMessage") 传入 --%>
    <c:if test="${not empty requestScope.errorMessage}">
        <div class="alert alert-danger">
            <c:out value="${requestScope.errorMessage}" escapeXml="false" />
        </div>
    </c:if>

    <%-- 会话过期提示：AuthFilter 重定向时带过来的参数 --%>
    <c:if test="${param.expired == '1'}">
        <div class="alert alert-warning">登录状态已失效，请重新登录。</div>
    </c:if>

    <%-- 正常退出提示：LogoutServlet 重定向时带过来的参数 --%>
    <c:if test="${param.logout == '1'}">
        <div class="alert alert-success">您已安全退出。</div>
    </c:if>

    <%-- 表单提交到 LoginServlet（web.xml 中映射为 /login），必须用 POST，密码不能出现在地址栏 --%>
    <form action="${ctx}/login" method="post" autocomplete="off">
        <label for="username">账号</label>
        <input type="text" id="username" name="username"
               value="<c:out value='${requestScope.username}'/>"
               placeholder="教师账号或学号" maxlength="50" required autofocus>

        <label for="password">密码</label>
        <input type="password" id="password" name="password"
               placeholder="请输入密码" maxlength="50" required>

        <button type="submit" class="btn btn-primary btn-block">登 录</button>
    </form>

    <div class="demo-tip">
        <p><strong>演示账号</strong></p>
        <p>教师：<code>teacher</code> / <code>123456</code></p>
        <p>学生：<code>20230001</code> / <code>123456</code></p>
        <p>学生：<code>20230002</code> / <code>123456</code></p>
    </div>
</div>

</body>
</html>
