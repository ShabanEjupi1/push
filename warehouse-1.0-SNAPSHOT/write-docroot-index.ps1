<#
==============================================================================
  write-docroot-index.ps1 - put a redirect page in the GlassFish docroot.
==============================================================================

  WHY THIS EXISTS
  ---------------
  Staff type "10.10.10.7" with no path. nginx used to answer that with a 301
  to /warehouse/; in glassfish mode GlassFish answers it from the domain
  docroot, which is empty, so they get a bare directory listing / 404 and
  assume the server is down.

  Dropping an index.html there is the cheap fix - far less risky than making
  the app the virtual server's default-web-module, which changes how every
  context path resolves.

  WHY IT IS A SEPARATE .PS1 AND NOT AN INLINE "powershell -Command" IN
  deploy.bat
  --------------------------------------------------------------------------
  It used to be inline, built out of five caret-continued, double-quoted
  fragments. cmd joins those into one command line and PowerShell then
  reassembles them, and on the server that reassembly lost everything up to
  the final fragment - PowerShell received a command starting mid-way through
  the Set-Content call and died with:

      Unexpected token '-Value' in expression or statement.

  The page was never written, and because the line ran under "if exist" with
  no error check the deploy carried on and reported success. HTML is made
  almost entirely of the characters cmd treats specially, so the fragile
  construct and the content were a bad match. A -File script takes its
  arguments through argv, where none of that applies.

  USAGE
  -----
      powershell -NoProfile -ExecutionPolicy Bypass -File write-docroot-index.ps1 `
          -Docroot "C:\...\glassfish3\glassfish\domains\domain1\docroot" `
          -ContextRoot "/warehouse"

  EXIT CODES
  ----------
    0 = the page is in place (written, or already correct)
    1 = the docroot exists but the page could not be written
==============================================================================
#>

param(
    [string]$Docroot = "",
    [string]$ContextRoot = "/warehouse"
)

$ErrorActionPreference = "Continue"

function Note($msg) { Write-Host "   $msg" }

if (-not $Docroot) {
    Note "no -Docroot given - skipping the docroot redirect page"
    exit 0
}
if (-not (Test-Path $Docroot)) {
    Note "docroot not found at $Docroot - skipping the redirect page"
    exit 0
}

# "/warehouse" and "warehouse" and "/warehouse/" all have to end up as the same
# URL, or the meta refresh points at a path that does not exist.
$target = "/" + $ContextRoot.Trim().Trim("/") + "/"

$html = @"
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="refresh" content="0;url=$target">
<title>Warehouse</title>
</head>
<body>
<p><a href="$target">Warehouse</a></p>
</body>
</html>
"@

$index = Join-Path $Docroot "index.html"

# Only write when the content differs. The docroot is served by a running
# server; rewriting an identical file on every deploy is pure risk for no gain.
if (Test-Path $index) {
    try {
        if ((Get-Content $index -Raw -ErrorAction Stop) -eq $html) {
            Note "docroot index.html already redirects to $target"
            exit 0
        }
    } catch { }
}

try {
    # ASCII, and written through .NET rather than Set-Content: this file is
    # read by browsers that were told it is utf-8, and Set-Content -Encoding
    # UTF8 would put a BOM in front of the doctype.
    [System.IO.File]::WriteAllText($index, $html, (New-Object System.Text.ASCIIEncoding))
} catch {
    Note "could not write $index : $($_.Exception.Message)"
    exit 1
}

Note "docroot index.html now redirects to $target"
exit 0
