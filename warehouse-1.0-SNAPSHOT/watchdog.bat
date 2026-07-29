@echo off
setlocal enabledelayedexpansion

REM ==========================================================================
REM   Warehouse watchdog - run every 5 minutes by the "Warehouse Watchdog"
REM   scheduled task (installed by install-services.bat). Do not run by hand
REM   except to test it.
REM
REM   Deliberately conservative: it only ever restarts something that is
REM   genuinely NOT RUNNING. It never restarts a live process for being slow,
REM   because that is how watchdogs turn one bad request into a restart loop.
REM ==========================================================================

set "GLASSFISH_ROOT=C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3"
set "NGINX_HOME=C:\nginx"
set "DOMAIN=domain1"
REM Must match SERVE_MODE in deploy.bat. In glassfish mode there is no proxy to
REM revive - and reviving one would be actively harmful, because nginx would
REM take port 443 away from GlassFish at the next restart.
set "SERVE_MODE=glassfish"
set "LOG=%~dp0watchdog.log"

REM Keep the watchdog's own log from becoming the thing that fills the disk.
for %%A in ("%LOG%") do if %%~zA GTR 5000000 del "%LOG%" >nul 2>&1

call :log "--- check ---"

REM ---------------------------------------------------------------- nginx ---
if /i "%SERVE_MODE%"=="glassfish" goto :nginx_checked
tasklist /fi "imagename eq nginx.exe" 2>nul | find /i "nginx.exe" >nul
if errorlevel 1 (
    call :log "nginx is NOT running - starting it"
    if exist "%NGINX_HOME%\nginx.exe" (
        start "" /b "%NGINX_HOME%\nginx.exe" -p "%NGINX_HOME%"
    ) else (
        call :log "ERROR: %NGINX_HOME%\nginx.exe not found"
    )
) else (
    call :log "nginx OK"
)
:nginx_checked

REM ------------------------------------------------------------ GlassFish ---
REM Locate the service by path rather than by a guessed name.
REM
REM Service name, state, free RAM and the OOM check all come back from ONE
REM PowerShell process. This runs every 5 minutes forever on a memory-starved
REM VM, so starting four interpreters to answer four questions is a cost worth
REM avoiding. Fields are '-' when empty: FOR /F collapses consecutive
REM delimiters, so a blank field would shift every value after it.
REM PORT_UP is the one that actually answers "is the site up?". The service can
REM be Running while the JVM inside it has stopped serving, and with the
REM scheduled-task fallback there may be no service to ask about at all.
REM
REM TcpClient, not Get-NetTCPConnection: that cmdlet needs Server 2012+ and is
REM absent on Server 2008 R2, where it returns nothing instead of failing. Every
REM port then reads DOWN, and in the no-service branch below this watchdog would
REM run start-domain against a perfectly healthy server every five minutes.
REM deploy.bat reached the same conclusion - see :is_port_open there.
set "GF_SVC="
set "GF_STATE="
set "FREE_MB="
set "GF_OOM="
set "PORT_UP="
if /i "%SERVE_MODE%"=="glassfish" (set "SERVE_PORT=443") else (set "SERVE_PORT=8080")
for /f "usebackq tokens=1-5 delims=|" %%a in (`powershell -NoProfile -Command ^
    "$s = Get-CimInstance Win32_Service | Where-Object { $_.PathName -like '*%DOMAIN%Service*' -or $_.Name -eq '%DOMAIN%' } | Select-Object -First 1; " ^
    "$n = '-'; $st = '-'; if ($s) { $n = $s.Name; $st = $s.State }; " ^
    "$f = [int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024); " ^
    "$o = '-'; $lg = '%GLASSFISH_ROOT%\glassfish\domains\%DOMAIN%\logs\server.log'; " ^
    "if (Test-Path $lg) { if (@(Get-Content $lg -Tail 300 -EA SilentlyContinue | Select-String 'OutOfMemoryError' -SimpleMatch).Count) { $o = 'OOM' } }; " ^
    "$p = 'DOWN'; $tc = New-Object Net.Sockets.TcpClient; " ^
    "try { $tc.Connect('127.0.0.1', %SERVE_PORT%); if ($tc.Connected) { $p = 'UP' }; $tc.Close() } catch { }; " ^
    "$n + '|' + $st + '|' + $f + '|' + $o + '|' + $p"`) do (
    set "GF_SVC=%%a"
    set "GF_STATE=%%b"
    set "FREE_MB=%%c"
    set "GF_OOM=%%d"
    set "PORT_UP=%%e"
)
if "!GF_SVC!"=="-" set "GF_SVC="
if defined FREE_MB call :log "free RAM: !FREE_MB! MB"

REM Do not restart on this - an OOM that already happened is history, and a
REM watchdog that reacts to log content is a watchdog that restarts a healthy
REM server. Record it so the cause is in writing when someone asks why the app
REM went away, and let install-services.bat fix the sizing.
if /i "!GF_OOM!"=="OOM" call :log "WARNING: OutOfMemoryError in recent server.log - the JVM heap/PermGen is too small for this app; re-run install-services.bat"

if defined GF_SVC (
    if /i "!GF_STATE!"=="Running" (
        call :log "GlassFish service !GF_SVC! OK"
    ) else (
        call :log "GlassFish service !GF_SVC! is !GF_STATE! - starting it"
        sc.exe start "!GF_SVC!" >nul 2>&1
    )
    goto :gf_checked
)

REM No service: install-services.bat fell back to a boot scheduled task, or was
REM never run. Either way the port is the thing to look at.
REM
REM Deliberately NOT "is there a java.exe": this box runs other Java, and a
REM stray JVM would make the watchdog decide the site was fine while it was
REM down. A listening socket on the serving port is the closest cheap proxy for
REM "somebody can reach the app".
if /i "!PORT_UP!"=="UP" (
    call :log "no GlassFish service, but port %SERVE_PORT% is listening - OK"
) else (
    call :log "no GlassFish service and nothing on port %SERVE_PORT% - starting the domain directly"
    call "%GLASSFISH_ROOT%\glassfish\bin\asadmin.bat" start-domain %DOMAIN% >nul 2>&1
)
:gf_checked

endlocal
exit /b 0

:log
echo %date% %time% %~1>>"%LOG%"
exit /b 0
