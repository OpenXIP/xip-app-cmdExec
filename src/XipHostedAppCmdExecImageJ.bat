@ECHO %1
@ECHO %2
@ECHO %3
@ECHO %4
for /f %%i in ("%0") do set curpath=%%~dpi
cd /d %curpath%
java -Xms256m -Xmx1024m -cp ..\lib\XIPApp.jar; XipHostedAppCmdExec %1 %2 %3 %4 --executable "C:\XIP\ImageJ\ImageJ.exe" --executableDir "C:\XIP\ImageJ"
REM EXIT