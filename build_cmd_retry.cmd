@echo off
setlocal

cd /d e:\mc_modding\MMCEGE

call e:\mc_modding\MMCEGE\mmce-src\gradlew.bat --stop >nul 2>nul

if exist "e:\mc_modding\MMCEGE\mmce-gui-ext\build\rfg\minecraft-src" (
  rmdir /s /q "e:\mc_modding\MMCEGE\mmce-gui-ext\build\rfg\minecraft-src"
)

set "JAVA_HOME=C:\Program Files\Java\jdk-1.8"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call e:\mc_modding\MMCEGE\mmce-src\gradlew.bat -p e:\mc_modding\MMCEGE\mmce-gui-ext build --no-daemon --no-parallel --console=plain --stacktrace > e:\mc_modding\MMCEGE\mmce-gui-ext\cmd_build.log 2>&1
echo EXITCODE=%ERRORLEVEL%> e:\mc_modding\MMCEGE\mmce-gui-ext\cmd_exit.txt

endlocal & exit /b %ERRORLEVEL%
