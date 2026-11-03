package com.score.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * JDBC 工具类：整个项目里唯一直接接触 DriverManager 的地方。
 * <p>
 * 职责只有三件事：
 * <ol>
 *     <li>从 classpath 读取 db.properties，加载 MySQL 驱动（只做一次，用 synchronized + 标志位保证）</li>
 *     <li>对外提供 {@link #getConnection()} 获取连接</li>
 *     <li>提供统一的 close 方法，把 ResultSet / Statement / Connection 按顺序安全关闭</li>
 * </ol>
 * 工具类不允许被实例化，所以类声明为 final、构造方法私有。
 * </p>
 *
 * @author 课程设计
 */
public final class DBUtil {

    /** 配置文件在 classpath 下的名字（实际位于 src/main/resources/db.properties） */
    private static final String CONFIG_FILE = "db.properties";

    /** 数据库连接地址 */
    private static String url;

    /** 数据库账号 */
    private static String username;

    /** 数据库密码 */
    private static String password;

    /** 是否已经完成初始化，避免每次获取连接都重新读配置、重新加载驱动 */
    private static boolean initialized = false;

    /** 私有构造方法：禁止 new DBUtil() */
    private DBUtil() {
    }

    /**
     * 初始化：读取配置 + 加载驱动。
     * <p>
     * 用 synchronized 修饰是为了保证多线程（多个请求）同时到达时只初始化一次。
     * 初始化失败时抛 SQLException，由上层 Servlet 捕获后展示友好的错误页面，
     * 而不是让整个类初始化失败导致 NoClassDefFoundError。
     * </p>
     *
     * @throws SQLException 配置文件缺失、读取失败或驱动类找不到时抛出
     */
    private static synchronized void init() throws SQLException {
        if (initialized) {
            return;
        }

        Properties props = new Properties();
        // try-with-resources 自动关闭输入流
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new SQLException("在 classpath 下找不到数据库配置文件 " + CONFIG_FILE
                        + "，请确认文件位于 src/main/resources 目录，并已重新构建项目。");
            }
            // 显式指定 UTF-8 读取，避免配置文件里的中文注释告警
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SQLException("读取数据库配置文件 " + CONFIG_FILE + " 失败：" + e.getMessage(), e);
        }

        // 逐项取配置，缺项时给出明确提示，方便排错
        String driver = getRequired(props, "jdbc.driver");
        url = getRequired(props, "jdbc.url");
        username = getRequired(props, "jdbc.username");
        // 密码允许为空字符串（某些本地数据库账号没有密码），所以不用 getRequired
        password = props.getProperty("jdbc.password", "").trim();

        try {
            // MySQL 8.x 驱动类：com.mysql.cj.jdbc.Driver
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("数据库驱动 " + driver + " 加载失败："
                    + "请检查 pom.xml 是否引入 mysql-connector-j，"
                    + "以及 WEB-INF/lib 下是否存在该驱动 jar。", e);
        }

        initialized = true;
    }

    /**
     * 从配置中读取必填项，缺失则抛异常。
     *
     * @param props 已加载的配置
     * @param key   键名
     * @return 去除首尾空格后的值
     * @throws SQLException 配置缺失或为空时抛出
     */
    private static String getRequired(Properties props, String key) throws SQLException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("数据库配置文件 " + CONFIG_FILE + " 中缺少必填项：" + key);
        }
        return value.trim();
    }

    /**
     * 获取一个数据库连接。
     * <p>调用方必须负责关闭，推荐使用 try-with-resources 语法。</p>
     *
     * @return 数据库连接
     * @throws SQLException 初始化失败或连接失败时抛出
     */
    public static Connection getConnection() throws SQLException {
        init();
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            // 把常见故障翻译成看得懂的提示，答辩演示时排错非常有用
            throw new SQLException("连接数据库失败（" + url + "）：" + e.getMessage()
                    + "。请检查 MySQL 服务是否启动、库 score_management 是否已创建、账号密码是否正确。", e);
        }
    }

    /**
     * 按 ResultSet → Statement → Connection 的顺序关闭三个资源。
     * 关闭顺序必须与打开顺序相反，否则可能抛异常。
     *
     * @param rs   结果集，可为 null
     * @param stmt 语句对象，可为 null
     * @param conn 连接，可为 null
     */
    public static void close(ResultSet rs, Statement stmt, Connection conn) {
        closeQuietly(rs);
        closeQuietly(stmt);
        closeQuietly(conn);
    }

    /**
     * 只关闭连接（查询条件用 try-with-resources 时可以只关连接）。
     *
     * @param conn 连接，可为 null
     */
    public static void close(Connection conn) {
        closeQuietly(conn);
    }

    /**
     * 静默关闭：关闭失败只忽略，不抛出异常，
     * 否则 finally 里抛异常会覆盖掉真正的业务异常，排查问题更难。
     *
     * @param closeable 任意可关闭资源
     */
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // 关闭资源失败不影响主流程，这里刻意忽略
            }
        }
    }
}
