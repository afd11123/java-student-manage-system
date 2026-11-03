<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" isErrorPage="true" %>
<%--
  ============================================================
  统一错误页 error.jsp
  三种进入方式：
    1) web.xml 的 <error-page> 配置（404 / 500 / 任何异常）
    2) AuthFilter 检测到越权时 forward 过来（403）
    3) 各个 Servlet 捕获 SQLException 后 forward 过来
  安全要求：只显示友好提示，绝不把异常堆栈直接输出到页面
  ============================================================
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<%-- 容器传进来的错误信息（只有在由 <error-page> 触发时才存在） --%>
<c:set var="statusCode" value="${requestScope['javax.servlet.error.status_code']}" />
<c:set var="containerMessage" value="${requestScope['javax.servlet.error.message']}" />
<%-- 业务代码主动放入的错误信息 --%>
<%-- requestScope.errorMessage 和 errorHint 由 Servlet / Filter 设置 --%>

<c:set var="displayCode" value="${empty statusCode ? '500' : statusCode}" />

<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>出错了 - 班级学生成绩管理系统</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body class="error-page">

<div class="error-box">
    <div class="code">${displayCode}</div>

    <h1>
        <c:choose>
            <c:when test="${displayCode == 403}">无权限访问</c:when>
            <c:when test="${displayCode == 404}">页面不存在</c:when>
            <c:otherwise>操作未能完成</c:otherwise>
        </c:choose>
    </h1>

    <div class="detail">
        <c:choose>
            <%-- 优先显示业务代码给出的具体原因 --%>
            <c:when test="${not empty requestScope.errorMessage}">
                <c:out value="${requestScope.errorMessage}" escapeXml="false" />
            </c:when>
            <%-- 其次显示容器给出的信息 --%>
            <c:when test="${not empty containerMessage}">
                <c:out value="${containerMessage}" />
            </c:when>
            <%-- 都没有就给一句通用提示 --%>
            <c:otherwise>
                服务器处理请求时发生了异常，请稍后重试。
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${not empty requestScope.errorHint}">
        <p class="hint"><c:out value="${requestScope.errorHint}" /></p>
    </c:if>

    <p class="hint">
        如果问题持续出现，请检查：MySQL 服务是否启动、数据库 score_management 是否已创建、
        db.properties 中的账号密码是否正确。详细异常已记录在 Tomcat 的日志文件中。
    </p>

    <div class="actions">
        <a class="btn btn-primary" href="${ctx}/login.jsp">返回登录页</a>
        <a class="btn" href="javascript:history.back()">返回上一页</a>
    </div>
</div>

</body>
</html>
