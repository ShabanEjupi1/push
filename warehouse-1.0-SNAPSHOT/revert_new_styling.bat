@echo off
echo ===================================================
echo         Reverting Warehouse Web Styling Only
echo ===================================================
echo.

set "BACKUP_DIR=%~dp0backups_styling_fix"
set "DEST_DIR=%~dp0"

if not exist "%BACKUP_DIR%" (
    echo ERROR: Backup directory not found at: %BACKUP_DIR%
    pause
    exit /b 1
)

echo Reverting style stylesheets and layout template...
copy /Y "%BACKUP_DIR%\template.xhtml" "%DEST_DIR%WEB-INF\templates\template.xhtml" >nul
copy /Y "%BACKUP_DIR%\default.css" "%DEST_DIR%resources\css\default.css" >nul
copy /Y "%BACKUP_DIR%\pf_overrides.css" "%DEST_DIR%resources\css\pf_overrides.css" >nul

echo.
echo SUCCESS: Styling and layout template reverted to original state!
echo Please run deploy.bat to redeploy the reverted version.
echo.
pause
exit /b 0
