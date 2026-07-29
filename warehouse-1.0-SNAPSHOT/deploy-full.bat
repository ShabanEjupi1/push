@echo off
setlocal enabledelayedexpansion

REM ==========================================================================
REM   WAREHOUSE - FULL SERVER DEPLOYMENT
REM ==========================================================================
REM   THIS IS THE ONLY SCRIPT YOU NEED TO RUN ON THE SERVER.
REM   Just double-click it. It asks for Administrator itself.
REM
REM   It runs, in the order that actually works:
REM
REM     1. deploy.bat           - compile the Java, redeploy the app to
REM                               GlassFish, install the TLS certificate into
REM                               the GlassFish keystore and put the HTTPS
REM                               listener on port 443.
REM     2. install-services.bat - register GlassFish so it runs as SYSTEM at
REM                               boot instead of inside your logon session, and
REM                               install the 5-minute watchdog.
REM     3. A health check       - prove the site actually answers, over TLS 1.2,
REM                               before it tells you the deployment succeeded.
REM
REM   The order matters: install-services.bat stops the domain to register the
REM   service, so it has to run after the code is deployed, not before.
REM
REM   Both sub-scripts are safe to re-run; running this repeatedly is fine.
REM ==========================================================================

REM These must match the values at the top of deploy.bat / install-services.bat.
set "SERVER_IP=10.10.10.7"
set "CONTEXT_ROOT=/warehouse"
set "GLASSFISH_ROOT=C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3"
set "JDK_HOME=C:\Program Files\Java\jdk1.7.0_45"
set "NGINX_HOME=C:\nginx"
REM Who terminates HTTPS: "glassfish" (default - GlassFish serves TLS on :443
REM itself, no proxy) or "nginx" (the old reverse-proxy layout). This value is
REM only used for the health check at the end; the sub-scripts each carry their
REM own SERVE_MODE, and all three must agree.
set "SERVE_MODE=glassfish"

set "APP_DIR=%~dp0"
if "%APP_DIR:~-1%"=="\" set "APP_DIR=%APP_DIR:~0,-1%"

REM If this script was copied next to the application folder instead of into
REM it, the other scripts are one level down. Find the folder that actually
REM holds them so the calls below still work.
if not exist "%APP_DIR%\deploy.bat" (
    for /d %%D in ("%APP_DIR%\*") do (
        if not defined FOUND_APP_DIR if exist "%%~fD\deploy.bat" set "FOUND_APP_DIR=%%~fD"
    )
)
if defined FOUND_APP_DIR (
    echo NOTE: Scripts found in !FOUND_APP_DIR! - using that folder.
    set "APP_DIR=!FOUND_APP_DIR!"
)

REM --------------------------------------------------------------------------
REM  Re-launch elevated if we are not already Administrator.
REM  install-services.bat cannot create services or SYSTEM scheduled tasks
REM  without this, and deploy.bat silently skips its firewall rules too.
REM --------------------------------------------------------------------------
net session >nul 2>&1
if not %errorlevel% == 0 (
    echo Requesting Administrator privileges...
    echo A UAC prompt will appear - choose Yes.
    powershell -NoProfile -Command ^
        "Start-Process -FilePath '%~f0' -WorkingDirectory '%APP_DIR%' -Verb RunAs"
    exit /b 0
)

echo ==========================================================
echo    WAREHOUSE - FULL DEPLOYMENT
echo    Server : %SERVER_IP%
echo    Folder : %APP_DIR%
echo    Started: %date% %time%
echo ==========================================================
echo.

REM Stop the sub-scripts blocking on "Press any key to continue".
set "WHS_NOPAUSE=1"

REM --------------------------------------------------------------------------
REM  Everything below is written to a log as well as to the screen.
REM  This exists because a deployment that dies mid-way - the window closing,
REM  a step being killed by the memory manager - leaves nothing to read
REM  afterwards, and the console scrollback is gone with it.
REM  WHS_RC_FILE: the steps run through a pipe (to tee), and a pipe discards
REM  the exit code, so each sub-script leaves its real result in this file.
REM --------------------------------------------------------------------------
set "WHS_LOG=%APP_DIR%\deploy-full.log"
set "WHS_RC_FILE=%TEMP%\whs_step_rc.txt"
echo. >>"%WHS_LOG%"
echo ===== run started %date% %time% ===== >>"%WHS_LOG%"
echo Full log of this run: %WHS_LOG%
echo.

REM --------------------------------------------------------------------------
echo ##########################################################
echo #  STEP 0 of 3 - Machine check ^(memory^)
echo ##########################################################
echo.
REM --------------------------------------------------------------------------
REM This VM has little RAM, and the JVM heap is the single biggest consumer on
REM it. Read the real numbers and size GlassFish from them, rather than
REM hardcoding a heap that happens to fit on a bigger machine. The values are
REM exported so install-services.bat uses exactly these.
set "TOTAL_MB="
set "FREE_MB="
set "PAGE_MB="
for /f "usebackq tokens=1,2,3" %%a in (`powershell -NoProfile -Command ^
    "$o = Get-CimInstance Win32_OperatingSystem; " ^
    "$pf = 0; foreach ($f in @(Get-CimInstance Win32_PageFileUsage)) { $pf += $f.AllocatedBaseSize }; " ^
    "'{0} {1} {2}' -f [int]($o.TotalVisibleMemorySize/1024), [int]($o.FreePhysicalMemory/1024), [int]$pf"`) do (
    set "TOTAL_MB=%%a"
    set "FREE_MB=%%b"
    set "PAGE_MB=%%c"
)
if not defined TOTAL_MB set "TOTAL_MB=4096"
if not defined FREE_MB set "FREE_MB=0"
if not defined PAGE_MB set "PAGE_MB=0"

REM The heap is not derived from TOTAL_MB. It is PROBED: jvm-size.ps1 runs
REM java -Xmx<N>m -version down a ladder and returns the largest heap that
REM really started. Sizing from total RAM is what produced -Xmx1536m here and
REM stopped GlassFish booting entirely - the limit is RAM + page file (and
REM ~1.4 GB on a 32-bit JVM), none of which total RAM tells you.
REM Flat, not nested: a FOR /F with caret-continued lines inside an IF block is
REM where batch parsing gets fragile, so jump over it instead of wrapping it.
set "WHS_XMX_MB="
set "WHS_PERM_MB="
if not exist "%APP_DIR%\jvm-size.ps1" goto :jvm_no_sizer
REM jvm-size.ps1 writes its findings to stderr and only the result line to
REM stdout, so this loop sees one line. The WHSJVM token is checked regardless.
for /f "usebackq tokens=1,2,3" %%a in (`powershell -NoProfile -ExecutionPolicy Bypass ^
    -File "%APP_DIR%\jvm-size.ps1" -JavaExe "%JDK_HOME%\bin\java.exe" -LogFile "%WHS_LOG%"`) do (
    if "%%a"=="WHSJVM" (
        set "WHS_XMX_MB=%%b"
        set "WHS_PERM_MB=%%c"
    )
)
goto :jvm_sized

:jvm_no_sizer
echo    NOTE: jvm-size.ps1 is missing - falling back to a RAM-based estimate.

:jvm_sized
REM Fallback only for a missing or failed sizer; deliberately the small,
REM always-safe pair rather than a guess scaled off total RAM.
if not defined WHS_XMX_MB set "WHS_XMX_MB=768"
if not defined WHS_PERM_MB set "WHS_PERM_MB=384"

echo    Physical RAM : !TOTAL_MB! MB total, !FREE_MB! MB free right now
echo    Page file    : !PAGE_MB! MB
echo    GlassFish    : will be set to Xmx=!WHS_XMX_MB!m, MaxPermSize=!WHS_PERM_MB!m ^(probed^)
echo.
REM Only the sub-scripts are teed into the log, so record this step's numbers
REM by hand. They are the first thing worth knowing when reading back a failure.
>>"%WHS_LOG%" echo STEP 0: RAM !TOTAL_MB! MB total / !FREE_MB! MB free, pagefile !PAGE_MB! MB, Xmx=!WHS_XMX_MB!m Perm=!WHS_PERM_MB!m

REM Deploying needs room for: the GlassFish JVM, a javac JVM (capped at 384m in
REM deploy.bat), and a few short-lived asadmin JVMs. Under roughly 700 MB free
REM one of them gets refused memory and the step dies with no useful message -
REM which is exactly what "the script stopped by itself" looks like.
if !FREE_MB! LSS 700 (
    echo    WARNING: only !FREE_MB! MB of RAM is free. A deployment can be killed
    echo             part way through at this level. Before continuing:
    echo               - close other applications and extra RDP sessions
    echo               - sign out any other logged-on users ^(query user^)
    echo               - or reboot the server and run this immediately after
    echo.
)
REM With this little RAM the page file is not optional - it is what absorbs the
REM overlap between the two JVMs during the compile+deploy window.
if !PAGE_MB! LSS 2048 (
    echo    WARNING: the page file is only !PAGE_MB! MB. On a !TOTAL_MB! MB machine
    echo             that makes memory allocation failures likely. Set it to
    echo             system-managed: sysdm.cpl -^> Advanced -^> Performance -^>
    echo             Settings -^> Advanced -^> Virtual memory -^> Change.
    echo.
)

REM --------------------------------------------------------------------------
echo ##########################################################
echo #  STEP 1 of 3 - Compile, deploy, certificate, nginx
echo ##########################################################
echo.
REM --------------------------------------------------------------------------
if not exist "%APP_DIR%\deploy.bat" (
    echo ERROR: deploy.bat not found next to this script.
    goto :failed
)
call :run_step "%APP_DIR%\deploy.bat"
if errorlevel 1 (
    echo.
    echo ERROR: deploy.bat failed - stopping here, the service step was skipped.
    echo.
    echo        deploy.bat stops at the FIRST thing that went wrong and prints
    echo        the reason, so read the error above rather than the last line.
    echo        In particular, if it stopped because domain1 would not start,
    echo        nothing after that point could have run: every other step talks
    echo        to the admin port, so they would all just report
    echo        "Remote server does not listen for requests on [localhost:4848]".
    echo.
    echo        NOTE: deploy.bat undeploys the old application BEFORE deploying
    echo        the new one, so depending on how far it got the app may now be
    echo        down. If it failed at the compile step the running app is fine;
    echo        if it failed at the deploy step it is not. Fix the error above
    echo        and re-run this script - that will redeploy it either way.
    goto :failed
)
echo.

REM --------------------------------------------------------------------------
echo ##########################################################
echo #  STEP 2 of 3 - Install always-on services + watchdog
echo ##########################################################
echo.
REM --------------------------------------------------------------------------
if not exist "%APP_DIR%\install-services.bat" (
    echo ERROR: install-services.bat not found next to this script.
    goto :failed
)
call :run_step "%APP_DIR%\install-services.bat"
if errorlevel 1 (
    echo.
    echo WARNING: install-services.bat failed. The application itself IS
    echo          deployed and running, but it is still tied to this logon
    echo          session - meaning it will stop when you log off. Fix the
    echo          error above and re-run.
    goto :failed
)
echo.

REM --------------------------------------------------------------------------
echo ##########################################################
echo #  STEP 3 of 3 - Health check
echo ##########################################################
echo.
REM --------------------------------------------------------------------------
REM All verification lives in healthcheck.ps1. Keeping it in a real .ps1 file
REM instead of inline PowerShell avoids cmd mangling the quotes and carets,
REM and lets you re-run the same check any time without redeploying.
REM GlassFish often needs a while after "started" before the app answers, so
REM the script polls rather than checking once.
if not exist "%APP_DIR%\healthcheck.ps1" (
    echo WARNING: healthcheck.ps1 not found - skipping verification.
    echo          Check manually: https://%SERVER_IP%%CONTEXT_ROOT%/
    goto :end
)

REM -LogFile appends the check's own output to the same log. It is passed as a
REM parameter instead of piping so the exit code survives - the pass/fail
REM verdict below depends on it.
powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\healthcheck.ps1" -ServerIp "%SERVER_IP%" -ContextRoot "%CONTEXT_ROOT%" -GlassfishRoot "%GLASSFISH_ROOT%" -NginxHome "%NGINX_HOME%" -ServeMode "%SERVE_MODE%" -LogFile "%WHS_LOG%"
if errorlevel 1 goto :health_bad

echo ==========================================================
echo   DEPLOYMENT SUCCESSFUL
echo ==========================================================
echo   The app is live at:  https://%SERVER_IP%%CONTEXT_ROOT%/
echo.
echo   It now runs as SYSTEM at boot, so it no longer stops when
echo   you log off. To PROVE that actually worked, do this once:
echo.
echo       1. Log off this server completely (not just disconnect).
echo       2. From another PC, open https://%SERVER_IP%%CONTEXT_ROOT%/
echo.
echo   That is the exact scenario that used to kill it after 2-3 days.
echo ==========================================================
goto :end

:health_bad
REM healthcheck.ps1 has already printed the per-item results and the ordered
REM troubleshooting list, so don't repeat it here.
echo ==========================================================
echo   DEPLOYED, BUT THE SITE DID NOT ANSWER
echo ==========================================================
echo   The code was compiled and deployed and the services were
echo   installed, but nothing answered on
echo       https://%SERVER_IP%%CONTEXT_ROOT%/
echo   See the failed rows and the checklist printed just above.
echo.
echo   You can re-run only the check (no redeploy) with:
echo       powershell -NoProfile -ExecutionPolicy Bypass -File "%APP_DIR%\healthcheck.ps1"
echo ==========================================================
goto :end

:failed
echo.
echo ==========================================================
echo   DEPLOYMENT FAILED - see the error above.
echo ==========================================================
echo   The complete output of this run, including everything
echo   that scrolled past, is in:
echo       %WHS_LOG%
echo ==========================================================
set "WHS_NOPAUSE="
pause
exit /b 1

:end
echo Finished: %date% %time%
echo Full log: %WHS_LOG%
set "WHS_NOPAUSE="
pause
exit /b 0

REM ==========================================================================
REM  :run_step - run a sub-script, show its output live, log it, and return
REM              its REAL exit code.
REM
REM  The pipe into Tee-Object is what puts the output in the log file while it
REM  still appears on screen. The cost of the pipe is that %errorlevel% then
REM  belongs to PowerShell, not to the sub-script - so each sub-script writes
REM  its own result into WHS_RC_FILE and we read it back here. A missing file
REM  means the script died without reaching either of its exits (killed, or the
REM  window was closed), which is a failure.
REM ==========================================================================
:run_step
if exist "%WHS_RC_FILE%" del "%WHS_RC_FILE%" >nul 2>&1
call "%~1" 2>&1 | powershell -NoProfile -ExecutionPolicy Bypass -Command "$input | Tee-Object -FilePath '%WHS_LOG%' -Append"
REM Read the result with a GOTO rather than "if exist ... set /p X=<file":
REM cmd sets up a redirection before it evaluates the IF, so that form prints a
REM "cannot find the file" error in exactly the case we are testing for.
set "STEP_RC=1"
if not exist "%WHS_RC_FILE%" goto :run_step_norc
set /p STEP_RC=<"%WHS_RC_FILE%"
goto :run_step_done

:run_step_norc
echo.
echo ERROR: %~nx1 ended without reporting a result - it was interrupted
echo        rather than finishing. On this machine the usual cause is
echo        running out of memory; see the RAM figures in STEP 0 and the
echo        tail of %WHS_LOG%.

:run_step_done
exit /b !STEP_RC!
