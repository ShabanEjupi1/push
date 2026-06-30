@echo off
echo ===================================================
echo           Restoring Original App Styling
echo ===================================================
echo.

set "BACKUP_DIR=%~dp0backups_styling_fix"
set "DEST_DIR=%~dp0"

if not exist "%BACKUP_DIR%" (
    echo ERROR: Backup directory not found at: %BACKUP_DIR%
    pause
    exit /b 1
)

echo Restoring files...
copy /Y "%BACKUP_DIR%\template.xhtml" "%DEST_DIR%WEB-INF\templates\template.xhtml" >nul
copy /Y "%BACKUP_DIR%\excel_report.xhtml" "%DEST_DIR%excel_report.xhtml" >nul
copy /Y "%BACKUP_DIR%\version.properties" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\version.properties" >nul
copy /Y "%BACKUP_DIR%\messages_en.properties" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\messages_en.properties" >nul
copy /Y "%BACKUP_DIR%\messages_sq.properties" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\messages_sq.properties" >nul
copy /Y "%BACKUP_DIR%\messages_sr.properties" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\messages_sr.properties" >nul
copy /Y "%BACKUP_DIR%\ExcelExporterService.java" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\boundary\ExcelExporterService.java" >nul
copy /Y "%BACKUP_DIR%\ExcelReportBean.java" "%DEST_DIR%WEB-INF\classes\mk\com\snt\kc\warehouse\view\ExcelReportBean.java" >nul
copy /Y "%BACKUP_DIR%\default.css" "%DEST_DIR%resources\css\default.css" >nul
copy /Y "%BACKUP_DIR%\pf_overrides.css" "%DEST_DIR%resources\css\pf_overrides.css" >nul

echo.
echo SUCCESS: Original files restored successfully!
echo.
pause
exit /b 0
