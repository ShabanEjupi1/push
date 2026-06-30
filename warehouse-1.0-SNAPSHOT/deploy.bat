@echo off
setlocal enabledelayedexpansion

echo ==========================================================
echo       Warehouse Application Compilation and Deployment
echo ==========================================================
echo.

REM --- CONFIGURATION ---
REM Adjust these paths to match the target server environment
set "GLASSFISH_ROOT=C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3"
set "JDK_HOME=C:\Program Files\Java\jdk1.7.0_45"
set "APP_DIR=%~dp0"
set "CONTEXT_ROOT=/warehouse"
set "APP_NAME=warehouse"

REM --- SSL / HTTPS CONFIGURATION ---
REM Put your certificate bundle (.pfx / .p12) here and set its password below.
REM If the PFX file is not found, the SSL step is skipped automatically.
set "PFX_FILE=%APP_DIR%\ssl\warehouse.pfx"
set "PFX_PASSWORD=CHANGE_ME_PFX_PASSWORD"
set "CERT_ALIAS=warehouse"
set "HTTPS_PORT=443"
REM GlassFish keystore password (default is 'changeit' on a stock install)
set "GF_STOREPASS=changeit"
REM ---------------------

REM Ensure APP_DIR does not end with trailing backslash (for consistency)
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"

echo [1/5] Verifying paths...
if not exist "%GLASSFISH_ROOT%" (
    echo ERROR: Glassfish root directory not found at: %GLASSFISH_ROOT%
    goto :error
)
if not exist "%JDK_HOME%" (
    echo ERROR: JDK directory not found at: %JDK_HOME%
    goto :error
)
echo Glassfish found at: %GLASSFISH_ROOT%
echo JDK found at: %JDK_HOME%
echo Application directory: %APP_DIR%
echo.

set "ASADMIN=%GLASSFISH_ROOT%\bin\asadmin.bat"
if not exist "%ASADMIN%" (
    set "ASADMIN=%GLASSFISH_ROOT%\glassfish\bin\asadmin.bat"
)
if not exist "%ASADMIN%" (
    echo ERROR: asadmin.bat not found under %GLASSFISH_ROOT%
    goto :error
)

echo [2/5] Fixing stale server configuration paths and services...
REM Check for admin privileges
net session >nul 2>&1
set "IS_ADMIN=0"
if %errorlevel% == 0 (
    set "IS_ADMIN=1"
    echo Running with Administrator privileges.
) else (
    echo WARNING: Not running as Administrator. Service configuration updates will be skipped.
)

REM Stop services and fix service binpaths if running as admin
powershell -Command ^
    "$services = Get-CimInstance Win32_Service | Where-Object { $_.PathName -like '*glassfish*' -or $_.PathName -like '*asadmin*' -or $_.Name -like '*domain1*' }; " ^
    "foreach ($svc in $services) { " ^
    "  Write-Host ('Found Glassfish Service: ' + $svc.DisplayName); " ^
    "  Write-Host ('  Current Path: ' + $svc.PathName); " ^
    "  if ($svc.State -eq 'Running') { " ^
    "    Write-Host '  Stopping service...'; " ^
    "    Stop-Service -Name $svc.Name -Force -ErrorAction SilentlyContinue; " ^
    "  } " ^
    "  if ('%IS_ADMIN%' -eq '1' -and $svc.PathName -like '*D:\glassfish from old server*') { " ^
    "    $newPath = $svc.PathName -replace [regex]::Escape('D:\glassfish from old server\Glassfish_3.1.2.2\glassfish3'), '%GLASSFISH_ROOT%'; " ^
    "    Write-Host ('  Updating service path to: ' + $newPath); " ^
    "    sc.exe config $svc.Name binPath= $newPath | Out-Null; " ^
    "  } " ^
    "}"

REM Kill any lingering process using port 4848 (the old instance)
echo Checking for active processes on port 4848...
powershell -Command ^
    "$p = Get-NetTCPConnection -LocalPort 4848 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -ErrorAction SilentlyContinue; " ^
    "if ($p) { " ^
    "  Write-Host ('Killing lingering Java process (PID: ' + $p + ') occupying admin port 4848...'); " ^
    "  Stop-Process -Id $p -Force -ErrorAction SilentlyContinue; " ^
    "  Start-Sleep -s 3; " ^
    "}"

set "DOMAIN_XML=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\domain.xml"
if exist "%DOMAIN_XML%" (
    echo Backing up domain.xml...
    if not exist "%DOMAIN_XML%.bak" copy "%DOMAIN_XML%" "%DOMAIN_XML%.bak" >nul
    
    echo Fixing old 'D:\glassfish from old server' references in domain.xml...
    set "OLD_PATH=D:\glassfish from old server\Glassfish_3.1.2.2\glassfish3"
    
    REM Run PowerShell to replace the old path and database IP with the current settings
    powershell -Command "$xml = '%DOMAIN_XML%'; $old = 'D:\glassfish from old server\Glassfish_3.1.2.2\glassfish3'; $new = '%GLASSFISH_ROOT%'; if (Test-Path $xml) { (Get-Content $xml) | ForEach-Object { $_ -replace [regex]::Escape($old), $new -replace '10\.10\.10\.210', '10.10.10.91' } | Set-Content $xml; echo 'Paths and database IP updated in domain.xml.' }"
) else (
    echo WARNING: domain.xml not found at %DOMAIN_XML%. Skipping path fix.
)

echo Starting Glassfish domain...
powershell -Command ^
    "$svc = Get-Service | Where-Object { $_.Name -like '*domain1*' -or $_.DisplayName -like '*glassfish*' } | Select-Object -First 1; " ^
    "if ($svc -and '%IS_ADMIN%' -eq '1') { " ^
    "  Write-Host ('Starting Glassfish via Service: ' + $svc.DisplayName); " ^
    "  Start-Service -Name $svc.Name; " ^
    "} else { " ^
    "  Write-Host 'Starting Glassfish domain domain1...'; " ^
    "  & '%ASADMIN%' start-domain domain1; " ^
    "}"
echo.

echo [SSL] Configuring HTTPS on port %HTTPS_PORT%...
set "KEYTOOL=%JDK_HOME%\bin\keytool.exe"
set "KEYSTORE=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\keystore.jks"
if not exist "%PFX_FILE%" (
    echo WARNING: Certificate "%PFX_FILE%" not found. Skipping SSL configuration.
    goto :ssl_done
)
if not exist "%KEYTOOL%" (
    echo WARNING: keytool.exe not found at "%KEYTOOL%". Skipping SSL configuration.
    goto :ssl_done
)

REM Import the certificate into the GlassFish keystore only if not already there
"%KEYTOOL%" -list -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -alias %CERT_ALIAS% >nul 2>&1
if errorlevel 1 (
    echo Importing certificate from "%PFX_FILE%" into the keystore...
    REM Discover the private-key alias inside the PFX (varies per certificate)
    set "PFX_LIST=%TEMP%\pfx_list_%RANDOM%.txt"
    "%KEYTOOL%" -list -keystore "%PFX_FILE%" -storetype PKCS12 -storepass %PFX_PASSWORD% > "!PFX_LIST!" 2>nul
    set "SRC_ALIAS="
    for /f "tokens=1 delims=," %%i in ('findstr /i "PrivateKeyEntry" "!PFX_LIST!"') do (
        if not defined SRC_ALIAS set "SRC_ALIAS=%%i"
    )
    del "!PFX_LIST!" >nul 2>&1
    if not defined SRC_ALIAS (
        echo ERROR: Could not read a private key from the PFX. Check PFX_PASSWORD. Skipping SSL.
        goto :ssl_done
    )
    "%KEYTOOL%" -importkeystore -noprompt ^
        -srckeystore "%PFX_FILE%" -srcstoretype PKCS12 -srcstorepass %PFX_PASSWORD% -srcalias "!SRC_ALIAS!" ^
        -destkeystore "%KEYSTORE%" -deststoretype JKS -deststorepass %GF_STOREPASS% -destalias %CERT_ALIAS%
    if errorlevel 1 (
        echo ERROR: Certificate import failed. Skipping SSL configuration.
        goto :ssl_done
    )
    echo Certificate imported as alias '%CERT_ALIAS%'.
) else (
    echo Certificate alias '%CERT_ALIAS%' already present in keystore. Skipping import.
)

REM Point the secure listener (http-listener-2) at the cert and move it to %HTTPS_PORT%
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.cert-nickname=%CERT_ALIAS%
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled=true
call "%ASADMIN%" set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.port=%HTTPS_PORT%
call "%ASADMIN%" set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-2.enabled=true
REM Make the HTTP->HTTPS redirect (web.xml CONFIDENTIAL) land on the clean 443 URL
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.redirect-port=%HTTPS_PORT%

echo Restarting domain to apply SSL settings...
call "%ASADMIN%" restart-domain domain1
echo SSL configuration applied.

:ssl_done
echo.

echo [3/5] Compiling Java source files...
REM Find javac.exe
set "JAVAC=%JDK_HOME%\bin\javac.exe"
if not exist "%JAVAC%" (
    echo ERROR: javac.exe not found at: %JAVAC%
    goto :error
)

REM Generate the list of java files to compile
set "SOURCES_FILE=%APP_DIR%\sources.txt"
if exist "%SOURCES_FILE%" del "%SOURCES_FILE%"

echo Searching for .java files in %APP_DIR%\WEB-INF\classes...
dir /s /b "%APP_DIR%\WEB-INF\classes\*.java" > "%SOURCES_FILE%" 2>nul

if not exist "%SOURCES_FILE%" (
    echo ERROR: No Java source files found to compile!
    goto :error
)

REM Check if sources file is empty
for %%A in ("%SOURCES_FILE%") do if %%~zA==0 (
    echo ERROR: No Java source files found in %APP_DIR%\WEB-INF\classes
    del "%SOURCES_FILE%"
    goto :error
)

echo Compiling files using javac...
REM Set up classpath (WEB-INF\lib\*.jar and Glassfish modules\*.jar)
set "CP=%APP_DIR%\WEB-INF\lib\*;%GLASSFISH_ROOT%\glassfish\modules\*"

"%JAVAC%" -proc:none -cp "%CP%" -d "%APP_DIR%\WEB-INF\classes" -source 1.7 -target 1.7 "@%SOURCES_FILE%"
if errorlevel 1 (
    echo ERROR: Compilation failed!
    del "%SOURCES_FILE%"
    goto :error
)
echo Compilation successful.
del "%SOURCES_FILE%"
echo.

echo [4/5] Undeploying old application versions...
echo Running asadmin undeploy for 'whs'...
call "%ASADMIN%" undeploy whs
if errorlevel 1 (
    echo WARNING: Failed to undeploy 'whs'. It might not be deployed. Continuing...
)

echo Running asadmin undeploy for 'warehouse'...
call "%ASADMIN%" undeploy warehouse
if errorlevel 1 (
    echo WARNING: Failed to undeploy 'warehouse'. It might not be deployed. Continuing...
)

echo Running asadmin undeploy for 'warehouse-1.0-SNAPSHOT'...
call "%ASADMIN%" undeploy warehouse-1.0-SNAPSHOT
if errorlevel 1 (
    echo WARNING: Failed to undeploy 'warehouse-1.0-SNAPSHOT'. It might not be deployed. Continuing...
)
echo.

echo [5/5] Deploying new application (%APP_NAME%)...
echo Running asadmin deploy...
call "%ASADMIN%" deploy --contextroot %CONTEXT_ROOT% --name %APP_NAME% "%APP_DIR%"
if errorlevel 1 (
    echo ERROR: Deployment failed!
    goto :error
)

echo ==========================================================
echo SUCCESS: Application compiled and deployed successfully!
echo Context Root: %CONTEXT_ROOT%
echo Access URL: https://10.10.10.7%CONTEXT_ROOT%/  (HTTP is redirected to HTTPS)
echo ==========================================================
pause
exit /b 0

:error
echo ==========================================================
echo ERROR: Deployment process failed. Please check the logs.
echo ==========================================================
pause
exit /b 1
