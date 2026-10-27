@echo off
cd /d "%~dp0.."
call gradlew.bat --no-daemon --console=plain -PtisFull -Dmixin.debug.verbose=true runClient --args="--quickPlaySingleplayer S6Test" > _s6_client1.log 2>&1
exit /b %ERRORLEVEL%
