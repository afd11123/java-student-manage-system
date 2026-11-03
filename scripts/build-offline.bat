@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

rem ============================================================
rem  一键打包脚本（不需要 Maven，只要有 JDK 8 + Tomcat 即可）
rem  用途：当 IDEA 里 Maven 下载依赖失败（校园网/无外网）时，
rem        用这个脚本把项目编译并打成 score-management.war
rem
rem  使用前请确认下面三个路径与你机器一致：
rem ============================================================

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261"
set "TOMCAT_HOME=D:\apache-tomcat-8.5.0-windows-x64\apache-tomcat-8.5.0"
set "M2=%USERPROFILE%\.m2\repository"

set "PROJECT_DIR=%~dp0.."
set "SRC=%PROJECT_DIR%\src\main\java"
set "WEBAPP=%PROJECT_DIR%\src\main\webapp"
set "RES=%PROJECT_DIR%\src\main\resources"
set "TARGET=%PROJECT_DIR%\target"
set "STAGE=%TARGET%\score-management"

echo ============================================================
echo  1/4 清理旧产物
echo ============================================================
if exist "%STAGE%" rmdir /s /q "%STAGE%"
if exist "%TARGET%\classes" rmdir /s /q "%TARGET%\classes"
if exist "%TARGET%\score-management.war" del /q "%TARGET%\score-management.war"
mkdir "%TARGET%\classes" 2>nul

echo ============================================================
echo  2/4 编译 Java 源码（UTF-8，目标 JDK 8）
echo ============================================================
dir /s /b "%SRC%\*.java" > "%TARGET%\sources.txt"
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -source 1.8 -target 1.8 ^
    -cp "%TOMCAT_HOME%\lib\servlet-api.jar" ^
    -d "%TARGET%\classes" @"%TARGET%\sources.txt"
if errorlevel 1 (
    echo [失败] 编译出错，请检查上面的错误信息
    goto :fail
)
echo [成功] 编译完成

echo ============================================================
echo  3/4 组装 Web 应用目录
echo ============================================================
xcopy "%WEBAPP%" "%STAGE%" /E /I /Y /Q >nul
mkdir "%STAGE%\WEB-INF\classes" 2>nul
mkdir "%STAGE%\WEB-INF\lib" 2>nul
xcopy "%TARGET%\classes" "%STAGE%\WEB-INF\classes" /E /I /Y /Q >nul
copy /y "%RES%\db.properties" "%STAGE%\WEB-INF\classes\" >nul

rem 从本地 Maven 仓库复制运行期依赖（如果找不到，请手动把这两个 jar 放进来）
copy /y "%M2%\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" "%STAGE%\WEB-INF\lib\" >nul 2>nul
if not exist "%STAGE%\WEB-INF\lib\mysql-connector-j-8.0.33.jar" echo [警告] 未找到 MySQL 驱动 jar，请手动复制到 WEB-INF\lib
copy /y "%M2%\javax\servlet\jstl\1.2\jstl-1.2.jar" "%STAGE%\WEB-INF\lib\" >nul 2>nul
if not exist "%STAGE%\WEB-INF\lib\jstl-1.2.jar" echo [警告] 未找到 JSTL jar，请手动复制到 WEB-INF\lib

echo ============================================================
echo  4/4 打包 WAR
echo ============================================================
pushd "%STAGE%"
"%JAVA_HOME%\bin\jar.exe" -cf "%TARGET%\score-management.war" .
popd
if errorlevel 1 (
    echo [失败] 打包失败
    goto :fail
)

echo.
echo [成功] 已生成：%TARGET%\score-management.war
echo 下一步：把该文件复制到 %TOMCAT_HOME%\webapps\ 下，然后运行 bin\startup.bat
echo.
pause
exit /b 0

:fail
echo.
echo 构建失败，请根据上面的提示排查。
pause
exit /b 1
