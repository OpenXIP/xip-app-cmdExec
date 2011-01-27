@ECHO %1
@ECHO %2
@ECHO %3
@ECHO %4
for /f %%i in ("%0") do set curpath=%%~dpi
cd /d %curpath%
java -Xms256m -Xmx1024m -cp ..\lib\XIPApp.jar; XipHostedAppCmdExec %1 %2 %3 %4 --executable "C:\Program Files (x86)\DICOM Tool\SR Browser\SRBrowseEng.exe" --executableDir "C:\Program Files (x86)\DICOM Tool\SR Browser"
REM EXIT