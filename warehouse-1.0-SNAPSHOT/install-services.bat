@echo off
setlocal enabledelayedexpansion

REM ==========================================================================
REM   Warehouse - make the app survive logoff, reboots and crashes
REM ==========================================================================
REM   RUN THIS ONCE, AS ADMINISTRATOR. Re-running it is safe (idempotent).
REM
REM   WHY THIS EXISTS
REM   ---------------
REM   deploy.bat starts GlassFish with "asadmin start-domain", which makes it
REM   belong to the INTERACTIVE LOGON SESSION of whoever ran the script. Windows
REM   kills that whole session's processes when the account logs off - and an RDP
REM   session that is merely disconnected gets logged off automatically by the
REM   usual domain idle-session policy, typically after a couple of days. That is
REM   the "it stops after 2-3 days" symptom. It was never nginx's doing.
REM
REM   After this script runs:
REM     * GlassFish is a real Windows service, starting at boot as SYSTEM, and
REM       auto-restarting if the JVM dies. If the service cannot be registered
REM       it falls back to a boot-time scheduled task, which achieves the same
REM       thing - the app must not stay tied to a logon session either way.
REM     * A watchdog task checks it every 5 minutes and revives it if it died.
REM     * The domain is left RUNNING. Earlier versions stopped it to install the
REM       service and, if that step failed, never started it again - which looks
REM       from outside like "the deploy worked but the site returns 502".
REM   None of that is tied to anybody being logged in.
REM ==========================================================================

REM --- must match deploy.bat ---
set "GLASSFISH_ROOT=C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3"
set "JDK_HOME=C:\Program Files\Java\jdk1.7.0_45"
set "NGINX_HOME=C:\nginx"
set "DOMAIN=domain1"
REM Must match SERVE_MODE in deploy.bat: glassfish = GlassFish serves TLS itself
REM and there is no nginx to keep alive; nginx = the old reverse-proxy layout.
set "SERVE_MODE=glassfish"

set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"

REM Same fallback as deploy.bat: if this script sits NEXT TO the application
REM folder rather than inside it, watchdog.bat is one level down.
if not exist "%APP_DIR%\watchdog.bat" (
    for /d %%D in ("%APP_DIR%\*") do (
        if not defined FOUND_APP_DIR if exist "%%~fD\watchdog.bat" set "FOUND_APP_DIR=%%~fD"
    )
)
if defined FOUND_APP_DIR set "APP_DIR=!FOUND_APP_DIR!"

echo ==========================================================
echo   Installing Warehouse as always-on Windows services
echo ==========================================================
echo.

net session >nul 2>&1
if not %errorlevel% == 0 (
    echo ERROR: This script must be run as Administrator.
    echo        Right-click install-services.bat -^> "Run as administrator".
    goto :error
)

set "ASADMIN=%GLASSFISH_ROOT%\glassfish\bin\asadmin.bat"
if not exist "%ASADMIN%" set "ASADMIN=%GLASSFISH_ROOT%\bin\asadmin.bat"
if not exist "%ASADMIN%" (
    echo ERROR: asadmin.bat not found under %GLASSFISH_ROOT%
    goto :error
)

REM ==========================================================================
echo [1/6] Hardening the GlassFish JVM ^(heap + PermGen^)...
REM ==========================================================================
REM IMPORTANT: create-jvm-options and set-log-attributes are REMOTE asadmin
REM commands - they talk to the running DAS on port 4848. With the domain down
REM they just fail. So make sure it is up before we touch anything.
REM Every asadmin invocation goes through CALL. asadmin.bat is itself a batch
REM file, and one batch file running another WITHOUT "call" hands over control
REM permanently - the rest of this script never runs. That looks exactly like
REM the script silently quitting straight after the [1/6] line.
call "%ASADMIN%" list-jvm-options >nul 2>&1
if errorlevel 1 (
    echo    Domain is not responding - starting it first...
    call "%ASADMIN%" start-domain %DOMAIN% >nul 2>&1
    call "%ASADMIN%" list-jvm-options >nul 2>&1
    if errorlevel 1 call :repair_heap_and_retry
)
call "%ASADMIN%" list-jvm-options >nul 2>&1
if errorlevel 1 (
    echo    ERROR: cannot reach the GlassFish admin port. Start the domain by
    echo           hand to see why:
    echo             "%ASADMIN%" start-domain --verbose %DOMAIN%
    echo           then re-run this script.
    goto :error
)

REM GlassFish 3.1.2.2 runs on JDK 7, where classes live in PermGen. Every
REM redeploy leaks the old app's classloader into PermGen, and the stock
REM MaxPermSize=192m fills up after a handful of redeploys -> OutOfMemoryError:
REM PermGen space -> the domain stops serving. Raising it does not fix the leak,
REM but it turns "dies this week" into "fine until the next planned restart".
REM
REM Size the heap by ASKING THE JVM whether it can allocate it, not by guessing
REM from total RAM. This script used to derive -Xmx from TotalVisibleMemorySize
REM alone, which put -Xmx1536m on this server and left GlassFish unable to boot
REM at all ("Could not reserve enough space for object heap"). Total RAM is the
REM wrong input: the reservation is charged against RAM + page file, and a
REM 32-bit JVM cannot go much past ~1.4 GB regardless. jvm-size.ps1 probes with
REM java -Xmx<N>m -version and steps down until one really starts.
REM
REM This runs even when deploy-full.bat already passed values down - it is
REM passed as -MaxMb, so this can only lower the inherited number, never raise
REM it. That matters because THIS script writes the final value: it runs after
REM deploy.bat, so anything deploy.bat lowered gets overwritten here, and
REM without its own probe it would put the unbootable heap straight back.
set "WHS_SIZER=%APP_DIR%\jvm-size.ps1"
if not exist "!WHS_SIZER!" goto :jvm_sized
set "WHS_MAX_ARG=0"
if defined WHS_XMX_MB set "WHS_MAX_ARG=!WHS_XMX_MB!"
REM Only the result line is on stdout (jvm-size.ps1 puts everything else on
REM stderr, so it still reaches the screen without landing here). The WHSJVM
REM token is checked anyway: a stray line must not be parsed as a heap size.
for /f "usebackq tokens=1,2,3" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass ^
    -File "!WHS_SIZER!" -JavaExe "%JDK_HOME%\bin\java.exe" -MaxMb !WHS_MAX_ARG!`) do (
    if "%%a"=="WHSJVM" (
        set "WHS_XMX_MB=%%b"
        set "WHS_PERM_MB=%%c"
    )
)
:jvm_sized
REM Safe floors, in case the sizing block above was skipped or the FOR failed:
REM an empty !WHS_XMX_MB! would build the nonsense option "-Xmxm".
if not defined WHS_XMX_MB set "WHS_XMX_MB=768"
if not defined WHS_PERM_MB set "WHS_PERM_MB=384"
echo    Sizing the JVM for this machine: Xmx=!WHS_XMX_MB!m, MaxPermSize=!WHS_PERM_MB!m

REM Remove every existing -Xmx / -XX:MaxPermSize first. Two reasons:
REM   1. create-jvm-options ADDS an option, it does not replace one. Leaving the
REM      stock -Xmx512m in place means the JVM is launched with two -Xmx flags
REM      and which one wins depends on the order they sit in domain.xml.
REM   2. asadmin treats ':' as its own list separator, so a colon inside an
REM      option MUST be escaped as '\:' - the unescaped form silently deletes or
REM      creates the wrong thing. Doing this from PowerShell keeps the escaping
REM      readable and lets us match options we did not write.
powershell -NoProfile -Command ^
    "$a = '%ASADMIN%'; " ^
    "$old = @(& $a list-jvm-options 2>$null | Where-Object { $_ -match '^\s*-Xmx' -or $_ -match 'MaxPermSize' }); " ^
    "foreach ($o in $old) { $e = $o.Trim() -replace ':', '\:'; & $a delete-jvm-options $e | Out-Null }; " ^
    "& $a create-jvm-options ('-Xmx!WHS_XMX_MB!m') | Out-Null; " ^
    "& $a create-jvm-options ('-XX\:MaxPermSize=!WHS_PERM_MB!m') | Out-Null; " ^
    "& $a create-jvm-options '-XX\:+HeapDumpOnOutOfMemoryError' | Out-Null"

REM Verify rather than trust: a JVM option that did not apply is exactly the
REM kind of thing that looks fine now and bites in a month.
echo    Current settings:
call "%ASADMIN%" list-jvm-options 2>nul | findstr /i "MaxPermSize Xmx HeapDump"
call "%ASADMIN%" list-jvm-options 2>nul | findstr /i "Xmx!WHS_XMX_MB!m" >nul
if errorlevel 1 (
    echo    WARNING: -Xmx!WHS_XMX_MB!m is NOT in the list above. Set it by hand in
    echo             the admin console ^(Configurations -^> server-config -^> JVM Settings^).
)
echo.
echo    NOTE: these take effect at the next domain START, which happens in
echo          step [3/5] below - not on the instance running right now.
echo.

REM ==========================================================================
echo [2/6] Capping GlassFish log growth...
REM ==========================================================================
REM server.log with no history cap is the other classic way this box dies:
REM the disk fills, GlassFish can no longer write, and the app stops. Rotate at
REM 20 MB and keep only the last 10 files.
call "%ASADMIN%" set-log-attributes com.sun.enterprise.server.logging.GFFileHandler.rotationLimitInBytes=20000000 >nul 2>&1
if errorlevel 1 echo    WARNING: could not set the log rotation size.
call "%ASADMIN%" set-log-attributes com.sun.enterprise.server.logging.GFFileHandler.maxHistoryFiles=10 >nul 2>&1
if errorlevel 1 echo    WARNING: could not set the log history cap.
echo    server.log rotates at 20 MB, keeps 10 files.
echo.

REM ==========================================================================
echo [3/6] Installing GlassFish as a Windows service...
REM ==========================================================================
REM Stop any copy that is currently running in somebody's console session,
REM otherwise the service cannot bind the ports.
REM
REM ANYTHING THAT FAILS FROM HERE ON MUST STILL LEAVE THE DOMAIN RUNNING.
REM The stop below is the single most dangerous line in this script: it takes
REM the site down on purpose, and step [6/6] is what guarantees it comes back.
call "%ASADMIN%" stop-domain %DOMAIN% >nul 2>&1

REM --force is not optional on a re-run. GlassFish leaves domain1Service.* in
REM the domain's bin folder, and create-service refuses outright when it finds
REM them - "Services have already been created" - even when no Windows service
REM is actually registered any more (deleted by hand, or created against the old
REM D:\ install path). Without --force the second run of this script silently
REM leaves the machine with no service at all.
call "%ASADMIN%" create-service --force --name %DOMAIN% %DOMAIN%
if errorlevel 1 (
    echo    NOTE: create-service reported an error - checking whether the service
    echo          exists anyway.
)

REM Find whatever the service actually got called and make it boot-start and
REM self-heal. GlassFish names it "domain1" or "domain1GlassFishServer"
REM depending on the build, so discover it rather than assume. Match on the
REM domain name alone: the service's binPath points at domain1Service.exe, and
REM on an install whose path does not contain the word "glassfish" - this one
REM lives under ...\Glassfish_3.1.2.2\..., capitalised, but a renamed folder
REM would not - an additional '*glassfish*' test finds nothing and the script
REM concludes there is no service.
set "GF_SVC="
for /f "usebackq delims=" %%S in (`powershell -NoProfile -Command ^
    "$s = Get-CimInstance Win32_Service | Where-Object { $_.PathName -like '*%DOMAIN%Service*' -or $_.Name -eq '%DOMAIN%' } | Select-Object -First 1; if ($s) { $s.Name }"`) do set "GF_SVC=%%S"

if defined GF_SVC (
    echo    Service: !GF_SVC!
    sc.exe config "!GF_SVC!" start= auto >nul
    REM Restart after 60s on the 1st, 2nd and subsequent failures; reset the
    REM failure counter once a day.
    sc.exe failure "!GF_SVC!" reset= 86400 actions= restart/60000/restart/60000/restart/60000 >nul
    echo    Set to Automatic start + auto-restart on failure.
    sc.exe start "!GF_SVC!" >nul 2>&1
    goto :svc_done
)

REM ---- Fallback: no service could be registered --------------------------
REM Do not stop here. The point of this script is that the app survives a
REM logoff, and a boot-time scheduled task running as SYSTEM achieves exactly
REM that - it is how nginx was already being kept alive. It has no
REM restart-on-crash, but the 5-minute watchdog in step [5/6] covers that.
echo    Could not register a Windows service. Falling back to a boot task.
schtasks /create /tn "Warehouse GlassFish" /f ^
    /tr "\"%ASADMIN%\" start-domain %DOMAIN%" ^
    /sc onstart /ru SYSTEM /rl HIGHEST >nul
if errorlevel 1 (
    echo    ERROR: the fallback boot task could not be registered either.
    echo           The app will run, but it will stop at the next logoff.
    echo           Check services.msc for a "%DOMAIN%" entry and set it to
    echo           Automatic by hand.
) else (
    echo    Task "Warehouse GlassFish" registered ^(starts the domain at boot as SYSTEM^).
)
:svc_done
echo.

REM ==========================================================================
echo [4/6] Reverse proxy ^(nginx^)...
REM ==========================================================================
if /i "%SERVE_MODE%"=="glassfish" goto :nginx_remove

REM nginx has no native Windows service mode. A SYSTEM scheduled task with an
REM "at startup" trigger is the equivalent that needs no extra downloads.
REM -p tells nginx its prefix, so it does not depend on the working directory.
if not exist "%NGINX_HOME%\nginx.exe" (
    echo    WARNING: %NGINX_HOME%\nginx.exe not found. Run deploy.bat first, then
    echo             re-run this script. Skipping nginx.
    goto :nginx_task_done
)
schtasks /create /tn "Warehouse nginx" /f ^
    /tr "\"%NGINX_HOME%\nginx.exe\" -p \"%NGINX_HOME%\"" ^
    /sc onstart /ru SYSTEM /rl HIGHEST >nul
if errorlevel 1 (
    echo    WARNING: could not register the nginx startup task.
) else (
    echo    Task "Warehouse nginx" registered ^(runs at boot as SYSTEM^).
)
goto :nginx_task_done

:nginx_remove
REM SERVE_MODE=glassfish. GlassFish itself now binds 80 and 443, so an nginx
REM starting at boot would either lose the race and log a bind error, or win it
REM and leave GlassFish unable to start - which is worse, because then the site
REM is down rather than merely proxied.
echo    SERVE_MODE=glassfish - GlassFish serves HTTPS directly, nginx not needed.
schtasks /query /tn "Warehouse nginx" >nul 2>&1
if not errorlevel 1 (
    schtasks /delete /tn "Warehouse nginx" /f >nul 2>&1
    echo    Removed the old "Warehouse nginx" boot task.
)
tasklist /fi "imagename eq nginx.exe" 2>nul | find /i "nginx.exe" >nul
if not errorlevel 1 (
    if exist "%NGINX_HOME%\nginx.exe" "%NGINX_HOME%\nginx.exe" -p "%NGINX_HOME%" -s stop >nul 2>&1
    taskkill /f /im nginx.exe >nul 2>&1
    echo    Stopped the running nginx so it cannot hold ports 80/443.
)
:nginx_task_done
echo.

REM ==========================================================================
echo [5/6] Installing the 5-minute watchdog...
REM ==========================================================================
if not exist "%APP_DIR%\watchdog.bat" (
    echo    WARNING: watchdog.bat is missing next to this script. Skipping.
) else (
    schtasks /create /tn "Warehouse Watchdog" /f ^
        /tr "\"%APP_DIR%\watchdog.bat\"" ^
        /sc minute /mo 5 /ru SYSTEM /rl HIGHEST >nul
    if errorlevel 1 (
        echo    WARNING: could not register the watchdog task.
    ) else (
        echo    Task "Warehouse Watchdog" registered ^(every 5 minutes as SYSTEM^).
    )
)
echo.

REM ==========================================================================
echo [6/6] Making sure the domain is actually up...
REM ==========================================================================
REM This step exists because of a real outage. Step [3/6] stops the domain to
REM free the ports; if anything after that failed, nothing started it again, and
REM the script still printed "DONE" - leaving a server that answers 502 (in
REM proxy mode) or nothing at all. Never finish this script with the app down.
REM
REM sc.exe start returns as soon as the service is told to start, and the domain
REM needs a while after that before the admin port answers, so poll instead of
REM checking once.
set "GF_UP="
for /l %%i in (1,1,12) do (
    if not defined GF_UP (
        call "%ASADMIN%" list-jvm-options >nul 2>&1
        if not errorlevel 1 set "GF_UP=1"
        if not defined GF_UP ping -n 6 127.0.0.1 >nul
    )
)
if defined GF_UP (
    echo    Domain %DOMAIN% is running and answering on the admin port.
    goto :up_done
)

echo    Domain is not answering yet - starting it directly...
call "%ASADMIN%" start-domain %DOMAIN%
call "%ASADMIN%" list-jvm-options >nul 2>&1
if errorlevel 1 (
    echo.
    echo    ERROR: the domain did not come up. The site is DOWN right now.
    echo           Look at the end of:
    echo             %GLASSFISH_ROOT%\glassfish\domains\%DOMAIN%\logs\server.log
    echo           The usual causes are a port already in use ^(nginx or IIS on 80
    echo           or 443^) and not enough free memory for the JVM heap.
    goto :error
)
echo    Domain started.
:up_done
echo.

echo ==========================================================
echo DONE.
echo.
echo Verify with:
echo    services.msc                     -^> GlassFish service = Running/Automatic
if /i "%SERVE_MODE%"=="nginx" echo    schtasks /query /tn "Warehouse nginx"
echo    schtasks /query /tn "Warehouse Watchdog"
echo.
echo Or run the full check, which also proves the site answers over TLS 1.2:
echo    powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\healthcheck.ps1"
echo.
echo Then do the real test: LOG OFF the server completely, wait a minute,
echo and browse to https://10.10.10.7/warehouse/ from another PC.
echo Before this change that is exactly what used to kill it.
echo ==========================================================
REM See the same note in deploy.bat: deploy-full.bat pipes this script's output
REM into its log, and a pipe discards the exit code.
if defined WHS_RC_FILE (
    (echo 0)>"%WHS_RC_FILE%"
)
if not defined WHS_NOPAUSE pause
exit /b 0

:error
echo.
echo ==========================================================
echo FAILED - see the message above.
echo ==========================================================
if defined WHS_RC_FILE (
    (echo 1)>"%WHS_RC_FILE%"
)
if not defined WHS_NOPAUSE pause
exit /b 1

REM ==========================================================================
REM  :repair_heap_and_retry - break the deadlock where a heap the machine
REM  cannot allocate stops the domain starting, and a stopped domain stops us
REM  fixing the heap.
REM
REM  Every asadmin JVM setting is a REMOTE command against port 4848, so with
REM  the domain down there is no supported way to lower -Xmx. Meanwhile the
REM  domain will not come up precisely BECAUSE -Xmx is too big:
REM      Error occurred during initialization of VM
REM      Could not reserve enough space for object heap
REM  Left alone that is terminal - the script used to just report "cannot reach
REM  the admin port" and stop, which describes the symptom and not the cause.
REM
REM  So edit domain.xml on disk instead (the one moment where that is the right
REM  tool rather than a shortcut), then start the domain again.
REM ==========================================================================
:repair_heap_and_retry
echo    The domain did not start. Checking whether its heap is too big for
echo    this machine to allocate...
if not exist "%APP_DIR%\jvm-size.ps1" (
    echo    jvm-size.ps1 not found - cannot check the heap automatically.
    exit /b 0
)
set "WHS_DOMAIN_XML=%GLASSFISH_ROOT%\glassfish\domains\domain1\config\domain.xml"
powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\jvm-size.ps1" ^
    -JavaExe "%JDK_HOME%\bin\java.exe" -DomainXml "!WHS_DOMAIN_XML!" -Apply >nul
REM A domain killed by a failed start leaves config\pid behind; the next start
REM then reports an instance is already running and refuses.
if exist "%GLASSFISH_ROOT%\glassfish\domains\domain1\config\pid" (
    del /f /q "%GLASSFISH_ROOT%\glassfish\domains\domain1\config\pid" >nul 2>&1
)
echo    Retrying the start with the corrected heap...
call "%ASADMIN%" start-domain %DOMAIN% >nul 2>&1
exit /b 0
