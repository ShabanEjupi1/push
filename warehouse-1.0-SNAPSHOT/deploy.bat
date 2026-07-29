@echo off
setlocal enabledelayedexpansion

REM Bump this whenever this file changes. It is printed on every run and copied
REM into deploy-full.log, so a log can be matched to the script that produced
REM it. Without it there is no way to tell from the output whether the server is
REM running the current script or a copy from three deployments ago - which has
REM already cost one debugging session.
set "SCRIPT_VERSION=2026-07-29.1"

echo ==========================================================
echo       Warehouse Application Compilation and Deployment
echo       script version %SCRIPT_VERSION%
echo ==========================================================
echo.

REM --- CONFIGURATION ---
REM Adjust these paths to match the target server environment
set "GLASSFISH_ROOT=C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3"
set "JDK_HOME=C:\Program Files\Java\jdk1.7.0_45"
set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"

REM --- LOCATE THE APPLICATION FOLDER ---
REM Normally this script lives INSIDE the exploded web app, i.e. next to
REM WEB-INF\web.xml, and APP_DIR above is already correct.
REM If the .bat files were copied somewhere else - typically straight onto the
REM Desktop, NEXT TO the warehouse-1.0-SNAPSHOT folder rather than inside it -
REM APP_DIR points at the Desktop, there is no WEB-INF there, and step [3/5]
REM dies with "No Java source files found". Recover by looking one level down
REM for the folder that really is the web app.
if not exist "%APP_DIR%\WEB-INF\web.xml" (
    for /d %%D in ("%APP_DIR%\*") do (
        if not defined FOUND_APP_DIR if exist "%%~fD\WEB-INF\web.xml" set "FOUND_APP_DIR=%%~fD"
    )
)
if defined FOUND_APP_DIR (
    echo NOTE: This script is not inside the application folder.
    echo       Using the app found at: !FOUND_APP_DIR!
    set "APP_DIR=!FOUND_APP_DIR!"
)
if not exist "%APP_DIR%\WEB-INF\web.xml" (
    echo ERROR: Could not find the application. Expected WEB-INF\web.xml in:
    echo            %APP_DIR%
    echo        or in a folder directly inside it ^(e.g. warehouse-1.0-SNAPSHOT^).
    echo.
    echo        Put deploy.bat / deploy-full.bat / install-services.bat /
    echo        healthcheck.ps1 INSIDE the warehouse-1.0-SNAPSHOT folder and
    echo        run them from there.
    goto :error
)

set "CONTEXT_ROOT=/warehouse"
set "APP_NAME=warehouse"

REM ==========================================================================
REM  SERVE_MODE - who terminates HTTPS
REM ==========================================================================
REM   glassfish : GlassFish serves TLS itself on :443. No proxy, no nginx, one
REM               process to keep alive. This is the default.
REM   nginx     : the old layout - nginx on :443 reverse-proxying to GlassFish
REM               on plain :8080. Kept as a fallback (see the TLS note below).
REM
REM WHY GLASSFISH MODE NEEDS THE "ALL PROTOCOLS FALSE" TRICK
REM   GlassFish 3.1.2.2 ships Grizzly 1.9, which builds the listener's enabled
REM   protocol list from the ssl2-enabled / ssl3-enabled / tls-enabled flags -
REM   and "tls-enabled" means TLSv1.0 ONLY. There are no tls11/tls12 flags in
REM   this version, so the obvious configuration produces a listener that only
REM   speaks TLS 1.0, which every current browser refuses ("unsupported
REM   protocol"). That is what drove this deployment to nginx in the first
REM   place. Setting ALL THREE flags to false makes Grizzly skip
REM   setEnabledProtocols() entirely, leaving the JSSE defaults - and on JDK 7 a
REM   server socket defaults to TLS 1.0 + 1.1 + 1.2. That is how :443 ends up
REM   speaking TLS 1.2. See the [SSL] step; healthcheck.ps1 verifies it for real
REM   with a TLS 1.2 handshake, because this is the one thing that decides
REM   whether glassfish mode works on this box.
REM
REM   The remaining limitation is ciphers: JDK 7 has no AES-GCM, so browsers
REM   negotiate the older AES-CBC suites. Current Chrome/Edge/Firefox still
REM   accept those. If a future browser drops them, set SERVE_MODE=nginx and
REM   re-run - nothing else needs to change.
set "SERVE_MODE=glassfish"

set "SERVER_IP=10.10.10.7"
set "HTTPS_PORT=443"
set "HTTP_PORT=80"

REM --- SSL / HTTPS CONFIGURATION ---
REM CERT_MODE decides where the certificate comes from. It applies to BOTH
REM serve modes - the same ssl\ folder feeds either the GlassFish keystore or
REM the nginx PEM files.
REM   cacert     -> a cert ISSUED BY THE DOGANA INTERNAL CA. Trusted on every
REM                 domain PC, so no browser warning. Needs two files that are
REM                 deliberately NOT in git:
REM                   ssl\warehouse.p12        - private key (ssl-tools\request-cert.bat)
REM                   ssl\warehouse-server.cer - the issued leaf cert from the CA
REM                 plus ssl\dogana-ca.cer, which IS committed.
REM   selfsigned -> generate a cert for the IP. Works immediately, one-time
REM                 browser warning on every client.
REM If cacert is selected but the two files are missing, the script says so and
REM falls back to selfsigned rather than leaving the site without HTTPS.
set "CERT_MODE=cacert"
set "CERT_ALIAS=warehouse"
set "CERT_HOST=%SERVER_IP%"
REM GlassFish keystore password (default is 'changeit' on a stock install)
set "GF_STOREPASS=changeit"
set "CA_CERT_FILE=%APP_DIR%\ssl\dogana-ca.cer"
set "SERVER_CERT_FILE=%APP_DIR%\ssl\warehouse-server.cer"
set "SERVER_P12=%APP_DIR%\ssl\warehouse.p12"
set "P12_PASSWORD=changeit"

REM --- NGINX (only used when SERVE_MODE=nginx) ---
set "NGINX_HOME=C:\nginx"
set "NGINX_VERSION=1.27.4"
set "BACKEND_HOSTPORT=127.0.0.1:8080"

REM Derive the two old switches from SERVE_MODE so the rest of the script, and
REM anything else that reads them, keeps working unchanged.
if /i "%SERVE_MODE%"=="glassfish" (
    set "CONFIGURE_GLASSFISH_SSL=true"
    set "SETUP_NGINX=false"
) else (
    set "CONFIGURE_GLASSFISH_SSL=false"
    set "SETUP_NGINX=true"
)
REM The nginx path still reads NGINX_CERT_MODE; keep the two names in step so
REM there is only ONE certificate switch to think about (CERT_MODE, above).
set "NGINX_CERT_MODE=%CERT_MODE%"

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

REM --------------------------------------------------------------------------
REM  Free the ports GlassFish is about to bind BEFORE trying to start it.
REM  In glassfish mode the domain owns 80 and 443, so nginx holding either one
REM  makes start-domain fail with a bind exception - and asadmin reports that
REM  only as "Please look at the server log for more details". This used to
REM  happen after the start attempt, which was too late to help it.
REM --------------------------------------------------------------------------
if /i "%SERVE_MODE%"=="glassfish" call :free_glassfish_ports

REM  Stop whatever instance is already running, cleanly. The old code killed the
REM  process on 4848 outright, which is precisely what leaves the stale pid file
REM  and half-written osgi-cache that make the NEXT start fail.
call :stop_running_domain

REM --------------------------------------------------------------------------
REM  domain.xml - migrate the paths left over from the old server.
REM
REM  This used to run Get-Content | Set-Content on EVERY deploy, whether or not
REM  there was anything to change. That is not free: it rewrites the whole file,
REM  re-encoding it, and if the run is interrupted part way - which on this box
REM  means the memory manager killing the step - domain.xml is left truncated.
REM  A truncated domain.xml is not a warning, it is a domain that never starts
REM  again, and the only message you get is "Please look at the server log".
REM
REM  So now: only rewrite when there is genuinely something to replace, write to
REM  a temp file first, and only put it in place once it parses as XML.
REM --------------------------------------------------------------------------
set "DOMAIN_XML=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\domain.xml"
if exist "%DOMAIN_XML%" (
    if not exist "%DOMAIN_XML%.bak" (
        echo Backing up domain.xml...
        copy "%DOMAIN_XML%" "%DOMAIN_XML%.bak" >nul
    )
    call :migrate_domain_xml
    if errorlevel 1 goto :error
) else (
    echo WARNING: domain.xml not found at %DOMAIN_XML%. Skipping path fix.
)

REM --------------------------------------------------------------------------
REM  Start the domain AND PROVE IT CAME UP before going any further.
REM  Everything after this point - undeploy, deploy, the asadmin set commands -
REM  talks to localhost:4848. If the domain is not up they all fail with
REM  "Remote server does not listen for requests on [localhost:4848]", which
REM  says nothing about the real cause and buries it under a screen of noise.
REM  :start_domain_verified stops at the first failure and prints the actual
REM  reason out of server.log.
REM --------------------------------------------------------------------------
call :start_domain_verified
if errorlevel 1 goto :error
echo.

echo [SSL] GlassFish-side TLS...
if /i not "%CONFIGURE_GLASSFISH_SSL%"=="true" (
    echo SERVE_MODE=%SERVE_MODE% - TLS is handled by nginx; GlassFish stays on plain HTTP. Skipping.
    goto :ssl_done
)
echo Configuring HTTPS on port %HTTPS_PORT% (certificate mode: %CERT_MODE%)...
set "KEYTOOL=%JDK_HOME%\bin\keytool.exe"
set "KEYSTORE=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\keystore.jks"
set "TRUSTSTORE=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\cacerts.jks"
if not exist "%KEYTOOL%" (
    echo WARNING: keytool.exe not found at "%KEYTOOL%". Skipping SSL configuration.
    goto :ssl_done
)

REM  nginx has already been taken off :80/:443 by :free_glassfish_ports, above -
REM  it has to happen before the domain starts, not here, or GlassFish never
REM  gets to bind the ports in the first place.

REM --- Back up the keystore before touching it -------------------------------
REM keytool edits keystore.jks in place. If an import goes wrong there is no
REM undo, and a corrupt keystore stops the domain from starting at all.
if not exist "%KEYSTORE%.bak" copy "%KEYSTORE%" "%KEYSTORE%.bak" >nul 2>&1

REM Re-importing on every deploy would fail on the existing alias, so only do
REM the certificate work when the alias is not there yet. Delete the alias by
REM hand to force a fresh import:
REM    keytool -delete -alias warehouse -keystore "<domain>\config\keystore.jks" -storepass changeit
"%KEYTOOL%" -list -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -alias %CERT_ALIAS% >nul 2>&1
if not errorlevel 1 (
    echo Certificate alias '%CERT_ALIAS%' already present in the keystore. Keeping it.
    goto :ssl_apply
)

if /i "%CERT_MODE%"=="cacert" goto :ssl_cacert

:ssl_selfsigned
REM --- Free self-signed certificate (CN + SAN = the server IP) ---------------
echo Generating a self-signed certificate for %CERT_HOST% (valid 10 years)...
"%KEYTOOL%" -genkeypair -alias %CERT_ALIAS% -keyalg RSA -keysize 2048 -validity 3650 ^
    -dname "CN=%CERT_HOST%, O=Dogana e Kosoves, C=XK" -ext "SAN=IP:%CERT_HOST%" ^
    -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -keypass %GF_STOREPASS%
if errorlevel 1 (
    echo ERROR: Self-signed certificate generation failed. Skipping SSL.
    goto :ssl_done
)
echo Self-signed certificate created as alias '%CERT_ALIAS%'.
"%KEYTOOL%" -exportcert -rfc -alias %CERT_ALIAS% -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -file "%APP_DIR%\warehouse-trust.crt" >nul 2>&1
certutil -addstore -f Root "%APP_DIR%\warehouse-trust.crt" >nul 2>&1
echo Public certificate exported to "%APP_DIR%\warehouse-trust.crt"
echo   ^(install it in "Trusted Root" on client PCs to remove the warning^).
goto :ssl_apply

:ssl_cacert
REM --- Certificate issued by the Dogana internal CA --------------------------
REM Three files, three different jobs - mixing them up is the usual failure:
REM   warehouse.p12        the PRIVATE KEY. The CA signed this exact key.
REM   warehouse-server.cer the ISSUED LEAF for CN=10.10.10.7. This is the one
REM                        the browser checks.
REM   dogana-ca.cer        the CA's OWN certificate. It signs, it never serves.
if not exist "%SERVER_P12%" (
    echo WARNING: "%SERVER_P12%" not found - no private key to serve with.
    echo          Run ssl-tools\request-cert.bat, submit the CSR to the Dogana CA,
    echo          then re-run. Falling back to a self-signed certificate for now.
    goto :ssl_selfsigned
)
if not exist "%SERVER_CERT_FILE%" (
    echo WARNING: issued certificate "%SERVER_CERT_FILE%" not found.
    echo          Submit ssl\warehouse.csr to http://^<ca^>/certsrv and save the
    echo          reply there. Falling back to a self-signed certificate for now.
    goto :ssl_selfsigned
)

REM Verify the "issued cert" really is OUR leaf. The CA web enrollment page has
REM a "Download CA certificate" link right next to the issued-certificate link,
REM and saving that one instead gives a file that looks plausible, imports
REM without complaint in some tools, and then serves a certificate for the CA
REM rather than for this server. Check the subject before trusting the file.
set "CERT_DUMP=%TEMP%\whs_cert_%RANDOM%.txt"
"%KEYTOOL%" -printcert -file "%SERVER_CERT_FILE%" > "!CERT_DUMP!" 2>&1
findstr /i /c:"Owner: CN=%CERT_HOST%" "!CERT_DUMP!" >nul
if not errorlevel 1 goto :ssl_cert_ok

REM Flat, not a parenthesised IF block: the explanation below contains quotes,
REM and cmd honours quoting when it scans a block for its closing ')' - an odd
REM number of them inside the block swallows the ')' and the script dies with
REM a syntax error instead of printing the message.
echo ERROR: "%SERVER_CERT_FILE%" is not the certificate for this server.
echo        Expected a subject of  CN=%CERT_HOST%  but the file says:
findstr /i /c:"Owner:" "!CERT_DUMP!"
echo.
echo        If that says CN=dogana-EMAILSUBBMITION-CA then you downloaded the CA's
echo        own certificate instead of the one issued to your CSR. Go back to
echo        http://^<ca^>/certsrv, open the page for viewing the status of a
echo        pending certificate request, and use the DOWNLOAD CERTIFICATE link
echo        there ^(Base-64^) - not the download-CA-certificate link.
echo        Falling back to a self-signed certificate for now.
del "!CERT_DUMP!" >nul 2>&1
goto :ssl_selfsigned

:ssl_cert_ok
del "!CERT_DUMP!" >nul 2>&1

REM 1) The CA must be a trusted entry in this keystore BEFORE the reply is
REM    imported, otherwise keytool cannot build leaf -> CA and refuses with
REM    "Failed to establish chain from reply".
REM    Each of these is one flat line: a '^' continuation inside an IF block is
REM    consumed by the block parser before the command ever runs.
if not exist "%CA_CERT_FILE%" goto :ssl_ca_done
"%KEYTOOL%" -importcert -noprompt -trustcacerts -alias dogana-ca -file "%CA_CERT_FILE%" -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% >nul 2>&1
REM The domain's own truststore, so outbound TLS from the app trusts it too.
if exist "%TRUSTSTORE%" "%KEYTOOL%" -importcert -noprompt -trustcacerts -alias dogana-ca -file "%CA_CERT_FILE%" -keystore "%TRUSTSTORE%" -storepass %GF_STOREPASS% >nul 2>&1
REM ...and Windows, so this server does not warn at its own site.
certutil -addstore -f Root "%CA_CERT_FILE%" >nul 2>&1
:ssl_ca_done

REM 2) Move the private key into the GlassFish keystore. It arrives carrying the
REM    original self-signed placeholder cert; step 3 replaces that.
echo Importing the private key into the GlassFish keystore...
"%KEYTOOL%" -importkeystore -noprompt ^
    -srckeystore "%SERVER_P12%" -srcstoretype PKCS12 -srcstorepass %P12_PASSWORD% -srcalias %CERT_ALIAS% ^
    -destkeystore "%KEYSTORE%" -deststoretype JKS -deststorepass %GF_STOREPASS% -destkeypass %GF_STOREPASS% ^
    -destalias %CERT_ALIAS%
if errorlevel 1 (
    echo ERROR: could not import the private key from "%SERVER_P12%".
    echo        Check that P12_PASSWORD above matches the one request-cert.bat used.
    goto :ssl_done
)

REM 3) Install the CA reply over that placeholder. This is what turns the entry
REM    into "key + leaf + CA chain", which is what the listener actually serves.
echo Installing the CA reply ^(leaf + chain^)...
"%KEYTOOL%" -importcert -noprompt -trustcacerts -alias %CERT_ALIAS% -file "%SERVER_CERT_FILE%" ^
    -keystore "%KEYSTORE%" -storepass %GF_STOREPASS% -keypass %GF_STOREPASS%
if errorlevel 1 (
    echo ERROR: the CA reply did not match the private key, or its chain could not
    echo        be built. The usual cause is that warehouse.p12 was regenerated
    echo        AFTER the CSR was submitted - the CA then signed a key this server
    echo        no longer has. Delete ssl\warehouse.p12 and ssl\warehouse.csr, run
    echo        ssl-tools\request-cert.bat again and request a new certificate.
    goto :ssl_done
)
echo Certificate installed as alias '%CERT_ALIAS%' - issued by the Dogana CA.

:ssl_apply
REM --------------------------------------------------------------------------
REM  Point the secure listener at the cert and put it on :443
REM --------------------------------------------------------------------------
set "GF_PROTO=configs.config.server-config.network-config.protocols.protocol"
set "GF_LISTENER=configs.config.server-config.network-config.network-listeners.network-listener"

call "%ASADMIN%" set %GF_PROTO%.http-listener-2.ssl.cert-nickname=%CERT_ALIAS%
call "%ASADMIN%" set %GF_PROTO%.http-listener-2.security-enabled=true

REM  READ THE LONG NOTE AT THE TOP OF THIS FILE BEFORE CHANGING THESE THREE.
REM  tls-enabled=false does NOT mean "no TLS". In Grizzly 1.9 these three flags
REM  build an explicit enabled-protocol list where "tls" means TLSv1.0 only;
REM  with all three false Grizzly never calls setEnabledProtocols and the JDK 7
REM  server defaults apply instead - TLS 1.0, 1.1 AND 1.2. Setting
REM  tls-enabled=true here is what pins the listener to TLS 1.0 and produces
REM  ERR_SSL_VERSION_OR_CIPHER_MISMATCH in every current browser.
call "%ASADMIN%" set %GF_PROTO%.http-listener-2.ssl.ssl2-enabled=false
call "%ASADMIN%" set %GF_PROTO%.http-listener-2.ssl.ssl3-enabled=false
call "%ASADMIN%" set %GF_PROTO%.http-listener-2.ssl.tls-enabled=false

call "%ASADMIN%" set %GF_LISTENER%.http-listener-2.port=%HTTPS_PORT%
call "%ASADMIN%" set %GF_LISTENER%.http-listener-2.enabled=true

REM http-listener-1 stays on 8080 for local debugging; the redirect-port makes
REM any redirect it issues land on the clean :443 URL.
call "%ASADMIN%" set %GF_PROTO%.http-listener-1.http.redirect-port=%HTTPS_PORT%

REM --------------------------------------------------------------------------
REM  Port 80. Staff type "10.10.10.7/warehouse" without a scheme, so the browser
REM  tries http on :80 first. nginx used to answer there and redirect; with it
REM  gone something has to listen or they get "cannot reach this site".
REM  A second network-listener on the SAME protocol as 8080 costs nothing and
REM  HttpsSecurityFilter turns whatever arrives into a 301 to https.
REM --------------------------------------------------------------------------
call "%ASADMIN%" get %GF_LISTENER%.http-listener-80.port >nul 2>&1
if errorlevel 1 (
    echo Adding a listener on port %HTTP_PORT% ^(redirects to HTTPS^)...
    call "%ASADMIN%" create-network-listener --listenerport %HTTP_PORT% --protocol http-listener-1 --enabled=true http-listener-80
) else (
    call "%ASADMIN%" set %GF_LISTENER%.http-listener-80.port=%HTTP_PORT% >nul
    call "%ASADMIN%" set %GF_LISTENER%.http-listener-80.enabled=true >nul
)

REM  ...and attach it to the virtual server. create-network-listener does NOT do
REM  this: the "server" virtual-server carries an explicit network-listeners list
REM  ("http-listener-1,http-listener-2"), and a listener missing from it binds the
REM  port and then answers every request with a 404. That failure is particularly
REM  confusing because the port IS open, so every port check passes.
REM  Read-modify-write from PowerShell so an existing custom entry is not lost.
set "GF_VS=configs.config.server-config.http-service.virtual-server.server"
powershell -NoProfile -Command ^
    "$a = '%ASADMIN%'; " ^
    "$line = @(& $a get %GF_VS%.network-listeners 2>$null | Where-Object { $_ -match '=' }) | Select-Object -First 1; " ^
    "if (-not $line) { exit 0 }; " ^
    "$val = $line.Substring($line.IndexOf('=') + 1).Trim(); " ^
    "$names = @($val -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ }); " ^
    "if ($names -contains 'http-listener-80') { exit 0 }; " ^
    "$names += 'http-listener-80'; " ^
    "& $a set ('%GF_VS%.network-listeners=' + ($names -join ',')) | Out-Null; " ^
    "Write-Host ('   Virtual server now serves: ' + ($names -join ', '))"

REM --------------------------------------------------------------------------
REM  Bare https://10.10.10.7/ - nginx used to answer that with a 301 to
REM  /warehouse/. GlassFish serves the domain docroot there, so put a redirect
REM  page in it. Cheaper and far less risky than making the app the virtual
REM  server's default-web-module, which changes how every context path resolves.
REM --------------------------------------------------------------------------
set "GF_DOCROOT=%GLASSFISH_ROOT%\glassfish\domains\domain1\docroot"
REM  Written from PowerShell rather than a series of ECHOs: HTML is made almost
REM  entirely of the characters cmd treats as redirection, and escaping them
REM  inside an IF block is how you get a half-written file and no error.
REM  The markup uses NO double-quote characters on purpose - one here would
REM  close the batch string that is protecting the '<' and '>' from cmd, and
REM  unquoted attribute values are valid HTML as long as they contain no spaces.
if exist "%GF_DOCROOT%" powershell -NoProfile -Command ^
    "$h = '<!DOCTYPE html><html><head><meta charset=utf-8>' + " ^
    "'<meta http-equiv=refresh content=0;url=%CONTEXT_ROOT%/>' + " ^
    "'<title>Warehouse</title></head><body>' + " ^
    "'<a href=%CONTEXT_ROOT%/>Warehouse</a></body></html>'; " ^
    "Set-Content -Path '%GF_DOCROOT%\index.html' -Value $h -Encoding ASCII"

REM --------------------------------------------------------------------------
REM  Firewall. deploy.bat used to open 80/443 only in the nginx step; in
REM  glassfish mode those ports belong to GlassFish and still need opening.
REM  Silently skipped without admin rights, which is why deploy-full.bat elevates.
REM --------------------------------------------------------------------------
if "%IS_ADMIN%"=="1" (
    netsh advfirewall firewall delete rule name="Warehouse HTTP 80" >nul 2>&1
    netsh advfirewall firewall delete rule name="Warehouse HTTPS 443" >nul 2>&1
    netsh advfirewall firewall add rule name="Warehouse HTTP 80" dir=in action=allow protocol=TCP localport=%HTTP_PORT% >nul
    netsh advfirewall firewall add rule name="Warehouse HTTPS 443" dir=in action=allow protocol=TCP localport=%HTTPS_PORT% >nul
) else (
    echo WARNING: not running as Administrator - firewall rules for %HTTP_PORT%/%HTTPS_PORT% were NOT
    echo          created. The site will answer on this machine but may be
    echo          unreachable from other PCs. Re-run via deploy-full.bat.
)

echo Restarting domain to apply the listener changes...
call "%ASADMIN%" restart-domain domain1
if errorlevel 1 (
    echo ERROR: the domain did not come back after the SSL change.
    echo        The most likely cause is that port %HTTPS_PORT% or %HTTP_PORT% is held by another
    echo        program ^(nginx, IIS, Skype^). Check the end of:
    echo            %GLASSFISH_ROOT%\glassfish\domains\domain1\logs\server.log
    goto :error
)
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

REM -J-Xmx384m caps the compiler's own JVM. Without it javac defaults to a
REM quarter of physical RAM, which on this 4 GB VM means it can ask for ~1 GB
REM while GlassFish already holds its heap - enough to push the box into
REM swapping and get the deployment killed half way through. 70 source files
REM need nowhere near that much.
"%JAVAC%" -J-Xmx384m -proc:none -cp "%CP%" -d "%APP_DIR%\WEB-INF\classes" -source 1.7 -target 1.7 "@%SOURCES_FILE%"
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

echo [NGINX] Reverse proxy...
if /i not "%SETUP_NGINX%"=="true" goto :nginx_skip
echo Setting up the HTTPS reverse proxy (SERVE_MODE=%SERVE_MODE%)...

set "NGINX_EXE=%NGINX_HOME%\nginx.exe"
set "SSL_DIR=%NGINX_HOME%\ssl"
set "SSL_DIR_FWD=%NGINX_HOME:\=/%/ssl"
set "NGINX_URL=https://nginx.org/download/nginx-%NGINX_VERSION%.zip"
set "NGINX_ZIP=%TEMP%\nginx-%NGINX_VERSION%.zip"
set "P12=%SSL_DIR%\warehouse.p12"

REM 1) Download + unpack nginx if it is not installed yet
if exist "%NGINX_EXE%" goto :nginx_installed
echo Downloading nginx %NGINX_VERSION% ...
powershell -NoProfile -Command "try { [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%NGINX_URL%' -OutFile '%NGINX_ZIP%' } catch { Write-Host $_.Exception.Message; exit 1 }"
if errorlevel 1 goto :nginx_dlfail
echo Extracting nginx to %NGINX_HOME% ...
powershell -NoProfile -Command "Expand-Archive -Path '%NGINX_ZIP%' -DestinationPath '%TEMP%\nginx_x' -Force; $s=Get-ChildItem -Directory '%TEMP%\nginx_x' | Select-Object -First 1; New-Item -ItemType Directory -Force -Path '%NGINX_HOME%' | Out-Null; Copy-Item ($s.FullName+'\*') '%NGINX_HOME%' -Recurse -Force; Remove-Item '%TEMP%\nginx_x' -Recurse -Force"
:nginx_installed

REM 2) Make sure the folders nginx needs exist
if not exist "%SSL_DIR%" mkdir "%SSL_DIR%"
if not exist "%NGINX_HOME%\logs" mkdir "%NGINX_HOME%\logs"
if not exist "%NGINX_HOME%\temp" mkdir "%NGINX_HOME%\temp"

REM 3) Produce fullchain.pem + privkey.pem for nginx (mode: selfsigned | cacert)
if exist "%SSL_DIR%\fullchain.pem" goto :nginx_havecert
echo Preparing nginx certificate (mode: %NGINX_CERT_MODE%)...
REM Compile the PKCS12 -> PEM helper once; both modes use it (no OpenSSL needed).
"%JDK_HOME%\bin\javac.exe" -d "%APP_DIR%\ssl-tools" "%APP_DIR%\ssl-tools\CertToPem.java"
if errorlevel 1 goto :nginx_certfail

if /i "%NGINX_CERT_MODE%"=="cacert" goto :nginx_cacert

REM --- selfsigned: generate a self-signed cert for the IP (one-time browser warning) ---
echo Generating self-signed certificate for %SERVER_IP% ...
"%JDK_HOME%\bin\keytool.exe" -genkeypair -alias warehouse -keyalg RSA -keysize 2048 -validity 3650 -dname "CN=%SERVER_IP%, O=Dogana e Kosoves, C=XK" -ext "SAN=IP:%SERVER_IP%" -storetype PKCS12 -keystore "%P12%" -storepass changeit -keypass changeit
if errorlevel 1 goto :nginx_certfail
"%JDK_HOME%\bin\java.exe" -cp "%APP_DIR%\ssl-tools" CertToPem "%P12%" changeit warehouse "%SSL_DIR%"
if errorlevel 1 goto :nginx_certfail
goto :nginx_havecert

:nginx_cacert
REM --- cacert: use a leaf cert issued by the Dogana internal CA (trusted, no warning) ---
if not exist "%SERVER_P12%" (
    echo ERROR: "%SERVER_P12%" not found.
    echo        Run  ssl-tools\request-cert.bat  first to create the key + CSR, then submit the CSR
    echo        to the Dogana CA and save the issued cert as "%SERVER_CERT_FILE%".
    goto :nginx_certfail
)
if not exist "%SERVER_CERT_FILE%" (
    echo ERROR: issued server cert "%SERVER_CERT_FILE%" not found.
    echo        Submit ssl\warehouse.csr to http://^<ca^>/certsrv and save the reply here first.
    goto :nginx_certfail
)
echo Building the nginx certificate chain from the issued cert...
REM NOTE: we do NOT import the CA reply back into warehouse.p12. Java 7 PKCS12
REM keystores cannot store trusted-CA entries, so that path fails with
REM "Failed to establish chain from reply". Instead we build nginx's two PEM
REM files directly:
REM   privkey.pem  - the private key already inside warehouse.p12 (the CA signed
REM                  THIS key, so it matches the issued cert).
REM   fullchain.pem- the issued leaf cert followed by the Dogana CA cert.
REM 1) private key -> privkey.pem  (CertToPem also writes a fullchain.pem we overwrite next)
"%JDK_HOME%\bin\java.exe" -cp "%APP_DIR%\ssl-tools" CertToPem "%SERVER_P12%" changeit warehouse "%SSL_DIR%"
if errorlevel 1 goto :nginx_certfail
REM 2) fullchain.pem = issued leaf + Dogana CA. keytool -printcert -rfc reads DER OR
REM    PEM input and always emits PEM, so it normalises both files for us.
"%JDK_HOME%\bin\keytool.exe" -printcert -rfc -file "%SERVER_CERT_FILE%" > "%SSL_DIR%\fullchain.pem" 2>nul
if errorlevel 1 (
    echo ERROR: could not read the issued cert "%SERVER_CERT_FILE%".
    echo        Make sure it is the Base-64 reply to ssl\warehouse.csr from THIS server.
    goto :nginx_certfail
)
if exist "%CA_CERT_FILE%" "%JDK_HOME%\bin\keytool.exe" -printcert -rfc -file "%CA_CERT_FILE%" >> "%SSL_DIR%\fullchain.pem" 2>nul
goto :nginx_havecert

:nginx_havecert

REM 3b) Make the cert easy to trust on clients, and trust it on THIS machine now
copy /y "%SSL_DIR%\fullchain.pem" "%APP_DIR%\warehouse-trust.crt" >nul 2>&1
echo Trusting the certificate on this server (removes the warning here)...
certutil -addstore -f Root "%SSL_DIR%\fullchain.pem" >nul 2>&1
REM Also trust the Dogana internal CA itself, so any cert it issues is trusted here.
if exist "%CA_CERT_FILE%" certutil -addstore -f Root "%CA_CERT_FILE%" >nul 2>&1
echo Distribute this file to staff PCs (or push via Group Policy) to remove the warning:
echo     %APP_DIR%\warehouse-trust.crt

REM 4) Write nginx.conf from the template (fill in IP / backend / cert dir)
echo Writing nginx configuration...
powershell -NoProfile -Command "(Get-Content '%APP_DIR%\nginx\nginx.conf.template') -replace '__SERVER_NAME__','%SERVER_IP%' -replace '__BACKEND__','%BACKEND_HOSTPORT%' -replace '__SSL_DIR__','%SSL_DIR_FWD%' | Set-Content '%NGINX_HOME%\conf\nginx.conf' -Encoding ASCII"

REM 5) Open the Windows firewall for 80 and 443 (needs admin to take effect)
netsh advfirewall firewall delete rule name="Warehouse HTTP 80" >nul 2>&1
netsh advfirewall firewall delete rule name="Warehouse HTTPS 443" >nul 2>&1
netsh advfirewall firewall add rule name="Warehouse HTTP 80" dir=in action=allow protocol=TCP localport=80 >nul
netsh advfirewall firewall add rule name="Warehouse HTTPS 443" dir=in action=allow protocol=TCP localport=443 >nul

REM 6) Validate the config and (re)start nginx
pushd "%NGINX_HOME%"
"%NGINX_EXE%" -t
if errorlevel 1 goto :nginx_testfail
tasklist /fi "imagename eq nginx.exe" 2>nul | find /i "nginx.exe" >nul
if errorlevel 1 goto :nginx_start
echo Reloading nginx...
"%NGINX_EXE%" -s reload
goto :nginx_started
:nginx_start
echo Starting nginx...
start "" "%NGINX_EXE%"
:nginx_started
popd
echo nginx reverse proxy is running.
goto :nginx_done

:nginx_dlfail
echo ERROR: Could not download nginx. Check internet access on this server.
goto :nginx_done
:nginx_certfail
echo ERROR: Certificate generation/conversion failed. Proxy not started.
goto :nginx_done
:nginx_testfail
echo ERROR: nginx config test failed - see messages above. Proxy not started.
popd
goto :nginx_done
:nginx_skip
echo Not used in this mode - GlassFish serves HTTPS itself. Skipping.

:nginx_done
echo.

echo ==========================================================
echo SUCCESS: Application compiled and deployed successfully!
echo Context Root: %CONTEXT_ROOT%
echo Access URL: https://%SERVER_IP%%CONTEXT_ROOT%/
if /i "%SERVE_MODE%"=="glassfish" (
    echo Serving:    GlassFish directly on %HTTPS_PORT% ^(no proxy^); %HTTP_PORT% redirects to HTTPS.
) else (
    echo Serving:    nginx on %HTTPS_PORT% -^> GlassFish on %BACKEND_HOSTPORT%.
)
echo.
REM This script starts GlassFish inside the CURRENT logon session, so it dies
REM when that account logs off. install-services.bat registers it as a boot-time
REM SYSTEM service and is what keeps the app up between deploys.
sc.exe query "domain1" >nul 2>&1
if errorlevel 1 (
    echo ----------------------------------------------------------
    echo ACTION REQUIRED: GlassFish is NOT installed as a Windows service,
    echo so it will stop when this account logs off. Run ONCE as Administrator:
    echo     %APP_DIR%\install-services.bat
    echo ----------------------------------------------------------
)
if /i "%CERT_MODE%"=="cacert" (
    echo Cert: issued by the Dogana CA - trusted on domain PCs, no browser warning.
) else (
    REM The parentheses MUST stay escaped: this echo is inside an IF block, and an
    REM unescaped ')' closes the block early - the rest of the line then runs as a
    REM command and cmd reports ". was unexpected at this time."
    echo Note: browsers show a one-time "not trusted" warning ^(self-signed, IP-only^).
    echo       Switch to a Dogana-CA cert to remove it - see ssl\README.md.
)
echo.
echo Verify it for real ^(this also proves TLS 1.2 works^):
echo     powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\healthcheck.ps1"
echo ==========================================================
REM deploy-full.bat runs this script through a pipe (to tee the output into
REM deploy-full.log), and a pipe throws the exit code away. Leave the real
REM result in a file it can read back. Harmless when run standalone.
REM Written as (echo 0)>file, not echo 0>file: the latter is parsed as a
REM redirect of handle 0 and writes nothing.
if defined WHS_RC_FILE (
    (echo 0)>"%WHS_RC_FILE%"
)
if not defined WHS_NOPAUSE pause
exit /b 0

:error
echo ==========================================================
echo ERROR: Deployment process failed. Please check the logs.
echo ==========================================================
if defined WHS_RC_FILE (
    (echo 1)>"%WHS_RC_FILE%"
)
if not defined WHS_NOPAUSE pause
exit /b 1


REM ##########################################################################
REM  SUBROUTINES - starting the domain, and proving that it started.
REM
REM  Everything below exists because of one failure mode: asadmin reports a
REM  failed start as "Attempting to start domain1.... Please look at the server
REM  log for more details" and then EXITS ZERO. The script used to believe it,
REM  carry on, and produce three pages of "Remote server does not listen for
REM  requests on [localhost:4848]" - none of which name the actual problem.
REM ##########################################################################

REM --------------------------------------------------------------------------
REM  :migrate_domain_xml - replace the old server's paths / DB IP, but only if
REM  they are actually in the file, and never leave a half-written file behind.
REM  It also repairs the damage the previous version of this script could do:
REM  if domain.xml no longer parses, the backup taken on the first ever run is
REM  put back, because a domain with no valid config cannot start at all.
REM --------------------------------------------------------------------------
:migrate_domain_xml
powershell -NoProfile -Command ^
    "$xml = '%DOMAIN_XML%'; $bak = $xml + '.bak'; " ^
    "$old = 'D:\glassfish from old server\Glassfish_3.1.2.2\glassfish3'; " ^
    "$new = '%GLASSFISH_ROOT%'; " ^
    "try { [xml](Get-Content $xml -Raw -ErrorAction Stop) | Out-Null; $ok = $true } catch { $ok = $false }; " ^
    "if (-not $ok) { " ^
    "  Write-Host '   domain.xml is CORRUPT (it does not parse as XML).'; " ^
    "  if (Test-Path $bak) { " ^
    "    Copy-Item $bak $xml -Force; " ^
    "    Write-Host '   Restored it from domain.xml.bak - the domain can start again.'; " ^
    "  } else { " ^
    "    Write-Host '   No domain.xml.bak to restore from. GlassFish will not start until this is fixed by hand.'; " ^
    "    exit 1; " ^
    "  } " ^
    "}; " ^
    "$text = Get-Content $xml -Raw; " ^
    "if (($text -notmatch [regex]::Escape($old)) -and ($text -notmatch '10\.10\.10\.210')) { " ^
    "  Write-Host '   domain.xml already points at this server - nothing to change.'; " ^
    "  exit 0; " ^
    "}; " ^
    "Write-Host '   Migrating old server paths / database IP in domain.xml...'; " ^
    "$fixed = $text -replace [regex]::Escape($old), $new -replace '10\.10\.10\.210', '10.10.10.91'; " ^
    "$tmp = $xml + '.new'; " ^
    "Set-Content -Path $tmp -Value $fixed -Encoding UTF8; " ^
    "try { [xml](Get-Content $tmp -Raw -ErrorAction Stop) | Out-Null } " ^
    "catch { Write-Host '   The rewritten domain.xml does not parse - keeping the original untouched.'; Remove-Item $tmp -Force; exit 1 }; " ^
    "Move-Item $tmp $xml -Force; " ^
    "Write-Host '   Paths and database IP updated.'"
if errorlevel 1 (
    echo ERROR: domain.xml could not be brought into a usable state - see above.
    exit /b 1
)
exit /b 0

REM --------------------------------------------------------------------------
REM  :is_port_open <port>   errorlevel 0 = something is listening, 1 = free.
REM  TcpClient rather than Get-NetTCPConnection: the latter does not exist on
REM  Windows Server 2008 R2 / PowerShell 2.0, where it fails silently and makes
REM  every port look free.
REM --------------------------------------------------------------------------
:is_port_open
powershell -NoProfile -Command ^
    "$c = New-Object Net.Sockets.TcpClient; " ^
    "try { $c.Connect('127.0.0.1', %1); if ($c.Connected) { $c.Close(); exit 0 } } catch { }; " ^
    "exit 1"
exit /b !errorlevel!

REM --------------------------------------------------------------------------
REM  :port_holder <port>  - print which program holds a port, by name not PID.
REM  Used only in the failure report, where "nginx.exe holds 443" is the whole
REM  answer and a bare PID is another lookup for whoever is reading.
REM --------------------------------------------------------------------------
:port_holder
powershell -NoProfile -Command ^
    "$p = %1; " ^
    "$line = @(netstat -ano -p TCP | Select-String (':' + $p + '\s') | Select-String 'LISTENING') | Select-Object -First 1; " ^
    "if (-not $line) { Write-Host ('   port ' + $p + ' : free'); exit 0 }; " ^
    "$procId = ($line.ToString().Trim() -split '\s+')[-1]; " ^
    "$proc = Get-Process -Id $procId -ErrorAction SilentlyContinue; " ^
    "if ($proc) { Write-Host ('   port ' + $p + ' : held by ' + $proc.ProcessName + '.exe (PID ' + $procId + ')') } " ^
    "else { Write-Host ('   port ' + $p + ' : held by PID ' + $procId) }"
exit /b 0

REM --------------------------------------------------------------------------
REM  :free_glassfish_ports - in glassfish mode the domain binds 80 and 443
REM  itself, so nginx must be gone before it starts.
REM --------------------------------------------------------------------------
:free_glassfish_ports
tasklist /fi "imagename eq nginx.exe" 2>nul | find /i "nginx.exe" >nul
if not errorlevel 1 (
    echo Stopping nginx - GlassFish takes over ports %HTTP_PORT% and %HTTPS_PORT% in this mode...
    if exist "%NGINX_HOME%\nginx.exe" "%NGINX_HOME%\nginx.exe" -p "%NGINX_HOME%" -s stop >nul 2>&1
    taskkill /f /im nginx.exe >nul 2>&1
)
REM Drop the boot task too, or nginx comes back at the next reboot and steals
REM :443 before GlassFish is up. install-services.bat does the same.
schtasks /query /tn "Warehouse nginx" >nul 2>&1
if not errorlevel 1 (
    echo Removing the "Warehouse nginx" boot task ^(not used in glassfish mode^)...
    schtasks /delete /tn "Warehouse nginx" /f >nul 2>&1
)
exit /b 0

REM --------------------------------------------------------------------------
REM  :stop_running_domain - leave the machine with no domain running and no
REM  stale lock files, whichever state it was in when we arrived.
REM --------------------------------------------------------------------------
:stop_running_domain
call :is_port_open 4848
if errorlevel 1 (
    echo No GlassFish instance is answering on the admin port ^(4848^).
    goto :srd_clean_locks
)
echo A GlassFish instance is already running - stopping it cleanly...
call "%ASADMIN%" stop-domain domain1
call :is_port_open 4848
if errorlevel 1 (
    echo Stopped.
    goto :srd_clean_locks
)
echo The clean stop did not release port 4848 - forcing it.
powershell -NoProfile -Command ^
    "$line = @(netstat -ano -p TCP | Select-String ':4848\s' | Select-String 'LISTENING') | Select-Object -First 1; " ^
    "if ($line) { " ^
    "  $procId = ($line.ToString().Trim() -split '\s+')[-1]; " ^
    "  Write-Host ('   killing PID ' + $procId + ' ...'); " ^
    "  Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue; " ^
    "  Start-Sleep -s 3; " ^
    "}"

:srd_clean_locks
REM A domain that was killed rather than stopped leaves config\pid behind. On
REM the next start GlassFish sees it, decides an instance is already running,
REM and refuses - reporting only "Please look at the server log".
set "GF_DOMAIN_DIR=%GLASSFISH_ROOT%\glassfish\domains\domain1"
if exist "%GF_DOMAIN_DIR%\config\pid" (
    echo Removing a stale pid file left by a previous forced shutdown...
    del /f /q "%GF_DOMAIN_DIR%\config\pid" >nul 2>&1
)
exit /b 0

REM --------------------------------------------------------------------------
REM  :do_start_domain - one start attempt, service first, asadmin otherwise.
REM --------------------------------------------------------------------------
REM  The service is tried first but is NOT trusted: on this server it has been
REM  found still pointing at 'D:\glassfish from old server', where Start-Service
REM  fails instantly and nothing else ever attempts the start. So check whether
REM  it actually reached Running, and fall back to asadmin when it did not.
:do_start_domain
powershell -NoProfile -Command ^
    "$svc = Get-Service | Where-Object { $_.Name -like '*domain1*' -or $_.DisplayName -like '*glassfish*' } | Select-Object -First 1; " ^
    "$started = $false; " ^
    "if ($svc -and '%IS_ADMIN%' -eq '1') { " ^
    "  Write-Host ('Starting GlassFish via service: ' + $svc.DisplayName); " ^
    "  Start-Service -Name $svc.Name -ErrorAction SilentlyContinue; " ^
    "  Start-Sleep -s 2; " ^
    "  $svc.Refresh(); " ^
    "  if ($svc.Status -eq 'Running') { $started = $true } " ^
    "  else { Write-Host '   the service did not start - falling back to asadmin' } " ^
    "} " ^
    "if (-not $started) { " ^
    "  Write-Host 'Starting GlassFish domain domain1...'; " ^
    "  & '%ASADMIN%' start-domain domain1; " ^
    "}"
exit /b 0

REM --------------------------------------------------------------------------
REM  :wait_for_admin_port <seconds> - poll 4848 until it answers.
REM  Polling, not a fixed sleep: on this box a cold start is anywhere between
REM  20 and 90 seconds depending on how much of it comes off the page file.
REM --------------------------------------------------------------------------
:wait_for_admin_port
set /a WFA_LEFT=%1
:wfa_loop
call :is_port_open 4848
if not errorlevel 1 (
    echo.
    exit /b 0
)
set /a WFA_LEFT-=3
if !WFA_LEFT! LEQ 0 (
    echo.
    exit /b 1
)
REM 3-second ticks, printed on one line so a slow start looks like progress
REM rather than a hang.
powershell -NoProfile -Command "Start-Sleep -s 3" >nul
<nul set /p "=."
goto :wfa_loop

REM --------------------------------------------------------------------------
REM  :start_domain_verified - start it, and if it does not come up, fix the two
REM  things that actually cause it on this machine and try once more.
REM --------------------------------------------------------------------------
:start_domain_verified
echo Starting GlassFish domain...
call :do_start_domain
echo Waiting for the admin port to answer
call :wait_for_admin_port 90
if not errorlevel 1 (
    echo GlassFish is up - admin port 4848 is answering.
    exit /b 0
)

echo.
echo WARNING: domain1 did not come up within 90 seconds.
echo          Clearing the two things that usually cause this, then retrying.
call :clear_start_blockers
call :do_start_domain
echo Waiting for the admin port to answer
call :wait_for_admin_port 120
if not errorlevel 1 (
    echo GlassFish is up - admin port 4848 is answering.
    exit /b 0
)

call :report_start_failure
exit /b 1

REM --------------------------------------------------------------------------
REM  :clear_start_blockers
REM   1. osgi-cache - GlassFish 3.1.2 caches the OSGi bundle state here. A
REM      forced kill can leave it half-written, and every later start then dies
REM      early with a Felix error. Deleting it is safe: it is rebuilt on the
REM      next start (which is why that start is slower).
REM   2. an -Xmx the machine cannot actually give out. The JVM refuses to start
REM      at all with "Could not reserve enough space for object heap", and the
REM      usable ceiling is set by RAM *plus page file* - so a big heap that
REM      worked before can stop working after someone shrinks the page file.
REM      Test the configured value for real with java -version and step it down
REM      until one succeeds.
REM --------------------------------------------------------------------------
:clear_start_blockers
set "GF_DOMAIN_DIR=%GLASSFISH_ROOT%\glassfish\domains\domain1"
if exist "%GF_DOMAIN_DIR%\osgi-cache" (
    echo   - clearing the OSGi bundle cache ^(rebuilt on the next start^)...
    rmdir /s /q "%GF_DOMAIN_DIR%\osgi-cache" >nul 2>&1
)
if exist "%GF_DOMAIN_DIR%\config\pid" del /f /q "%GF_DOMAIN_DIR%\config\pid" >nul 2>&1

echo   - checking the configured heap size is one this machine can allocate...
REM Delegated to jvm-size.ps1 so this repair and the value written by
REM install-services.bat come from the SAME probe. They used to disagree: this
REM step lowered the heap in domain.xml, then install-services.bat - which runs
REM afterwards - put its RAM-derived guess straight back, and the next start
REM failed exactly the same way. jvm-size.ps1 edits domain.xml rather than
REM calling asadmin because the domain is down here and create-jvm-options is a
REM remote command that needs the admin port.
if not exist "%APP_DIR%\jvm-size.ps1" (
    echo     jvm-size.ps1 not found - skipping the heap check.
    exit /b 0
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\jvm-size.ps1" ^
    -JavaExe "%JDK_HOME%\bin\java.exe" -DomainXml "%DOMAIN_XML%" -Apply >nul
exit /b 0

REM --------------------------------------------------------------------------
REM  :report_start_failure - the domain is down and we are giving up. Print the
REM  evidence here, in this window, instead of telling the reader to go and
REM  find server.log themselves.
REM --------------------------------------------------------------------------
:report_start_failure
set "GF_LOG=%GLASSFISH_ROOT%\glassfish\domains\domain1\logs\server.log"
echo.
echo ==========================================================
echo ERROR: GlassFish domain1 did not start.
echo ==========================================================
echo.
echo Nothing else in this script can work until it does - every asadmin
echo command talks to localhost:4848 - so the deployment stops here.
echo.
echo --- who is holding the ports GlassFish needs ---
call :port_holder 4848
call :port_holder 8080
if /i "%SERVE_MODE%"=="glassfish" call :port_holder %HTTP_PORT%
if /i "%SERVE_MODE%"=="glassfish" call :port_holder %HTTPS_PORT%
echo.
if not exist "%GF_LOG%" (
    echo --- server.log was not found at %GF_LOG% ---
    goto :rsf_checklist
)
echo --- last 40 lines of server.log ---
REM Select-Object -Last rather than Get-Content -Tail: -Tail needs PowerShell 3.
powershell -NoProfile -Command ^
    "Get-Content '%GF_LOG%' | Select-Object -Last 40"
echo --- end of server.log extract ---
echo.
echo Lines worth looking for above:
echo    'Address already in use'  -^> another program holds the port; see the list above
echo    'Could not reserve enough space for object heap' -^> heap too big for RAM+page file
echo    'java.net.BindException'  -^> same as the first one
echo    'Keystore was tampered'   -^> keystore password wrong or keystore.jks corrupt

:rsf_checklist
echo.
echo Try, in this order:
echo    1. Re-run this script. It clears the stale pid/osgi-cache on a retry and
echo       that alone fixes a domain killed by a crash or a hard reboot.
echo    2. Fix the page file if STEP 0 warned about it: sysdm.cpl -^> Advanced -^>
echo       Performance -^> Settings -^> Advanced -^> Virtual memory -^> system managed.
echo    3. Start it by hand to see the error live:
echo          "%ASADMIN%" start-domain --verbose domain1
echo    4. If the keystore is the problem, restore the backup this script keeps -
echo       copy keystore.jks.bak over keystore.jks in:
echo          %GLASSFISH_ROOT%\glassfish\domains\domain1\config
echo.
exit /b 0
