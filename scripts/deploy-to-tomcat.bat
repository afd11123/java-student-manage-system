@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  一键部署脚本
rem  作用：把 target\score-management.war 复制到 Tomcat 的 webapps 下并重启 Tomcat
rem  前提：先执行过 build-offline.bat，或 IDEA 里 mvn package 成功
rem ============================================================

set "JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261"
set "TOMCAT_HOME=D:\apache-tomcat-8.5.0-windows-x64\apache-tomcat-8.5.0"
set "PROJECT_DIR=%~dp0.."
set "WAR=%PROJECT_DIR%\target\score-management.war"

if not exist "%WAR%" (
    echo [错误] 找不到 %WAR%
    echo 请先执行 scripts\build-offline.bat 打包
    pause
    exit /b 1
)

echo ============================================================
echo  1/3 停止 Tomcat（如果正在运行）
echo ============================================================
call "%TOMCAT_HOME%\bin\shutdown.bat"
timeout /t 3 /nobreak >nul

echo ============================================================
echo  2/3 清理旧部署并复制新 WAR
echo ============================================================
if exist "%TOMCAT_HOME%\webapps\score-management" rmdir /s /q "%TOMCAT_HOME%\webapps\score-management"
copy /y "%WAR%" "%TOMCAT_HOME%\webapps\" >nul
if errorlevel 1 (
    echo [错误] 复制失败，请检查 Tomcat 是否已停止、文件是否被占用
    pause
    exit /b 1
)

echo ============================================================
echo  3/3 启动 Tomcat
echo ============================================================
call "%TOMCAT_HOME%\bin\startup.bat"

echo.
echo [完成] 等待 5 秒后访问：
echo        http://localhost:8080/score-management/
echo.
timeout /t 5 /nobreak >nul
start "" "http://localhost:8080/score-management/"
exit /b 0
