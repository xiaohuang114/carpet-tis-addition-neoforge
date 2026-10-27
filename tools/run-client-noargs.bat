@echo off
cd /d "%~dp0.."
call gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runClient > _s6_client2.log 2>&1
exit /b %ERRORLEVEL%
