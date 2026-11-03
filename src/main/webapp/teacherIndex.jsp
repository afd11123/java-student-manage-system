<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%--
  ============================================================
  教师后台 teacherIndex.jsp
  一个页面承担教师端全部功能：
    1) 顶部数据卡片
    2) 按科目统计表格（数据由 TeacherScoreServlet 用 Java 计算后放入 request）
    3) 新增 / 编辑 共用的成绩表单
    4) 全部成绩列表（含编辑、删除）
  本页由 /teacher/score 转发进入，AuthFilter 与 Servlet 双重校验教师身份
  ============================================================
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="me" value="${sessionScope.loginUser}" />
<%-- formScore 同时用于"编辑回填"和"校验失败后回填"，页面只认这一个变量 --%>
<c:set var="f" value="${requestScope.formScore}" />
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>教师后台 - 班级学生成绩管理系统</title>
    <link rel="stylesheet" href="${ctx}/css/style.css">
</head>
<body>

<%-- ================= 顶部栏 ================= --%>
<header class="topbar">
    <div class="brand">班级学生成绩管理系统 · 教师端</div>
    <div class="user-box">
        <span class="badge">教师</span>
        <strong><c:out value="${me.realName}" /></strong>
        <span class="muted">（<c:out value="${me.username}" />）</span>
        <a class="btn btn-ghost btn-sm" href="${ctx}/logout">退出登录</a>
    </div>
</header>

<main class="container">

    <%-- ================= 一次性提示信息（Flash） =================
         由 TeacherScoreServlet 写入 Session，显示一次后立即删除，
         这样刷新页面不会重复弹出 --%>
    <c:if test="${not empty sessionScope.flashMessage}">
        <div class="alert alert-${sessionScope.flashType}">
            <c:out value="${sessionScope.flashMessage}" />
        </div>
        <c:remove var="flashMessage" scope="session" />
        <c:remove var="flashType" scope="session" />
    </c:if>

    <%-- 表单校验失败提示（保存在 request 中，只在本次请求有效） --%>
    <c:if test="${not empty requestScope.errorMessage}">
        <div class="alert alert-danger">
            <c:out value="${requestScope.errorMessage}" />
        </div>
    </c:if>

    <%-- ================= 数据卡片 ================= --%>
    <section class="cards">
        <div class="card">
            <div class="card-label">成绩记录总数</div>
            <div class="card-value"><c:out value="${requestScope.totalCount}" default="0" /></div>
        </div>
        <div class="card">
            <div class="card-label">及格记录数（≥60 分）</div>
            <div class="card-value"><c:out value="${requestScope.passTotal}" default="0" /></div>
        </div>
        <div class="card">
            <div class="card-label">科目数量</div>
            <div class="card-value">${fn:length(requestScope.stats)}</div>
        </div>
        <div class="card">
            <div class="card-label">学生人数</div>
            <div class="card-value">${fn:length(requestScope.students)}</div>
        </div>
    </section>

    <%-- ================= 按科目统计 ================= --%>
    <section class="section">
        <h2>按科目统计（平均分 / 及格率，统计逻辑由 Java 代码计算）</h2>
        <table class="table">
            <thead>
            <tr>
                <th>科目</th>
                <th>参考人数</th>
                <th>平均分</th>
                <th>及格人数</th>
                <th>及格率</th>
                <th>最高分</th>
                <th>最低分</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="stat" items="${requestScope.stats}">
                <tr>
                    <td><strong><c:out value="${stat.subject}" /></strong></td>
                    <td>${stat.count}</td>
                    <td>${stat.averageText}</td>
                    <td>${stat.passCount}</td>
                    <td>
                        <span class="tag ${stat.passRate >= 60 ? 'ok' : 'warn'}">
                            ${stat.passRateText}%
                        </span>
                    </td>
                    <td>${stat.maxScoreText}</td>
                    <td>${stat.minScoreText}</td>
                </tr>
            </c:forEach>
            <c:if test="${empty requestScope.stats}">
                <tr>
                    <td colspan="7" class="empty">暂无成绩数据，请先在下方新增一条成绩</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </section>

    <%-- ================= 新增 / 编辑 表单 ================= --%>
    <section class="section">
        <h2>
            <c:choose>
                <c:when test="${empty f}">新增成绩</c:when>
                <c:otherwise>修改成绩（编号 ${f.id}）</c:otherwise>
            </c:choose>
        </h2>

        <form class="score-form" action="${ctx}/teacher/score" method="post">
            <%-- action=save 告诉 Servlet 这是保存操作；有 id 就是修改，没有就是新增 --%>
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="id" value="${f.id}">

            <div class="form-row">
                <div class="field">
                    <label for="studentId">学生</label>
                    <select id="studentId" name="studentId" required>
                        <option value="">-- 请选择学生 --</option>
                        <c:forEach var="stu" items="${requestScope.students}">
                            <option value="${stu.id}" ${f.studentId == stu.id ? 'selected' : ''}>
                                <c:out value="${stu.username}" /> <c:out value="${stu.realName}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="field">
                    <label for="subject">科目</label>
                    <input type="text" id="subject" name="subject" maxlength="100"
                           placeholder="如：Java程序设计"
                           value="<c:out value='${f.subject}'/>" required>
                </div>

                <div class="field">
                    <label for="score">分数（0~100）</label>
                    <%-- 前端第一道校验：type=number + min/max + step，后端还会再校验一次 --%>
                    <input type="number" id="score" name="score" min="0" max="100" step="0.01"
                           placeholder="0 ~ 100" value="${f.score}" required>
                </div>

                <div class="field">
                    <label for="examDate">考试日期</label>
                    <input type="date" id="examDate" name="examDate"
                           value="${f.examDate}" required>
                </div>

                <div class="field field-actions">
                    <button type="submit" class="btn btn-primary">
                        <c:choose>
                            <c:when test="${empty f}">新增成绩</c:when>
                            <c:otherwise>保存修改</c:otherwise>
                        </c:choose>
                    </button>
                    <c:if test="${not empty f}">
                        <a class="btn" href="${ctx}/teacher/score?action=list">取消编辑</a>
                    </c:if>
                </div>
            </div>
        </form>
    </section>

    <%-- ================= 全部成绩列表 ================= --%>
    <section class="section">
        <h2>全部成绩（共 ${fn:length(requestScope.scores)} 条）</h2>
        <table class="table">
            <thead>
            <tr>
                <th>编号</th>
                <th>学生</th>
                <th>科目</th>
                <th>分数</th>
                <th>考试日期</th>
                <th>是否及格</th>
                <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="s" items="${requestScope.scores}">
                <tr>
                    <td>${s.id}</td>
                    <%-- 所有来自数据库的文本都用 c:out 输出，它会做 HTML 转义，防止 XSS --%>
                    <td><c:out value="${s.studentName}" /></td>
                    <td><c:out value="${s.subject}" /></td>
                    <td class="${s.pass ? 'score-pass' : 'score-fail'}">
                        ${s.scoreText}
                    </td>
                    <td>${s.examDate}</td>
                    <td>
                        <span class="tag ${s.pass ? 'ok' : 'warn'}">${s.pass ? '及格' : '不及格'}</span>
                    </td>
                    <td class="ops">
                        <a class="btn btn-sm" href="${ctx}/teacher/score?action=edit&id=${s.id}">编辑</a>
                        <%-- 删除必须用 POST：GET 请求不应该修改数据，否则可能被浏览器预加载误触发 --%>
                        <form class="inline-form" action="${ctx}/teacher/score" method="post"
                              onsubmit="return confirm('确定要删除该学生的这条成绩吗？删除后无法恢复。');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${s.id}">
                            <button type="submit" class="btn btn-sm btn-danger">删除</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty requestScope.scores}">
                <tr>
                    <td colspan="7" class="empty">还没有任何成绩记录</td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </section>

</main>

</body>
</html>
