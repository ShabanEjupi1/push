# HTTPS setup (automated, IP-only)

HTTPS is fully automated inside [deploy.bat](deploy.bat). You run the script — that's it.

## What the script does (the `[NGINX]` step)

```
Browser ──TLS 1.2/1.3──▶ nginx (:443, self-signed cert) ──plain HTTP──▶ GlassFish (:8080)
   nginx (:80) ── redirects to :443
```

On each run it will, idempotently:

1. **Download + install nginx** to `C:\nginx` (only if not already there).
2. **Generate a self-signed certificate** for `10.10.10.7` with `keytool`, then convert
   it to the PEM files nginx needs using a tiny bundled JDK helper
   ([ssl-tools/CertToPem.java](ssl-tools/CertToPem.java)) — no OpenSSL required.
3. **Write `C:\nginx\conf\nginx.conf`** from [nginx/nginx.conf.template](nginx/nginx.conf.template)
   (modern TLS only — this is what fixes the *"unsupported protocol"* error).
4. **Open the firewall** for ports 80 and 443.
5. **Start (or reload) nginx.**

GlassFish stays on plain HTTP 8080 behind the proxy. The old GlassFish SSL listener and
the `web.xml` redirect are intentionally left off so there's no redirect loop.

## After running

Open: **`https://10.10.10.7/warehouse/`**

Because the server is reached by **IP only** (no domain name), the certificate is
**self-signed** — no public authority (Let's Encrypt etc.) will ever issue a cert for a
bare IP. So browsers show a **one-time "Not secure / not trusted" warning**. The
connection itself is real, encrypted, modern TLS. Click through once, or remove the
warning entirely (see below).

## Optional: remove the browser warning on staff PCs

The script exports the public certificate to `C:\nginx\ssl\fullchain.pem`. To make it
trusted (no warning) on client machines:

- **One PC:** double-click `fullchain.pem` → Install Certificate → Local Machine →
  "Trusted Root Certification Authorities".
- **Whole Customs domain:** have IT push that cert to "Trusted Root" via Group Policy.

## Configuration knobs (top of deploy.bat)

```bat
set "SETUP_NGINX=true"            REM set false to skip the proxy step
set "NGINX_HOME=C:\nginx"
set "NGINX_VERSION=1.27.4"
set "SERVER_IP=10.10.10.7"
set "BACKEND_HOSTPORT=127.0.0.1:8080"
```

## Remove the warning properly: use a Dogana-CA cert (recommended)

Dogana runs its own internal CA (`dogana-EMAILSUBBMITION-CA`), and every domain PC
already trusts it. A certificate **issued by that CA** is therefore trusted with **no
warning** — even for an IP-only server. To switch to it:

1. On the server run [ssl-tools/request-cert.bat](ssl-tools/request-cert.bat)
   → makes `ssl\warehouse.p12` (private key) + `ssl\warehouse.csr`.
2. Submit `warehouse.csr` at `http://<ca-server>/certsrv` (Web Server template),
   add attribute `san:ipaddress=10.10.10.7`, download the reply as `ssl\warehouse-server.cer`.
3. Set `NGINX_CERT_MODE=cacert` in [deploy.bat](deploy.bat) and re-run.

Full details: [ssl/README.md](ssl/README.md). The nginx proxy itself is unchanged;
only the certificate source differs.

## If you later get a domain name

A real *public* domain would also unlock a free, trusted, auto-renewing Let's Encrypt
cert. On the internal-only IP setup, the Dogana-CA cert above is the right choice.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `Could not download nginx` | Server has no internet. Download the nginx Windows zip manually, unzip to `C:\nginx`, re-run. |
| `nginx config test failed` | Run `C:\nginx\nginx.exe -t` for the exact line; usually the cert files aren't in `C:\nginx\ssl`. |
| 502 Bad Gateway | GlassFish/app not up on 8080. Check `http://127.0.0.1:8080/warehouse/`. |
| Port 443 in use | Something else (IIS?) holds 443. Stop it, or change nginx `listen`. |
| Still see old broken HTTPS | You're hitting GlassFish `:8181` directly. Use `https://10.10.10.7/warehouse/` (port 443, nginx). |
