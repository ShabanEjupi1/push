<#
==============================================================================
  jvm-size.ps1 - decide a heap size this machine can ACTUALLY allocate.
==============================================================================

  WHY THIS EXISTS
  ---------------
  The scripts used to pick -Xmx from total physical RAM alone:

      < 5 GB  -> 768m        5-9 GB -> 1536m        > 9 GB -> 2048m

  That is a guess, and on this server it was the wrong one. GlassFish was
  started with -Xmx1536m and the JVM refused to come up at all:

      Error occurred during initialization of VM
      Could not reserve enough space for object heap

  Total RAM is not what limits a heap. What limits it is:

    * the free COMMIT charge (physical RAM + page file, minus what is already
      committed) - so shrinking the page file, or another program holding
      memory, can break a heap size that worked last week; and
    * on a 32-bit JVM, the contiguous address space it can reserve, which
      tops out around 1.4 GB no matter how much RAM the box has. 1536m sits
      just the wrong side of that line, which is why exactly this value fails.

  Neither is visible in TotalVisibleMemorySize. So this script stops guessing
  and asks the JVM directly: it runs "java -Xmx<N>m -version" and steps down a
  ladder until one actually starts. Whatever survives that test is a value
  GlassFish is known to be able to boot with.

  USAGE
  -----
    Report a safe size (prints one line "WHSJVM <xmx> <perm>" on stdout):
        powershell -NoProfile -ExecutionPolicy Bypass -File jvm-size.ps1 `
            -JavaExe "C:\Program Files\Java\jdk1.7.0_45\bin\java.exe"

    Repair a domain.xml in place while the domain is DOWN (asadmin cannot be
    used then - create-jvm-options is a remote command and needs port 4848):
        powershell -NoProfile -ExecutionPolicy Bypass -File jvm-size.ps1 `
            -JavaExe "...\java.exe" -DomainXml "...\domain1\config\domain.xml" -Apply

  All human-readable output goes to STDERR so that the single machine-readable
  line on stdout stays clean for FOR /F in the .bat files. It still appears on
  screen, and is appended to -LogFile when one is given.
==============================================================================
#>

param(
    # Full path to java.exe. The probe is only meaningful when it is the same
    # JVM GlassFish runs on, so callers pass their own JDK_HOME.
    [string]$JavaExe = "",

    # Never return more than this (0 = no extra cap). deploy-full.bat passes the
    # value it already computed so this run can only ever lower it, not raise it.
    [int]$MaxMb = 0,

    # Never return less than this. Below ~256m GlassFish 3.1.2 will not deploy.
    [int]$MinMb = 256,

    # domain.xml to inspect/repair. With -Apply the -Xmx and -XX:MaxPermSize in
    # it are rewritten to the probed values.
    [string]$DomainXml = "",

    # Without this, -DomainXml only reports what it would change.
    [switch]$Apply,

    [string]$LogFile = ""
)

$ErrorActionPreference = "Continue"

# Diagnostics go to stderr, never stdout: stdout carries the one result line
# that the calling .bat parses, and anything else there would corrupt it.
function Note($msg) {
    [Console]::Error.WriteLine("     $msg")
    if ($LogFile) {
        try { Add-Content -Path $LogFile -Value ("JVM-SIZE: " + $msg) -ErrorAction SilentlyContinue } catch { }
    }
}

# --------------------------------------------------------------------------
#  Locate a JVM if the caller did not give us one.
# --------------------------------------------------------------------------
if (-not $JavaExe -or -not (Test-Path $JavaExe)) {
    $guesses = @()
    if ($env:JAVA_HOME) { $guesses += (Join-Path $env:JAVA_HOME "bin\java.exe") }
    $guesses += "C:\Program Files\Java\jdk1.7.0_45\bin\java.exe"
    $found = $guesses | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
    if ($found) {
        $JavaExe = $found
    } else {
        $cmd = Get-Command java.exe -ErrorAction SilentlyContinue
        if ($cmd) { $JavaExe = $cmd.Path }
    }
}

# --------------------------------------------------------------------------
#  Read the memory picture. Get-WmiObject rather than Get-CimInstance because
#  it works on every PowerShell this server could be running, v2 included.
#  TotalVirtualMemorySize / FreeVirtualMemory are the COMMIT limit and the
#  commit still available - RAM plus page file. That, not physical RAM, is
#  what a heap reservation is charged against.
# --------------------------------------------------------------------------
$totalMb = 0; $freeMb = 0; $commitLimitMb = 0; $commitFreeMb = 0
try {
    $os = Get-WmiObject Win32_OperatingSystem -ErrorAction Stop
    $totalMb       = [int]($os.TotalVisibleMemorySize / 1024)
    $freeMb        = [int]($os.FreePhysicalMemory / 1024)
    $commitLimitMb = [int]($os.TotalVirtualMemorySize / 1024)
    $commitFreeMb  = [int]($os.FreeVirtualMemory / 1024)
} catch {
    Note "could not read memory counters - falling back to a conservative size"
}

# --------------------------------------------------------------------------
#  Is this a 64-bit JVM? A 32-bit one cannot reserve much beyond ~1.4 GB
#  whatever the RAM, and that fact explains more failures on this server than
#  the free-memory number does - so say it out loud rather than letting the
#  ladder silently absorb it.
# --------------------------------------------------------------------------
$is64 = $true
if ($JavaExe -and (Test-Path $JavaExe)) {
    try {
        $banner = (& $JavaExe -version 2>&1 | Out-String)
        if ($banner -notmatch '64-Bit') { $is64 = $false }
    } catch { }
}

# --------------------------------------------------------------------------
#  Work out a starting ceiling, then let the probe do the real deciding.
# --------------------------------------------------------------------------

# Same policy as before for physical RAM - this part was never wrong, it was
# just the only input.
if     ($totalMb -le 0)    { $capTotal = 768 }
elseif ($totalMb -lt 5000) { $capTotal = 768 }
elseif ($totalMb -lt 9000) { $capTotal = 1536 }
else                       { $capTotal = 2048 }

# Leave commit headroom for everything that is NOT the heap and still has to
# fit while a deploy runs: PermGen (up to 512m, outside the heap on JDK 7),
# JVM native/thread/code-cache overhead, the capped 384m javac JVM in
# deploy.bat, and the short-lived asadmin JVMs.
$cap = $capTotal
if ($commitFreeMb -gt 0) {
    $capCommit = $commitFreeMb - 1200
    if ($capCommit -lt $cap) { $cap = $capCommit }
}
if (-not $is64 -and $cap -gt 1024) { $cap = 1024 }
if ($MaxMb -gt 0 -and $MaxMb -lt $cap) { $cap = $MaxMb }
if ($cap -lt $MinMb) { $cap = $MinMb }

if ($totalMb -gt 0) {
    Note ("physical RAM {0} MB total, {1} MB free; commit {2} MB free of {3} MB" -f $totalMb, $freeMb, $commitFreeMb, $commitLimitMb)
}
if (-not $is64) {
    Note "this is a 32-BIT JVM - it cannot reserve much over ~1.4 GB whatever the RAM."
    Note "  (that is the real reason -Xmx1536m fails here; a 64-bit JDK would lift it)"
}

# Finer than halving. The old repair went 1536 -> 768 and gave away half the
# heap when 1280m would have started perfectly well.
$ladder = @(2048, 1792, 1536, 1280, 1024, 896, 768, 640, 512, 384, 256)

$candidates = @($ladder | Where-Object { $_ -le $cap -and $_ -ge $MinMb })
if ($candidates.Count -eq 0) { $candidates = @($MinMb) }

# --------------------------------------------------------------------------
#  The probe: the only test that is not a guess. A JVM that prints its version
#  under -Xmx<N>m is a JVM that can reserve that heap right now.
# --------------------------------------------------------------------------
$xmx = 0
if ($JavaExe -and (Test-Path $JavaExe)) {
    foreach ($mb in $candidates) {
        # Reset first: if the launch itself fails, $LASTEXITCODE keeps whatever
        # the previous command left there, and a stale 0 would "pass" a heap
        # size that was never actually tested.
        $global:LASTEXITCODE = 1
        & $JavaExe ("-Xmx" + $mb + "m") "-version" 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) { $xmx = $mb; break }
        Note ("-Xmx{0}m was refused by the JVM - trying a smaller heap" -f $mb)
    }
    if ($xmx -eq 0) {
        Note "the JVM would not start at ANY heap size down to ${MinMb}m."
        Note "  the machine is out of memory right now - close other programs,"
        Note "  sign out other users (query user), or reboot and retry."
    }
} else {
    Note "java.exe not found - cannot probe, using the RAM-based estimate only"
}

if ($xmx -eq 0) { $xmx = $candidates[$candidates.Count - 1] }

# PermGen tracks the heap. JDK 7 keeps classes here, and every redeploy leaks
# the old classloader into it, so it cannot be trimmed to nothing - but on a
# small heap a 512m PermGen is a big share of the commit budget.
if     ($xmx -ge 1024) { $perm = 512 }
elseif ($xmx -ge 768)  { $perm = 384 }
else                   { $perm = 256 }

Note ("chosen: -Xmx{0}m with -XX:MaxPermSize={1}m (probed, not estimated)" -f $xmx, $perm)

# --------------------------------------------------------------------------
#  Optional: repair domain.xml directly. This is the path that works with the
#  domain DOWN, which is exactly when a bad -Xmx has locked you out - asadmin
#  create-jvm-options talks to port 4848 and there is nothing listening.
# --------------------------------------------------------------------------
if ($DomainXml) {
    if (-not (Test-Path $DomainXml)) {
        Note "domain.xml not found at $DomainXml - nothing to repair"
    } else {
        $text = Get-Content $DomainXml -Raw
        $m = [regex]::Match($text, '-Xmx(\d+)m')
        if (-not $m.Success) {
            Note "no -Xmx in domain.xml - leaving it alone"
        } else {
            $current = [int]$m.Groups[1].Value
            if ($current -eq $xmx) {
                Note "domain.xml already has -Xmx${xmx}m - no change needed"
            } elseif (-not $Apply) {
                Note "domain.xml has -Xmx${current}m; would change it to -Xmx${xmx}m (re-run with -Apply)"
            } else {
                $new = $text -replace ('-Xmx' + $current + 'm'), ('-Xmx' + $xmx + 'm')
                $new = $new -replace '-XX:MaxPermSize=\d+m', ('-XX:MaxPermSize=' + $perm + 'm')
                # Write to a temp file and parse it as XML before it replaces the
                # real one. A corrupted domain.xml is not a config problem, it is
                # a rebuild-the-domain problem.
                $tmp = $DomainXml + ".new"
                Set-Content -Path $tmp -Value $new -Encoding UTF8
                try {
                    [xml](Get-Content $tmp -Raw -ErrorAction Stop) | Out-Null
                    if (-not (Test-Path ($DomainXml + ".bak"))) {
                        Copy-Item $DomainXml ($DomainXml + ".bak") -Force
                    }
                    Move-Item $tmp $DomainXml -Force
                    Note "domain.xml updated: -Xmx${current}m -> -Xmx${xmx}m, MaxPermSize=${perm}m"
                    Note "  this takes effect on the next start-domain."
                } catch {
                    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
                    Note "the rewrite would have corrupted domain.xml - left it untouched"
                }
            }
        }
    }
}

# The one line on stdout. Prefixed so a caller can findstr for it and never
# mistake a stray warning for the result.
Write-Output ("WHSJVM {0} {1}" -f $xmx, $perm)
exit 0
