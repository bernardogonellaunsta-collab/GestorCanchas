
@echo off
cd /d %~dp0
cd src
javac com/gestor/*.java
if %errorlevel% neq 0 exit /b %errorlevel%
java com.gestor.GestorDeportivoApp
