<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%--
  ============================================================
  学生端 studentIndex.jsp
  只读页面：展示当前登录学生本人的成绩和个人统计
  数据由 StudentScoreServlet 只按 Session 中的 id 查出，
  页面没有任何"按 id 查询"的入口，从根本上杜绝越权
  ============================================================
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${sessionScope.loginUser}" />
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>我的成绩 - 班级学生成绩管理系统</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>

<%-- ================= 顶部栏 ================= --%>
<header class="topbar">
    <div class="brand">班级学生成绩管理系统 · 学生端</div>
    <div class="user-box">
        <span class="badge">学生</span>
        <strong><c:out value="${me.realName}" /></strong>
        <span class="muted">（学号 <c:out value="${me.username}" />）</span>
        <a class="btn btn-ghost btn-sm" href="${ctx}/logout">退出登录</a>
    </div>
</header>

<main class="container">

    <%-- 提示条 --%>
    <c:if test="${not empty requestScope.errorMessage}">
        <div class="alert alert-danger"><c:out value="${requestScope.errorMessage}" /></div>
    </c:if>

    <%-- ================= 个人统计卡片 ================= --%>
    <section class="cards">
        <div class="card">
            <div class="card-label">已出成绩科目数</div>
            <div class="card-value"><c:out value="${requestScope.totalCount}" default="0" /></div>
        </div>
        <div class="card">
            <div class="card-label">平均分</div>
            <div class="card-value">${requestScope.averageText}</div>
        </div>
        <div class="card">
            <div class="card-label">及格科目</div>
            <div class="card-value score-pass"><c:out value="${requestScope.passCount}" default="0" /></div>
        </div>
        <div class="card">
            <div class="card-label">不及格科目</div>
            <div class="card-value score-fail"><c:out value="${requestScope.failCount}" default="0" /></div>
        </div>
    </section>

    <%-- ================= 我的成绩列表 ================= --%>
    <section class="section">
        <h2>
            我的成绩
            <span class="muted" style="font-weight:normal;font-size:12px;color:#8494a8;">
                （及格率 ${requestScope.passRateText}%）
            </span>
        </h2>
        <table class="table">
            <thead>
            <tr>
                <th>序号</th>
                <th>科目</th>
                <th>分数</th>
                <th>考试日期</th>
                <th>结果</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${requestScope.scores}" varStatus="st">
                <tr>
                    <td>${st.count}</td>
                    <td><c:out value="${s.subject}" /></td>
                    <td class="${s.pass ? 'score-pass' : 'score-fail'}">
                        ${s.scoreText}
                    </td>
                    <td>${s.examDate}</td>
                    <td>
                        <span class="tag ${s.pass ? 'ok' : 'warn'}">${s.pass ? '及格' : '不及格'}</span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty requestScope.scores}">
                <tr>
                    <td colspan="5" class="empty">暂无成绩记录，请联系任课教师录入</td>
                </tr>
            </c:if>
            </tbody>
        </table>

        <c:if test="${not empty requestScope.scores}">
            <p style="margin-top:14px;color:#8494a8;font-size:12px;">
                说明：本页只显示当前登录学生本人的成绩，每 60 分为及格线。
            </p>
        </c:if>
    </section>

</main>

</body>
</html>
