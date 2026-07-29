<#
    Warehouse deployment health check.

    Called by deploy-full.bat as the last step, but also useful on its own at
    any time to answer "is the warehouse app actually up?":

        powershell -NoProfile -ExecutionPolicy Bypass -File healthcheck.ps1

    Exit code 0 = the site answered. Exit code 1 = it did not.

    This lives in its own .ps1 rather than inline in the .bat on purpose:
    multi-line PowerShell inside a batch for /f loop gets mangled by cmd's
    caret and quote handling, which is a classic source of "works on my
    machine" deployment scripts.
#>
param(
    [string]$ServerIp    = "10.10.10.7",
    [string]$ContextRoot = "/warehouse",
    [string]$GlassfishRoot = "C:\Users\Warehouse\Desktop\Glassfish_3.1.2.2\glassfish3",
    [string]$NginxHome   = "C:\nginx",
    # Must match SERVE_MODE in deploy.bat. "glassfish" = GlassFish terminates TLS
    # on :443 itself; "nginx" = the old reverse-proxy layout.
    [ValidateSet("glassfish","nginx")]
    [string]$ServeMode   = "glassfish",
    [int]   $TimeoutSeconds = 90,
    # When set, everything printed here is appended to this file as well.
    # deploy-full.bat points it at deploy-full.log so one file holds the whole run.
    [string]$LogFile     = ""
)

$ErrorActionPreference = "SilentlyContinue"

if ($LogFile) { Start-Transcript -Path $LogFile -Append | Out-Null }
function Finish {
    param([int]$Code)
    if ($LogFile) { Stop-Transcript | Out-Null }
    exit $Code
}

# Is something listening on a local port?
#
# Get-NetTCPConnection is the obvious cmdlet and it is used when it exists, but
# it ships with the NetTCPIP module - Windows 8 / Server 2012 and later. On
# Server 2008 R2 it is simply absent, and because this script sets
# $ErrorActionPreference = "SilentlyContinue" its absence is SILENT: every port
# reads as free and the health check fails a site that is running perfectly.
# deploy.bat hit this and settled on TcpClient (see :is_port_open there); the
# same fallback belongs here, so both scripts answer the question the same way.
$script:HasNetTcp = [bool](Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue)
function Test-PortListening {
    param([int]$Port)
    if ($script:HasNetTcp) {
        return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    }
    $c = New-Object Net.Sockets.TcpClient
    try {
        $c.Connect("127.0.0.1", $Port)
        $up = $c.Connected
        $c.Close()
        return $up
    } catch {
        return $false
    }
}

function Write-Row {
    param([string]$Label, [string]$Value, [bool]$Good)
    $mark = if ($Good) { "  OK  " } else { " FAIL " }
    $colour = if ($Good) { "Green" } else { "Red" }
    Write-Host ("[" + $mark + "] ") -NoNewline -ForegroundColor $colour
    Write-Host ($Label.PadRight(22) + ": " + $Value)
}

$modeNote = if ($ServeMode -eq "glassfish") { "GlassFish serves TLS on :443 directly" }
            else { "nginx :443 -> GlassFish :8080" }

Write-Host ""
Write-Host "---------------- health check ----------------"
Write-Host "mode: $ServeMode  ($modeNote)"

$allGood = $true

# ---------------------------------------------------------------- GlassFish --
# Match on the service binary (domain1Service.exe) or the plain name. The old
# filter also required "glassfish" in the path, which quietly found nothing on
# an install whose folder is not spelled that way - and then reported the
# service as missing when it was there.
$svc = Get-CimInstance Win32_Service |
       Where-Object { $_.PathName -like "*domain1Service*" -or $_.Name -eq "domain1" } |
       Select-Object -First 1
if ($svc) {
    $good = ($svc.State -eq "Running")
    # "Auto" is what makes it come back after a reboot; without it the whole
    # point of installing the service is lost.
    if ($svc.StartMode -ne "Auto") { $good = $false }
    Write-Row "GlassFish service" ("$($svc.State), start=$($svc.StartMode)") $good
    if (-not $good) { $allGood = $false }
} else {
    # install-services.bat falls back to a boot task when the service cannot be
    # registered. That survives a logoff just as well, so it is a pass; what is
    # not a pass is neither of them existing.
    $null = schtasks /query /tn "Warehouse GlassFish" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Row "GlassFish autostart" "boot task (no service) - OK" $true
    } else {
        Write-Row "GlassFish service" "NOT INSTALLED - app will die on logoff" $false
        $allGood = $false
    }
}

# -------------------------------------------------------------------- nginx --
$ngx = @(Get-Process nginx -ErrorAction SilentlyContinue)
if ($ServeMode -eq "nginx") {
    if ($ngx.Count) {
        Write-Row "nginx process" "running ($($ngx.Count) worker/master)" $true
    } else {
        Write-Row "nginx process" "NOT RUNNING" $false
        $allGood = $false
    }
} else {
    # In glassfish mode a running nginx is not a bonus - it is the thing that
    # takes :443 away from GlassFish at the next reboot.
    if ($ngx.Count) {
        Write-Row "nginx process" "RUNNING but should not be - it will steal :443" $false
        $allGood = $false
    } else {
        Write-Row "nginx process" "not running (correct for this mode)" $true
    }
}

# ----------------------------------------------------------- scheduled tasks --
$tasks = @("Warehouse Watchdog")
if ($ServeMode -eq "nginx") { $tasks += "Warehouse nginx" }
foreach ($t in $tasks) {
    $null = schtasks /query /tn $t 2>$null
    $ok = ($LASTEXITCODE -eq 0)
    Write-Row ("task '" + $t + "'") $(if ($ok) { "registered" } else { "MISSING" }) $ok
    if (-not $ok) { $allGood = $false }
}

# ------------------------------------------------------------------- ports ---
# In glassfish mode all three belong to GlassFish: 443 serves, 80 redirects,
# 8080 is the plain-HTTP listener kept for local diagnosis. In nginx mode 80 and
# 443 are nginx and 8080 is the backend.
foreach ($p in @(80, 443, 8080)) {
    $ok = Test-PortListening -Port $p
    Write-Row ("port $p") $(if ($ok) { "listening" } else { "NOT LISTENING" }) $ok
    if (-not $ok) { $allGood = $false }
}

# ------------------------------------------------------------------ TLS 1.2 --
# The row that decides whether glassfish mode is viable on this box at all.
#
# GlassFish 3.1.2.2 ships a Grizzly whose "tls-enabled" flag means TLS 1.0 ONLY,
# and every current browser refuses TLS 1.0 - so the site can answer this script
# perfectly (PowerShell will happily speak 1.0) and still show
# ERR_SSL_VERSION_OR_CIPHER_MISMATCH to every user. Handshake with TLS 1.2
# specifically and report what was actually negotiated, rather than trusting the
# configuration to mean what it says.
$tlsOk = $false
$tlsDetail = "no handshake"
$certLine = $null
$certSelfSigned = $false
# 3072 is SslProtocols.Tls12, written as a number rather than by name so this
# still parses on a .NET where the enum member does not exist - in which case the
# handshake fails and says so, instead of the script dying with a type error.
$tls12 = [System.Security.Authentication.SslProtocols]3072
try {
    $client = New-Object Net.Sockets.TcpClient
    $client.Connect($ServerIp, 443)
    $stream = New-Object Net.Security.SslStream($client.GetStream(), $false,
                  ([Net.Security.RemoteCertificateValidationCallback] { $true }))
    $stream.AuthenticateAsClient($ServerIp, $null, $tls12, $false)
    $tlsOk = $true
    $tlsDetail = "$($stream.SslProtocol), $($stream.CipherAlgorithm) $($stream.CipherStrength)-bit"

    # Expiry and subject come off the same handshake. An expired or wrong-subject
    # certificate looks identical from a browser to a dead server.
    $cert = New-Object Security.Cryptography.X509Certificates.X509Certificate2($stream.RemoteCertificate)
    $daysLeft = [int]($cert.NotAfter - (Get-Date)).TotalDays
    $certLine = "$($cert.Subject) - expires $($cert.NotAfter.ToString('yyyy-MM-dd')) ($daysLeft days)"
    $certSelfSigned = ($cert.Subject -eq $cert.Issuer)
    $stream.Dispose(); $client.Close()
} catch {
    $inner = $_.Exception.InnerException
    $msg = if ($inner) { $inner.Message } else { $_.Exception.Message }
    $tlsDetail = "TLS 1.2 handshake failed - $msg"
}
Write-Row "TLS 1.2 on :443" $tlsDetail $tlsOk
if (-not $tlsOk) { $allGood = $false }
if ($certLine) {
    Write-Host ("[ INFO ] " + "certificate".PadRight(22) + ": $certLine")
    if ($certSelfSigned) {
        Write-Host "         Self-signed - every client shows a warning until it is trusted."
    }
}

# -------------------------------------------------------------- the real test --
# Everything above can look right while the app still returns an error page,
# so the thing that actually decides success is an HTTPS request.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
# The cert is issued by the internal Dogana CA. Whether THIS box happens to
# trust it is a separate question from whether the app is up, so don't let
# certificate validation decide the health verdict.
[Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

$url = "https://$ServerIp$ContextRoot/"
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$status = $null
$errStatus = $null

Write-Host ""
Write-Host "Polling $url (up to $TimeoutSeconds s)..." -NoNewline
while ((Get-Date) -lt $deadline) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 400) { $status = $r.StatusCode; break }
    } catch {
        # A 302 to the login page arrives here as an exception in some
        # PowerShell versions; treat any real HTTP status as "it answered".
        $resp = $_.Exception.Response
        if ($resp -and $resp.StatusCode) {
            $code = [int]$resp.StatusCode
            if ($code -ge 200 -and $code -lt 400) { $status = $code; break }
            # 4xx/5xx is still an answer: GlassFish is up, listening and
            # speaking TLS, and the APP is what is broken. Remember it so the
            # summary can say so instead of "no answer", which reads as a
            # network/TLS fault and sends you off checking ports for nothing.
            # Keep polling - a 503 during deployment turns into a 200 shortly.
            $errStatus = $code
        }
    }
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 5
}
Write-Host ""

if ($status) {
    Write-Row "https app response" "HTTP $status" $true
} elseif ($errStatus) {
    Write-Row "https app response" "HTTP $errStatus - server answered, app failed" $false
    $allGood = $false
} else {
    Write-Row "https app response" "no answer in ${TimeoutSeconds}s" $false
    $allGood = $false
}

Write-Host "----------------------------------------------"
Write-Host ""

# ------------------------------------------------------------------ memory --
# This box is a VM with very little RAM, and that is the difference between a
# deployment that finishes and one that stops half way. These rows are
# informational - low memory does not make the health verdict fail, it explains
# failures elsewhere - so print the numbers whether or not anything is wrong.
$os = Get-CimInstance Win32_OperatingSystem
if ($os) {
    $totalMb = [int]($os.TotalVisibleMemorySize / 1024)
    $freeMb  = [int]($os.FreePhysicalMemory / 1024)
    $usedPct = if ($totalMb) { [int](100 - ($freeMb * 100 / $totalMb)) } else { 0 }
    Write-Host ("[ INFO ] " + "memory".PadRight(22) + ": $freeMb MB free of $totalMb MB ($usedPct% used)")

    foreach ($name in @("java", "nginx")) {
        $procs = @(Get-Process $name -ErrorAction SilentlyContinue)
        if ($procs.Count) {
            $mb = [int](($procs | Measure-Object -Property WorkingSet64 -Sum).Sum / 1MB)
            Write-Host ("[ INFO ] " + "  $name".PadRight(22) + ": $mb MB across $($procs.Count) process(es)")
        }
    }
    if ($freeMb -lt 400) {
        Write-Host "         Free memory is very low - expect deployments to be killed" -ForegroundColor Yellow
        Write-Host "         part way through until this is dealt with." -ForegroundColor Yellow
    }
}

# "It stopped on its own, I think the VM ran out of RAM" is a guess until this
# says so. Windows logs a Resource-Exhaustion-Detector event when it runs out of
# commit charge, naming the processes it took the memory from - that is the
# difference between knowing and assuming.
$rex = @(Get-WinEvent -FilterHashtable @{
             LogName      = 'System'
             ProviderName = 'Microsoft-Windows-Resource-Exhaustion-Detector'
             StartTime    = (Get-Date).AddDays(-7)
         } -ErrorAction SilentlyContinue)
if ($rex.Count) {
    Write-Host ("[ WARN ] " + "memory exhaustion".PadRight(22) + ": $($rex.Count) event(s) in the last 7 days") -ForegroundColor Yellow
    Write-Host ("         most recent: " + $rex[0].TimeCreated)
    # The message names the top consumers at the time, which is the whole point.
    foreach ($line in ($rex[0].Message -split "`n" | Select-Object -First 3)) {
        if ($line.Trim()) { Write-Host ("         " + $line.Trim()) }
    }
} else {
    Write-Host ("[ INFO ] " + "memory exhaustion".PadRight(22) + ": no Windows out-of-memory events in 7 days")
}

# An OutOfMemoryError in the log is the definitive answer to "did it run out of
# memory?", so look rather than guess. It survives the restart that hides the
# symptom, which is why it is worth surfacing here on every check.
$gfLog = "$GlassfishRoot\glassfish\domains\domain1\logs\server.log"
if (Test-Path $gfLog) {
    $oom = @(Get-Content $gfLog -Tail 2000 -ErrorAction SilentlyContinue |
             Select-String -Pattern "OutOfMemoryError" -SimpleMatch)
    if ($oom.Count) {
        Write-Host ("[ WARN ] " + "server.log".PadRight(22) + ": $($oom.Count) OutOfMemoryError(s) in the recent log") -ForegroundColor Yellow
        Write-Host ("         last: " + $oom[-1].Line.Trim())
        Write-Host "         Run install-services.bat - it sizes the heap and PermGen"
        Write-Host "         for this machine's RAM and restarts the domain."
    }
}
Write-Host ""

if (-not $status) {
    Write-Host "The site did not answer. Check in this order:" -ForegroundColor Yellow
    Write-Host "  1. Direct to GlassFish:  http://127.0.0.1:8080$ContextRoot/"
    Write-Host "     That listener is plain HTTP and is exempt from the HTTPS"
    Write-Host "     redirect on purpose, so it separates 'the app is broken'"
    Write-Host "     from 'TLS is broken'. If it fails, the app is the problem:"
    Write-Host "     $gfLog"
    if ($ServeMode -eq "glassfish") {
        Write-Host "  2. Is anything else holding the ports?"
        Write-Host "     netstat -ano | findstr `":443 :80`"   then match the PID in Task Manager."
        Write-Host "     nginx and IIS are the usual culprits."
        Write-Host "  3. Certificate in the keystore:"
        Write-Host "     keytool -list -v -alias warehouse -keystore `"$GlassfishRoot\glassfish\domains\domain1\config\keystore.jks`" -storepass changeit"
        Write-Host "  4. Memory - see the rows above. A JVM that cannot get memory"
        Write-Host "     starts, logs, and then serves nothing."
    } else {
        Write-Host "  2. nginx config test:    $NginxHome\nginx.exe -t"
        Write-Host "  3. nginx errors:         $NginxHome\logs\error.log"
        Write-Host "  4. Certificate files:    $NginxHome\ssl\fullchain.pem + privkey.pem"
        Write-Host "  5. Memory - see the rows above. A JVM that cannot get memory"
        Write-Host "     starts, logs, and then serves nothing."
    }
    Write-Host ""
}

# A site that answers this script but fails the TLS 1.2 row is the specific
# failure mode of serving TLS from GlassFish 3.1.2.2, and it is invisible from
# the server itself - so say what to do about it rather than leaving one red row.
if ($ServeMode -eq "glassfish" -and -not $tlsOk) {
    Write-Host "TLS 1.2 is not working on :443." -ForegroundColor Yellow
    Write-Host "  Browsers will refuse to open the site even if everything else is green."
    Write-Host "  1. Confirm the three protocol flags are all false (see deploy.bat):"
    Write-Host "     asadmin get configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.*-enabled"
    Write-Host "     ssl2-enabled, ssl3-enabled and tls-enabled must ALL be false. In this"
    Write-Host "     GlassFish version tls-enabled=true means TLS 1.0 only, and all-false is"
    Write-Host "     what makes it fall back to the JDK defaults, which include TLS 1.2."
    Write-Host "  2. Re-run deploy.bat (it sets them) and restart the domain."
    Write-Host "  3. If it still fails, this JDK cannot serve TLS 1.2 for this Grizzly."
    Write-Host "     Set SERVE_MODE=nginx in deploy.bat, install-services.bat and"
    Write-Host "     watchdog.bat and re-run deploy-full.bat - that puts the proxy back."
    Write-Host ""
}

if ($allGood) { Finish 0 }
# The app answering is what decides deployment success; the other rows are
# warnings that something will break later (typically at the next logoff).
if ($status) { Finish 0 }
Finish 1
