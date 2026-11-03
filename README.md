# 班级学生成绩管理系统

> **JavaEE 期末大作业** · 原生 MVC 分层 · **不使用 Spring / Spring Boot / SSM / MyBatis**
>
> Maven + Servlet + JSP + JDBC + MySQL + Tomcat，IDEA 开发

项目地址：<https://github.com/afd11123/java-student-manage-system>

---

## 一分钟了解怎么部署

```bash
# 1. 导入数据库（建库 + 建表 + 演示数据）
mysql -u root -p < sql/score_management.sql

# 2. 改成你自己的数据库密码
#    src/main/resources/db.properties  ->  jdbc.password=你的密码

# 3. 打包
mvn clean package

# 4. 把 target/score-management.war 丢进 Tomcat 的 webapps/，启动 Tomcat
#    浏览器打开 http://localhost:8080/score-management/
```

演示账号：教师 `teacher / 123456`，学生 `20230001 / 123456`。

详细步骤（IDEA 社区版怎么跑、Smart Tomcat 插件、离线脚本、Linux 服务器部署）见 [第四章](#四5-分钟跑起来)。

---

## 目录

- [一、功能清单](#一功能清单)
- [二、运行环境](#二运行环境)
- [三、目录结构](#三目录结构)
- [四、5 分钟跑起来（部署）](#四5-分钟跑起来)
- [五、答辩演示脚本](#五答辩演示脚本按这个顺序讲5-分钟讲完)
- [六、设计说明](#六设计说明老师常问的-6-个问题)
- [七、常见问题排查](#七常见问题排查)
- [八、上传到 GitHub](#八上传到-github)
- [九、已知限制与可扩展方向](#九已知限制与可扩展方向)

---

## 一、功能清单

| 角色 | 功能 | 说明 |
|---|---|---|
| 教师 | 登录 | 账号密码校验，Session 保存登录态 |
| 教师 | 查看全部成绩 | 联表显示学生姓名、科目、分数、日期、是否及格 |
| 教师 | 新增成绩 | 学生下拉选择 + 科目 + 分数 + 日期 |
| 教师 | 修改成绩 | 点"编辑"复用同一张表单回填 |
| 教师 | 删除成绩 | 二次确认后删除 |
| 教师 | 按科目统计 | 平均分、及格人数、**及格率**（60 分及格） |
| 学生 | 登录 | 同上 |
| 学生 | 查看本人成绩 | **只能看到自己**，改动 URL 也看不到别人的 |
| 学生 | 个人汇总 | 平均分、及格科目数、不及格科目数、及格率 |
| 公共 | 退出登录 | 销毁 Session |
| 公共 | 权限控制 | 未登录跳登录页；跨角色访问返回 403 |

**统计逻辑全部写在 Java 代码里**（`TeacherScoreServlet`、`StudentScoreServlet`），SQL 只用 `SELECT` 查明细，**没有使用任何 `COUNT/AVG/SUM` 等聚合函数**，符合题目要求。

---

## 二、运行环境

本项目在以下环境实测通过：

| 组件 | 版本 | 备注 |
|---|---|---|
| JDK | **1.8.0_261** | 必须用 JDK 8，Tomcat 8.5 不支持高版本 JDK |
| Tomcat | **8.5.0** | 对应 Servlet 3.1，包名 `javax.servlet` |
| MySQL | **8.0.45** | 驱动 `com.mysql.cj.jdbc.Driver` |
| Maven | 3.9.9（IDEA 自带） | 也可不用 Maven，见"方式 C" |
| IDEA | Community 2025.2 | **社区版没有 Tomcat 集成**，见下方说明 |

> ⚠️ **IDEA 社区版重要提示**：Tomcat 运行配置是 IDEA **Ultimate 版专属功能**。
> 社区版请按下面"方式 A"或"方式 B"运行，不要去找 `Run → Edit Configurations → Tomcat Server`，那里没有。

---

## 三、目录结构

```
javaee/
├── pom.xml                          Maven 配置（war 打包，JDK 8）
├── README.md                        本文件
├── .gitignore
├── sql/
│   └── score_management.sql         建库建表 + 演示数据
├── scripts/
│   ├── build-offline.bat            不依赖 Maven 的一键打包脚本
│   └── deploy-to-tomcat.bat         一键部署到 Tomcat 并启动
└── src/main/
    ├── java/com/score/
    │   ├── entity/                  实体层
    │   │   ├── User.java            用户（教师/学生共用）
    │   │   ├── Score.java           成绩
    │   │   └── SubjectStat.java     科目统计结果（供页面渲染）
    │   ├── util/
    │   │   └── DBUtil.java          JDBC 工具类（读配置、取连接、关资源）
    │   ├── dao/                     数据访问层（唯一写 SQL 的地方）
    │   │   ├── UserDao.java
    │   │   └── ScoreDao.java
    │   ├── filter/                  过滤器
    │   │   ├── EncodingFilter.java  统一 UTF-8，解决中文乱码
    │   │   └── AuthFilter.java      登录校验 + 角色校验
    │   └── servlet/                 控制层
    │       ├── LoginServlet.java
    │       ├── LogoutServlet.java
    │       ├── TeacherScoreServlet.java
    │       └── StudentScoreServlet.java
    ├── resources/
    │   └── db.properties            数据库连接配置（改这里！）
    └── webapp/
        ├── WEB-INF/web.xml          过滤器/Servlet/错误页 集中配置
        ├── login.jsp
        ├── teacherIndex.jsp
        ├── studentIndex.jsp
        ├── error.jsp
        └── css/style.css
```

---

## 四、5 分钟跑起来

### 步骤 1：导入数据库

用 Navicat / SQLyog / 命令行任选其一，执行 `sql/score_management.sql`：

```bash
mysql -u root -p < sql/score_management.sql
```

执行完会得到数据库 `score_management`，两张表 `user`、`score`，以及演示数据（4 个账号、9 条成绩）。

### 步骤 2：改数据库密码（**必做**）

打开 `src/main/resources/db.properties`，把用户名密码改成你自己的：

```properties
jdbc.username=root
jdbc.password=你的MySQL密码
```

> 这一步不做，登录时会提示"连接数据库失败：Access denied"。

### 步骤 3：用 IDEA 打开项目

1. `File → Open`，选中 `javaee` 这个文件夹（里面有 `pom.xml` 的那层）
2. 右下角提示 `Maven project detected`，点 **Load / 导入**
3. `File → Project Structure → Project SDK`，选择 **1.8**（`jdk1.8.0_261`）
   - 如果没有 1.8 选项，点 `Add SDK → JDK`，选 `C:\Program Files\Java\jdk1.8.0_261`
4. `Project language level` 选 **8**

### 步骤 4：运行（三选一）

#### 方式 A：打包 WAR + 手动部署（最稳，推荐新手）

在 IDEA 右侧 `Maven` 面板双击 `Lifecycle → package`，或终端执行：

```bash
mvn clean package
```

生成 `target/score-management.war`，然后：

1. 把 `score-management.war` 复制到 `D:\apache-tomcat-8.5.0-windows-x64\apache-tomcat-8.5.0\webapps\`
2. 双击 `bin\startup.bat` 启动 Tomcat
3. 浏览器访问 <http://localhost:8080/score-management/>

> 也可以直接双击 `scripts\build-offline.bat`（不联网也能打包），再双击 `scripts\deploy-to-tomcat.bat`（自动复制 + 重启 Tomcat + 打开浏览器）。
> **注意**：这两个 bat 开头有 `JAVA_HOME` 和 `TOMCAT_HOME` 两个路径，如果和你机器不一致要先改。

#### 方式 B：Smart Tomcat 插件（体验最好，需能联网）

1. `File → Settings → Plugins`，搜索 **Smart Tomcat**，安装并重启 IDEA
2. `Run → Edit Configurations → + → Smart Tomcat`
3. 配置：
   - `Tomcat Server`：选到 Tomcat 根目录（`...\apache-tomcat-8.5.0`）
   - `Deployment`：选 `src/main/webapp`
   - `Context Path`：`/score-management`
4. 点绿色三角运行，访问 <http://localhost:8080/score-management/>

#### 方式 C：离线脚本（没有网络、Maven 下载不了依赖时）

```bash
scripts\build-offline.bat
scripts\deploy-to-tomcat.bat
```

脚本用 `javac` 直接编译，从本地 `.m2` 仓库复制 `mysql-connector-j` 和 `jstl` 到 `WEB-INF/lib`，再用 `jar` 打包。

### 步骤 5：登录

访问 <http://localhost:8080/score-management/>

| 账号 | 密码 | 角色 |
|---|---|---|
| `teacher` | `123456` | 教师 |
| `20230001` | `123456` | 学生（李小明） |
| `20230002` | `123456` | 学生（王小华） |
| `20230003` | `123456` | 学生（赵小雨） |

### 步骤 6：部署到 Linux 服务器（可选，方便给老师远程看）

步骤和本地完全一样，只是路径不同：

```bash
# 1. 装 JDK 8 和 Tomcat 8.5（以 CentOS 为例，Ubuntu 用 apt）
sudo yum install -y java-1.8.0-openjdk
wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.100/bin/apache-tomcat-8.5.100.tar.gz
tar -zxf apache-tomcat-8.5.100.tar.gz -C /opt

# 2. 装 MySQL，并用同一条命令导入脚本
mysql -u root -p < sql/score_management.sql

# 3. 在本地打包后上传 WAR
scp target/score-management.war root@你的服务器IP:/opt/apache-tomcat-8.5.100/webapps/

# 4. 启动 Tomcat
/opt/apache-tomcat-8.5.100/bin/startup.sh

# 5. 访问 http://你的服务器IP:8080/score-management/
```

> 阿里云 / 腾讯云需要在**安全组**里放行 8080 端口，否则外网打不开。

### 部署成功自检清单

| 检查项 | 预期结果 |
|---|---|
| 打开 `/score-management/` | 显示登录页，蓝色渐变背景（说明 CSS 生效） |
| `teacher / 123456` 登录 | 进入教师端，3 个科目的及格率分别是 100.00% / 66.67% / 33.33% |
| 新增一条 120 分的成绩 | 被拦下，提示"分数必须在 0 ~ 100 之间" |
| 新增一条 88 分的成绩 | 提示"新增成功"，统计表格数字同步变化 |
| `20230001 / 123456` 登录 | 只看到李小明自己的 3 条成绩 |
| 学生身份访问 `/teacher/score` | 返回 403 错误页（权限生效） |

---

## 五、答辩演示脚本（按这个顺序讲，5 分钟讲完）

1. **登录页**：说明这是唯一不用登录的页面，账号密码走 POST，不出现在地址栏
2. **教师登录** → 展示数据卡片、按科目统计表格
   - 指着三个科目说：`Java程序设计 100%`、`数据库原理 66.67%`、`高等数学 33.33%`，数据是刻意设计的
   - **重点讲**：这个及格率不是 SQL 算的，是 `TeacherScoreServlet.buildStats()` 里 for 循环累加出来的
3. **新增成绩**：选学生、填科目、填一个 `120` 分 → 演示后端校验拦下"分数必须在 0~100 之间"
4. **新增一条合法成绩** → 提示"新增成功"，统计数字自动变化（PRG 模式，刷新不会重复提交）
5. **编辑**：点编辑，表单回填 → 改分数 → 保存
6. **删除**：点删除，弹出确认框 → 确定 → 列表少一条
7. **退出，学生登录** → 只能看到自己的 3 条成绩
8. **越权演示（关键）**：
   - 地址栏输入 `http://localhost:8080/score-management/teacherIndex.jsp` → 被踢回登录页
   - 地址栏输入 `http://localhost:8080/score-management/teacher/score` → 被踢回登录页
   - 再讲：即使登录着学生账号，`/student/score?studentId=3` 也不会查出别人的数据，因为 Servlet 只认 Session 里的 id
9. **退出**：Session 销毁，后退键也看不到旧页面

---

## 六、设计说明（老师常问的 6 个问题）

### 1. 为什么没有 Service 层？

题目要求的包结构是 `entity / util / dao / servlet` 四层，没有 service。
这个规模下业务逻辑就是"校验 + 调 DAO"，加一层只会让代码多跳一次。
如果将来要加事务、要复用到别的入口，在 `com.score.service` 下加一层即可，现有代码不用改。

### 2. 为什么统计不写在 SQL 里？

题目明确要求"统计逻辑写在 Java 代码，不用 SQL 聚合函数"。
好处：逻辑集中在一处、可读、可单测、换数据库时不用改；
代价：数据量大时要把全部记录读到内存。
`ScoreDao` 里所有 SQL 都只有 `SELECT ... WHERE ... ORDER BY`，没有任何 `COUNT/AVG/SUM`。

### 3. 学生怎么保证看不到别人的成绩？

**两层防护**：

- `AuthFilter`：拦截 `/student*` 的 URL，非学生的 Session 直接 403
- `StudentScoreServlet`：查询条件**只取 Session 里的 `loginUser.getId()`**，`request.getParameter("studentId")` 根本不存在于代码里，传什么参数都没用

这就是"永远不要相信前端传来的身份标识"。

### 4. 中文乱码怎么解决的？

三处一起治，缺一处都可能出问题：

| 位置 | 做法 |
|---|---|
| 请求/响应 | `EncodingFilter` 设 `UTF-8`（web.xml 里映射 `/*`，排第一个） |
| 页面 | 每个 JSP 的 `pageEncoding="UTF-8"` |
| 数据库 | JDBC url 加 `characterEncoding=UTF-8`，库表用 `utf8mb4` |

### 5. 为什么保存后是"重定向"而不是"转发"？

保存成功后 `sendRedirect` 回列表页，这叫 **PRG（Post-Redirect-Get）**。
如果是转发，用户按 F5 会把表单**再提交一次**，造成重复数据。
提示信息通过 Session 里的 Flash 消息带过去，显示一次就删掉。

### 6. 密码明文有什么问题？

课程作业为方便演示用了明文。真实项目必须改成加盐哈希（BCrypt/Argon2），
并且登录失败不能区分"用户不存在"和"密码错误"，否则可被用来枚举账号。
本项目的错误提示已经统一成"账号或密码错误"。

---

## 七、常见问题排查

| 现象 | 原因 | 解决 |
|---|---|---|
| 登录提示 `Access denied for user 'root'` | `db.properties` 密码不对 | 改成你自己的 MySQL 密码 |
| 登录提示 `Unknown database 'score_management'` | 没执行建表脚本 | 执行 `sql/score_management.sql` |
| 登录提示 `数据库驱动 ... 加载失败` | 缺驱动 jar | Maven 重新导入，或把 `mysql-connector-j-8.0.33.jar` 放进 `WEB-INF/lib` |
| 页面中文变问号 | 编码三处没对齐 | 检查 Filter、JSP pageEncoding、JDBC url |
| 页面报 `javax.servlet` 找不到 | 用了 JDK 17+ 或 Tomcat 10+ | 必须 JDK 8 + Tomcat 8.5（Tomcat 10 起包名改成 `jakarta.servlet`） |
| 启动报 `Port 8080 already in use` | 端口被占用 | 关掉占用进程，或改 `conf/server.xml` 的 Connector port |
| 访问 404 | 部署名不对 | 访问路径是 `/score-management/`，和 `pom.xml` 的 `finalName` 一致 |
| 学生访问教师页返回 403 | 这是**正确行为** | 权限拦截生效了 |
| `mvn package` 卡在下载依赖 | 校园网/无外网 | 用方式 C 离线脚本，或在 `settings.xml` 配阿里云镜像 |

---

## 八、上传到 GitHub

仓库地址：<https://github.com/afd11123/java-student-manage-system>

### 方式一：网页上传（不用装 git，最省事）

1. 打开仓库页面 → `Add file` → `Upload files`
2. 把**项目文件夹里的内容**拖进去：
   - `pom.xml`、`README.md`、`.gitignore`
   - `sql/` 整个文件夹
   - `src/` 整个文件夹
   - `scripts/` 整个文件夹
3. **不要**上传 `target/`、`.idea/`、`.tomcat-base/`（`.gitignore` 已经排除了，但网页拖拽会带上，注意别拖）
4. 下方写提交说明，点 `Commit changes`

### 方式二：命令行（推荐，以后好更新）

先装 Git：<https://git-scm.com/download/win>，装完重启 IDEA。

```bash
cd "D:\桌面\javaEE"

git init
git add .
git commit -m "feat: JavaEE 班级学生成绩管理系统（Servlet+JSP+JDBC）"

git branch -M main
git remote add origin https://github.com/afd11123/java-student-manage-system.git
git push -u origin main
```

如果仓库里已有 README 导致冲突，先拉一下：

```bash
git pull origin main --allow-unrelated-histories
# 解决冲突后再 git push -u origin main
```

推送时会要求登录：用 **Personal Access Token** 当密码（GitHub 已不支持账号密码推送），
在 `Settings → Developer settings → Personal access tokens → Tokens (classic)` 生成，勾选 `repo` 权限。

### 方式三：从 GitHub 克隆到另一台电脑

```bash
git clone https://github.com/afd11123/java-student-manage-system.git
cd java-student-manage-system

# 然后重复"步骤 1 导入数据库"和"步骤 2 改密码"
mvn clean package
# 把 target/score-management.war 放进本机 Tomcat 的 webapps/
```

> ⚠️ 克隆下来的是**源码**，不是能直接双击运行的成品。
> 必须经过"导数据库 → 改配置 → 打包 → 放进 Tomcat"这几步，
> 原因见 [第四章](#四5-分钟跑起来)。

### 上传前的三个检查

1. `target/`、`.idea/`、`.tomcat-base/` **不要提交**（`.gitignore` 已排除，网页拖拽时别拖进去）
2. `db.properties` 里是你的真实数据库密码，**公开仓库建议改成占位符** `123456`
3. `README.md` 就是仓库首页，改完记得一起提交

---

## 九、已知限制与可扩展方向

| 限制 | 说明 |
|---|---|
| 密码明文 | 演示方便；生产改 BCrypt |
| 无分页 | 成绩多了会一次性查出；可加 `LIMIT` 分页 |
| 无事务 | 单表操作，暂不需要；将来批量录入要加 `conn.setAutoCommit(false)` |
| 科目是自由文本 | 会出现"Java程序设计"和"Java 程序设计"被当成两个科目；可加 `subject` 字典表 |
| 无操作日志 | 谁改了什么查不到；可加 `operate_log` 表 |

---

## 十、许可

仅用于课程学习与交流。
