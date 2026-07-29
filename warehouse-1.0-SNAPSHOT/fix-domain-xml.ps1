<#
==============================================================================
  fix-domain-xml.ps1 - make domain.xml readable by GlassFish again.
==============================================================================

  WHY THIS EXISTS
  ---------------
  The domain stopped starting with:

      com.sun.enterprise.universal.xml.MiniXmlParserException:
      "Xml Parser Error: javax.xml.stream.XMLStreamException:
       ParseError at [row,col]:[1,1]
       Message: Content is not allowed in prolog."

  That message means: there are bytes in front of the "<?xml" declaration.
  Almost always those bytes are a UTF-8 byte-order mark (EF BB BF), and it got
  there because the repair scripts rewrote domain.xml with

      Set-Content -Encoding UTF8

  Windows PowerShell's "UTF8" ALWAYS writes a BOM. Normally that is harmless -
  but GlassFish 3.1.2's MiniXmlParser opens domain.xml through an
  InputStreamReader, i.e. as characters, not bytes. StAX only strips a BOM when
  it is handed a byte stream; given a Reader the BOM survives as a real U+FEFF
  character sitting in front of the prolog, and the parse dies at [1,1].

  The nasty part is that this is invisible to the checks the scripts were
  making: .NET's [xml] cast skips a BOM happily, so the rewritten file was
  validated, declared good, and shipped - and only GlassFish disagreed. Hence a
  separate check that looks at BYTES, which is the only level at which the
  problem exists.

  WHAT IT DOES
  ------------
    1. Reads domain.xml as raw bytes and removes anything before the first '<'
       (BOM, stray whitespace, or junk from a half-written rewrite).
    2. Writes it back as UTF-8 WITHOUT a BOM, via a temp file, keeping a
       one-time .bak of the original.
    3. Confirms the result parses. If it does not, the file is damaged beyond a
       prolog problem, so domain.xml.bak is restored (and sanitised too).

  It is safe to run at any time and does nothing when the file is already
  clean. Run it by hand with:

      powershell -NoProfile -ExecutionPolicy Bypass -File fix-domain-xml.ps1 `
          -DomainXml "C:\...\glassfish3\glassfish\domains\domain1\config\domain.xml"

  EXIT CODES
  ----------
    0 = the file is usable (either already clean, or repaired)
    1 = the file is unusable and could not be repaired - GlassFish will not
        start until it is fixed by hand
==============================================================================
#>

param(
    [string]$DomainXml = "",
    [string]$LogFile = ""
)

$ErrorActionPreference = "Continue"

function Note($msg) {
    Write-Host "     $msg"
    if ($LogFile) {
        try { Add-Content -Path $LogFile -Value ("FIX-DOMAIN-XML: " + $msg) -ErrorAction SilentlyContinue } catch { }
    }
}

# Strip anything before the first '<' from a file, in place. Used on a file we
# have just restored from .bak: that backup was taken BEFORE the prolog was
# cleaned, so it can carry the very BOM we are removing, and putting it back
# untouched would restore the fault along with the file.
function Remove-PrologJunk($path) {
    try { $b = [System.IO.File]::ReadAllBytes($path) } catch { return $false }
    $k = -1
    for ($j = 0; $j -lt $b.Length -and $j -lt 512; $j++) {
        if ($b[$j] -eq 0x3C) { $k = $j; break }
    }
    if ($k -lt 0) { return $false }
    if ($k -eq 0) { return $true }
    $out = New-Object byte[] ($b.Length - $k)
    [System.Array]::Copy($b, $k, $out, 0, $out.Length)
    [System.IO.File]::WriteAllBytes($path, $out)
    Note "also removed $k byte(s) in front of the restored file's declaration"
    return $true
}

if (-not $DomainXml) {
    Note "no -DomainXml given - nothing to check"
    exit 0
}
if (-not (Test-Path $DomainXml)) {
    Note "domain.xml not found at $DomainXml - nothing to check"
    exit 0
}
# [System.IO.File] uses the process working directory, which is not PowerShell's
# - so a relative path typed by hand would read or write the wrong file.
$DomainXml = (Resolve-Path $DomainXml).Path

# --------------------------------------------------------------------------
#  Read as bytes. Get-Content would silently swallow exactly the byte we are
#  hunting for, which is how this went unnoticed for so long.
# --------------------------------------------------------------------------
try {
    $bytes = [System.IO.File]::ReadAllBytes($DomainXml)
} catch {
    Note "could not read domain.xml: $($_.Exception.Message)"
    exit 1
}

if ($bytes.Length -eq 0) {
    Note "domain.xml is EMPTY (0 bytes) - a rewrite was interrupted part way."
    $restored = $false
    if (Test-Path ($DomainXml + ".bak")) {
        Copy-Item ($DomainXml + ".bak") $DomainXml -Force
        Note "restored it from domain.xml.bak"
        $restored = $true
        $bytes = [System.IO.File]::ReadAllBytes($DomainXml)
    }
    if (-not $restored) {
        Note "no domain.xml.bak to restore from - the domain cannot start."
        exit 1
    }
}

# --------------------------------------------------------------------------
#  Locate the real start of the document: the first '<'. Everything before it
#  is what GlassFish is complaining about.
# --------------------------------------------------------------------------
$lt = -1
for ($i = 0; $i -lt $bytes.Length -and $i -lt 512; $i++) {
    if ($bytes[$i] -eq 0x3C) { $lt = $i; break }   # '<'
}

if ($lt -lt 0) {
    Note "domain.xml contains no XML at all in its first bytes - it is not a"
    Note "  domain.xml any more. Restore domain.xml.bak by hand, or recreate the"
    Note "  domain with: asadmin create-domain domain1"
    exit 1
}

# A UTF-16 file would show '<' at offset 2 with a 00 next to it. GlassFish 3.1.2
# does not read those, and no script here writes one, but say so plainly rather
# than "repairing" it into nonsense.
if ($bytes.Length -gt ($lt + 1) -and $bytes[$lt + 1] -eq 0x00) {
    Note "domain.xml looks like UTF-16 - GlassFish 3.1.2 cannot read that."
    Note "  Restore domain.xml.bak, or re-save the file as UTF-8 without a BOM."
    exit 1
}

if ($lt -eq 0) {
    # Nothing in front of the prolog. Still confirm it parses, because an
    # interrupted rewrite truncates the END of the file, which looks fine here.
    try {
        [xml](Get-Content $DomainXml -Raw -ErrorAction Stop) | Out-Null
        exit 0
    } catch {
        Note "domain.xml starts correctly but does not parse - it is truncated or"
        Note "  malformed: $($_.Exception.Message)"
        if (Test-Path ($DomainXml + ".bak")) {
            Copy-Item ($DomainXml + ".bak") $DomainXml -Force
            Note "restored it from domain.xml.bak - re-run this script to verify."
            Remove-PrologJunk $DomainXml | Out-Null
            exit 0
        }
        Note "no domain.xml.bak to restore from - the domain cannot start."
        exit 1
    }
}

# --------------------------------------------------------------------------
#  There IS junk in front. Name it, because "BOM" and "leftover garbage" call
#  for different levels of worry.
# --------------------------------------------------------------------------
if ($lt -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    Note "domain.xml starts with a UTF-8 BOM. That is the 'Content is not allowed"
    Note "  in prolog' error - GlassFish reads this file as characters, so the BOM"
    Note "  is not skipped. Removing it."
} else {
    Note "domain.xml has $lt stray byte(s) before the '<?xml' declaration - that is"
    Note "  the 'Content is not allowed in prolog' error. Removing them."
}

$clean = New-Object byte[] ($bytes.Length - $lt)
[System.Array]::Copy($bytes, $lt, $clean, 0, $clean.Length)

# One-time backup of whatever was there before we touch it.
if (-not (Test-Path ($DomainXml + ".bak"))) {
    Copy-Item $DomainXml ($DomainXml + ".bak") -Force
    Note "kept the original as domain.xml.bak"
}

# Temp file first: a domain.xml half-written by an interrupted script is the
# failure this whole script exists to clean up, so do not create another one.
$tmp = $DomainXml + ".fixed"
try {
    [System.IO.File]::WriteAllBytes($tmp, $clean)
    [xml](Get-Content $tmp -Raw -ErrorAction Stop) | Out-Null
} catch {
    Remove-Item $tmp -Force -ErrorAction SilentlyContinue
    Note "with the prolog junk removed the file still does not parse: $($_.Exception.Message)"
    if (Test-Path ($DomainXml + ".bak")) {
        Copy-Item ($DomainXml + ".bak") $DomainXml -Force
        Remove-PrologJunk $DomainXml | Out-Null
        try {
            [xml](Get-Content $DomainXml -Raw -ErrorAction Stop) | Out-Null
            Note "restored domain.xml.bak - it parses, so the domain can start again."
            Note "  NOTE: the backup predates any later configuration changes."
            exit 0
        } catch {
            Note "domain.xml.bak does not parse either - both copies are damaged."
            Note "  Recreate the domain: asadmin create-domain domain1"
            exit 1
        }
    }
    exit 1
}

Move-Item $tmp $DomainXml -Force
Note "domain.xml repaired - it now starts at '<?xml' with no BOM."
exit 0
