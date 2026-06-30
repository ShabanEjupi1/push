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
REM TLS is handled by the nginx reverse proxy (see nginx\warehouse.conf + SSL_SETUP.md),
REM so GlassFish stays on plain HTTP (8080) and this block is OFF by default.
REM Set CONFIGURE_GLASSFISH_SSL=true only if you want GlassFish to terminate TLS itself.
set "CONFIGURE_GLASSFISH_SSL=false"
REM SSL_MODE=selfsigned -> auto-generate a FREE self-signed cert (no file needed)
REM SSL_MODE=pfx        -> import an existing .pfx/.p12 from the ssl\ folder
set "SSL_MODE=selfsigned"
set "CERT_ALIAS=warehouse"
set "CERT_HOST=10.10.10.7"
set "HTTPS_PORT=443"
REM GlassFish keystore password (default is 'changeit' on a stock install)
set "GF_STOREPASS=changeit"
REM Only used when SSL_MODE=pfx:
set "PFX_PASSWORD=CHANGE_ME_PFX_PASSWORD"

REM --- NGINX REVERSE PROXY (handles HTTPS, IP-only self-signed) ---
REM Set SETUP_NGINX=false to skip the proxy install/config step entirely.
set "SETUP_NGINX=true"
set "NGINX_HOME=C:\nginx"
set "NGINX_VERSION=1.27.4"
set "SERVER_IP=10.10.10.7"
set "BACKEND_HOSTPORT=127.0.0.1:8080"
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

echo [SSL] GlassFish-side TLS...
if /i not "%CONFIGURE_GLASSFISH_SSL%"=="true" (
    echo TLS is handled by the nginx reverse proxy; GlassFish stays on plain HTTP. Skipping.
    goto :ssl_done
)
echo Configuring HTTPS on port %HTTPS_PORT% (mode: %SSL_MODE%)...
set "KEYTOOL=%JDK_HOME%\bin\keytool.exe"
set "KEYSTORE=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\keystore.jks"
set "PFX_FILE=%APP_DIR%\ssl\warehouse.pfx"
if not exist "%KEYTOOL%" (
    echo WARNING: keytool.exe not found at "%KEYTOOL%". Skipping SSL configuration.
    goto :ssl_done
)

REM Only create/import the cert if the alias is not already in the keystore
"%KEYTOOL%" -list -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -alias %CERT_ALIAS% >nul 2>&1
if not errorlevel 1 (
    echo Certificate alias '%CERT_ALIAS%' already present in keystore. Skipping creation.
    goto :ssl_apply
)

if /i "%SSL_MODE%"=="pfx" goto :ssl_import_pfx

REM --- Generate a free self-signed certificate (CN + SAN = the server IP) ---
echo Generating a free self-signed certificate for %CERT_HOST% (valid 10 years)...
"%KEYTOOL%" -genkeypair -alias %CERT_ALIAS% -keyalg RSA -keysize 2048 -validity 3650 ^
    -dname "CN=%CERT_HOST%, O=Dogana e Kosoves, C=XK" -ext "SAN=IP:%CERT_HOST%" ^
    -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -keypass %GF_STOREPASS%
if errorlevel 1 (
    echo ERROR: Self-signed certificate generation failed. Skipping SSL.
    goto :ssl_done
)
echo Self-signed certificate created as alias '%CERT_ALIAS%'.
REM Export the public cert so it can be trusted on client PCs (import into "Trusted Root")
"%KEYTOOL%" -exportcert -rfc -alias %CERT_ALIAS% -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -file "%APP_DIR%\warehouse-cert.crt" >nul 2>&1
echo Public certificate exported to "%APP_DIR%\warehouse-cert.crt" (install this on client PCs to remove the warning).
goto :ssl_apply

:ssl_import_pfx
if not exist "%PFX_FILE%" (
    echo WARNING: PFX "%PFX_FILE%" not found. Skipping SSL configuration.
    goto :ssl_done
)
echo Importing certificate from "%PFX_FILE%" into the keystore...
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
goto :ssl_apply

:ssl_apply
REM Point the secure listener (http-listener-2) at the cert and move it to %HTTPS_PORT%
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.cert-nickname=%CERT_ALIAS%
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.security-enabled=true
REM Turn off the dead SSLv2/SSLv3 protocols (leave TLS on)
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.ssl2-enabled=false
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.ssl3-enabled=false
call "%ASADMIN%" set configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.tls-enabled=true
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
echo.

echo [NGINX] Setting up HTTPS reverse proxy (IP-only, self-signed)...
if /i not "%SETUP_NGINX%"=="true" (
    echo SETUP_NGINX is not 'true'. Skipping reverse proxy setup.
    goto :nginx_done
)

set "NGINX_EXE=%NGINX_HOME%\nginx.exe"
set "SSL_DIR=%NGINX_HOME%\ssl"
set "SSL_DIR_FWD=%NGINX_HOME:\=/%/ssl"
set "NGINX_URL=https://nginx.org/download/nginx-%NGINX_VERSION%.zip"
set "NGINX_ZIP=%TEMP%\nginx-%NGINX_VERSION%.zip"
set "P12=%SSL_DIR%\warehouse.p12"

REM 1) Download + unpack nginx if it is not installed yet
if not exist "%NGINX_EXE%" (
    echo Downloading nginx %NGINX_VERSION% ...
    powershell -Command "try { [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%NGINX_URL%' -OutFile '%NGINX_ZIP%' } catch { Write-Host $_.Exception.Message; exit 1 }"
    if errorlevel 1 (
        echo ERROR: Could not download nginx. Check internet access on this server. Skipping proxy.
        goto :nginx_done
    )
    echo Extracting nginx to %NGINX_HOME% ...
    powershell -Command "Expand-Archive -Path '%NGINX_ZIP%' -DestinationPath '%TEMP%\nginx_x' -Force; $s=Get-ChildItem -Directory '%TEMP%\nginx_x' | Select-Object -First 1; New-Item -ItemType Directory -Force -Path '%NGINX_HOME%' | Out-Null; Copy-Item ($s.FullName+'\*') '%NGINX_HOME%' -Recurse -Force; Remove-Item '%TEMP%\nginx_x' -Recurse -Force"
)

REM 2) Make sure the folders nginx needs exist
if not exist "%SSL_DIR%" mkdir "%SSL_DIR%"
if not exist "%NGINX_HOME%\logs" mkdir "%NGINX_HOME%\logs"
if not exist "%NGINX_HOME%\temp" mkdir "%NGINX_HOME%\temp"

REM 3) Create the self-signed cert (once) and convert it to PEM for nginx
if not exist "%SSL_DIR%\fullchain.pem" (
    echo Generating self-signed certificate for %SERVER_IP% ...
    "%JDK_HOME%\bin\keytool.exe" -genkeypair -alias warehouse -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=%SERVER_IP%, O=Dogana e Kosoves, C=XK" -ext "SAN=IP:%SERVER_IP%" -storetype PKCS12 -keystore "%P12%" -storepass changeit -keypass changeit
    if errorlevel 1 (
        echo ERROR: keytool certificate generation failed. Skipping proxy.
        goto :nginx_done
    )
    echo Converting certificate to PEM (via JDK helper, no OpenSSL needed)...
    "%JDK_HOME%\bin\javac.exe" -d "%APP_DIR%\ssl-tools" "%APP_DIR%\ssl-tools\CertToPem.java"
    if errorlevel 1 (
        echo ERROR: Could not compile CertToPem.java. Skipping proxy.
        goto :nginx_done
    )
    "%JDK_HOME%\bin\java.exe" -cp "%APP_DIR%\ssl-tools" CertToPem "%P12%" changeit warehouse "%SSL_DIR%"
    if errorlevel 1 (
        echo ERROR: PEM conversion failed. Skipping proxy.
        goto :nginx_done
    )
)

REM 4) Write nginx.conf from the template (fill in IP / backend / cert dir)
echo Writing nginx configuration...
powershell -Command "(Get-Content '%APP_DIR%\nginx\nginx.conf.template') -replace '__SERVER_NAME__','%SERVER_IP%' -replace '__BACKEND__','%BACKEND_HOSTPORT%' -replace '__SSL_DIR__','%SSL_DIR_FWD%' | Set-Content '%NGINX_HOME%\conf\nginx.conf' -Encoding ASCII"

REM 5) Open the Windows firewall for 80 and 443
netsh advfirewall firewall delete rule name="Warehouse HTTP 80" >nul 2>&1
netsh advfirewall firewall delete rule name="Warehouse HTTPS 443" >nul 2>&1
netsh advfirewall firewall add rule name="Warehouse HTTP 80" dir=in action=allow protocol=TCP localport=80 >nul
netsh advfirewall firewall add rule name="Warehouse HTTPS 443" dir=in action=allow protocol=TCP localport=443 >nul

REM 6) Validate the config and (re)start nginx
pushd "%NGINX_HOME%"
"%NGINX_EXE%" -t
if errorlevel 1 (
    echo ERROR: nginx config test failed - see messages above. Proxy not started.
    popd
    goto :nginx_done
)
tasklist /fi "imagename eq nginx.exe" 2>nul | find /i "nginx.exe" >nul
if errorlevel 1 (
    echo Starting nginx...
    start "" "%NGINX_EXE%"
) else (
    echo Reloading nginx...
    "%NGINX_EXE%" -s reload
)
popd
echo nginx reverse proxy is running.

:nginx_done
echo.

echo ==========================================================
echo SUCCESS: Application compiled and deployed successfully!
echo Context Root: %CONTEXT_ROOT%
echo Access URL: https://%SERVER_IP%%CONTEXT_ROOT%/  (HTTP redirects to HTTPS via nginx)
echo Note: browsers show a one-time "not trusted" warning (self-signed, IP-only).
echo ==========================================================
pause
exit /b 0

:error
echo ==========================================================
echo ERROR: Deployment process failed. Please check the logs.
echo ==========================================================
pause
exit /b 1
