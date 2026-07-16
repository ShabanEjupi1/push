@echo off
setlocal
REM ==========================================================
REM   Request a TLS certificate from the Dogana internal CA
REM ==========================================================
REM Run this ONCE on the warehouse server. It creates:
REM     ssl\warehouse.p12  - the private key (KEEP THIS - never share it)
REM     ssl\warehouse.csr  - the request you submit to the CA
REM
REM Then:
REM   1. Open  http://<ca-server>/certsrv  (the Dogana CA web enrollment).
REM   2. Request a certificate -> advanced request -> submit a base-64 CSR.
REM      Paste the contents of ssl\warehouse.csr. Template: "Web Server".
REM   3. IMPORTANT (IP-only server): in the "Additional Attributes" box type:
REM          san:ipaddress=10.10.10.7
REM      (Java 7 keytool cannot put the SAN in the CSR itself, so it must be
REM       added here or the browser will complain the cert name doesn't match.)
REM   4. Download the issued cert as Base-64 and save it as:
REM          ssl\warehouse-server.cer
REM   5. In deploy.bat set   NGINX_CERT_MODE=cacert   and re-run deploy.bat.
REM ==========================================================

REM --- Must match deploy.bat ---
set "JDK_HOME=C:\Program Files\Java\jdk1.7.0_45"
set "SERVER_IP=10.10.10.7"

set "SCRIPT_DIR=%~dp0"
REM Resolve the project ssl\ folder to an ABSOLUTE path (so "..\ssl" can't
REM silently land somewhere unexpected when the .bat is run from elsewhere).
pushd "%SCRIPT_DIR%.." >nul 2>&1
if errorlevel 1 (
    echo ERROR: cannot locate the project folder above "%SCRIPT_DIR%".
    echo Run this script from inside ...\warehouse-1.0-SNAPSHOT\ssl-tools\.
    goto :end
)
set "APP_DIR=%CD%"
popd >nul
set "SSL_DIR=%APP_DIR%\ssl"
echo Files will be written to: %SSL_DIR%
echo.
set "P12=%SSL_DIR%\warehouse.p12"
set "CSR=%SSL_DIR%\warehouse.csr"
set "KEYTOOL=%JDK_HOME%\bin\keytool.exe"

if not exist "%KEYTOOL%" (
    echo ERROR: keytool.exe not found at "%KEYTOOL%". Fix JDK_HOME above.
    goto :end
)
if not exist "%SSL_DIR%" mkdir "%SSL_DIR%"

if exist "%P12%" (
    echo A keystore already exists at "%P12%".
    echo Delete it first if you really want to generate a brand-new key. Aborting.
    goto :end
)

echo Generating a 2048-bit private key for %SERVER_IP% ...
"%KEYTOOL%" -genkeypair -alias warehouse -keyalg RSA -keysize 2048 -validity 3650 ^
    -dname "CN=%SERVER_IP%, O=Dogana e Kosoves, C=XK" -ext "SAN=IP:%SERVER_IP%" ^
    -storetype PKCS12 -keystore "%P12%" -storepass changeit -keypass changeit
if errorlevel 1 (
    echo ERROR: key generation failed.
    goto :end
)

echo Generating the certificate signing request (CSR) ...
"%KEYTOOL%" -certreq -alias warehouse -file "%CSR%" ^
    -keystore "%P12%" -storetype PKCS12 -storepass changeit
if errorlevel 1 (
    echo ERROR: CSR generation failed.
    goto :end
)

if not exist "%P12%" (
    echo ERROR: expected key file was not created at "%P12%".
    goto :end
)
if not exist "%CSR%" (
    echo ERROR: expected CSR was not created at "%CSR%".
    goto :end
)

echo.
echo ==========================================================
echo Done.  Both files verified in: %SSL_DIR%
echo   Private key : %P12%   (keep safe, do not share)
echo   CSR to submit: %CSR%
echo.
echo Next: submit the CSR to http://^<ca^>/certsrv (Web Server template),
echo       add attribute  san:ipaddress=%SERVER_IP%  , download the reply as
echo       Base-64, save it as ssl\warehouse-server.cer, then set
echo       NGINX_CERT_MODE=cacert in deploy.bat and re-run it.
echo ==========================================================

:end
pause
endlocal
