# HTTPS setup

HTTPS is automated inside [deploy.bat](deploy.bat). You run the script — that's it.

## Architecture

```
Browser ──TLS 1.2──▶ GlassFish :443   (the app)
Browser ──HTTP────▶ GlassFish :80    → 301 to https
                      GlassFish :8080 → plain HTTP, localhost only, for diagnosis
```

There is **no reverse proxy**. GlassFish terminates TLS itself. This is chosen by
one knob at the top of [deploy.bat](deploy.bat):

```bat
set "SERVE_MODE=glassfish"    REM glassfish (default) | nginx
```

`SERVE_MODE=nginx` restores the old layout (nginx on :443 proxying to GlassFish on
:8080). It is kept as a fallback for one specific reason, explained below. The same
value must be set in `deploy.bat`, `install-services.bat`, `watchdog.bat` and
`deploy-full.bat` — they each carry their own copy so any of them can be run alone.

## The one thing that makes this work: TLS 1.2 on GlassFish 3.1.2.2

GlassFish 3.1.2.2 is from 2012 and ships Grizzly 1.9, which builds the HTTPS
listener's enabled-protocol list from three flags:

| flag | means |
|---|---|
| `ssl2-enabled` | SSLv2 |
| `ssl3-enabled` | SSLv3 |
| `tls-enabled` | **TLS 1.0 only** |

There is no `tls11-enabled` / `tls12-enabled` in this version — those arrived in
GlassFish 4. So the obvious configuration (`tls-enabled=true`) produces a listener
that speaks TLS 1.0 and nothing else, and every current browser refuses it with
`ERR_SSL_VERSION_OR_CIPHER_MISMATCH`. That is the "unsupported protocol" error that
originally pushed this deployment onto nginx.

The way out: set **all three to false**. Grizzly then never calls
`setEnabledProtocols()` at all and the JSSE defaults apply — and a JDK 7 *server*
socket defaults to TLS 1.0 + 1.1 + 1.2. deploy.bat does exactly this:

```bat
asadmin set ...http-listener-2.ssl.ssl2-enabled=false
asadmin set ...http-listener-2.ssl.ssl3-enabled=false
asadmin set ...http-listener-2.ssl.tls-enabled=false
```

`tls-enabled=false` looks like it disables TLS. It does the opposite. Do not
"fix" it.

**This is verified, not assumed.** [healthcheck.ps1](healthcheck.ps1) opens a real
TLS 1.2-only handshake against :443 and prints the negotiated protocol and cipher.
If that row fails, the site is unusable from any browser no matter how green
everything else looks — and the check says so, and tells you how to fall back to
`SERVE_MODE=nginx`.

### The remaining limitation

JDK 7 has no AES-GCM cipher suites, so browsers negotiate the older AES-CBC ones.
Chrome, Edge and Firefox still accept those today; Chrome's DevTools will call the
connection "obsolete". If a browser update ever drops them, the symptom is the
same `ERR_SSL_VERSION_OR_CIPHER_MISMATCH` and the fix is `SERVE_MODE=nginx` — which
is why that mode still exists.

## What the `[SSL]` step does

Idempotently, on every run:

1. **Stops nginx** and deletes its boot task, so nothing else holds :80 / :443.
2. **Backs up** `domain1/config/keystore.jks` to `keystore.jks.bak`.
3. **Installs the certificate** into the keystore under the alias `warehouse`
   (skipped if the alias is already there — see *Replacing the certificate* below).
4. Points `http-listener-2` at that alias, moves it to **:443**, and applies the
   three protocol flags above.
5. Adds a second listener on **:80**. Staff type `10.10.10.7/warehouse` with no
   scheme, so the browser tries port 80 first; without a listener there they get
   "cannot reach this site". Everything arriving there is redirected to HTTPS by
   `HttpsSecurityFilter`.
6. Writes `domain1/docroot/index.html` so bare `https://10.10.10.7/` lands on
   `/warehouse/`.
7. Opens the Windows firewall for 80 and 443.
8. Restarts the domain.

## What replaced the nginx config

nginx was doing four things besides TLS. They now live in the application:

| nginx directive | now |
|---|---|
| `return 301 https://...` on :80 | [HttpsSecurityFilter.java](WEB-INF/classes/mk/com/snt/kc/warehouse/view/security/HttpsSecurityFilter.java), mapped first in [web.xml](WEB-INF/web.xml) |
| `add_header Strict-Transport-Security` etc. | the same filter |
| `proxy_cookie_flags ~ secure httponly` | `cookieSecure=dynamic` + `cookieHttpOnly` in [glassfish-web.xml](WEB-INF/glassfish-web.xml) |
| `location = / { return 301 /warehouse/; }` | `docroot/index.html`, written by deploy.bat |

The filter honours `X-Forwarded-Proto`, so it is correct in **both** serve modes —
which is why the redirect is not a `CONFIDENTIAL` security-constraint in web.xml.
That mechanism looks only at the container's own view of the request and would
redirect forever the moment a TLS-terminating proxy is put back in front.

## Certificates

`CERT_MODE` at the top of deploy.bat, applies to both serve modes:

- **`cacert`** (default) — a certificate issued by the Dogana internal CA
  (`dogana-EMAILSUBBMITION-CA`). Every domain PC already trusts that CA, so there
  is **no browser warning**, even for an IP-only server.
- **`selfsigned`** — generated on the spot for `10.10.10.7`. Works immediately,
  one-time warning on every client. Used automatically as a fallback when the
  `cacert` files are missing, so a deploy never ends up with no HTTPS at all.

### Getting a CA-issued certificate

1. On the server run [ssl-tools/request-cert.bat](ssl-tools/request-cert.bat).
   → creates `ssl\warehouse.p12` (the private key — never leaves the server) and
   `ssl\warehouse.csr`.
2. Open `http://<ca-server>/certsrv` → *Request a certificate* → *advanced
   certificate request* → paste the contents of `warehouse.csr`, template
   **Web Server**.
3. In *Additional Attributes* type `san:ipaddress=10.10.10.7`. Java 7 `keytool`
   cannot put a SAN in the CSR itself, and without it the browser rejects the
   name.
4. Download the issued certificate as **Base-64** and save it as
   `ssl\warehouse-server.cer`.
5. Re-run deploy.bat.

> **The mistake to avoid.** The CA page has a *"Download CA certificate"* link
> right next to the issued-certificate link. That file is the CA's own
> certificate — it is not yours, there is no matching private key here, and it
> cannot serve this site. deploy.bat now checks the subject of
> `warehouse-server.cer` and refuses it with an explanation if it is not
> `CN=10.10.10.7`.

deploy.bat then imports `dogana-ca.cer` as a trusted entry, imports the private
key from the `.p12`, and installs the issued certificate over it as the CA reply —
which is what turns the keystore entry into key + leaf + chain. The order matters:
without the CA already in the keystore, keytool refuses the reply with
*"Failed to establish chain from reply"*.

### Replacing the certificate later

The `[SSL]` step skips the import when the alias already exists, so an expired
certificate is never silently replaced. To force a new one:

```bat
keytool -delete -alias warehouse -keystore "C:\...\domain1\config\keystore.jks" -storepass changeit
```

then re-run deploy.bat. `healthcheck.ps1` prints the expiry date on every run.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Browser: `ERR_SSL_VERSION_OR_CIPHER_MISMATCH` | The TLS 1.2 row in healthcheck.ps1 is the diagnosis. See the protocol-flags section above. |
| 502 Bad Gateway | An nginx is still running in front. In `glassfish` mode there should be none — `healthcheck.ps1` flags it as a failure. |
| The domain will not start after the SSL step | Something else holds :80 or :443. `netstat -ano \| findstr ":443 :80"`, then match the PID in Task Manager. Usually nginx or IIS. |
| `Failed to establish chain from reply` | `ssl\dogana-ca.cer` is missing, or `warehouse-server.cer` was issued for a different key. Delete `warehouse.p12` + `warehouse.csr` and request again. |
| Certificate warning on client PCs | You are on `selfsigned`. Get a CA-issued cert (above), or push `warehouse-trust.crt` to Trusted Root via Group Policy. |
| Fine on the server, unreachable from other PCs | Firewall rules were skipped because deploy.bat was not run as Administrator. Use [deploy-full.bat](deploy-full.bat), which elevates itself. |
| Need to check the app with TLS out of the way | `http://127.0.0.1:8080/warehouse/` — the loopback exemption in the filter keeps that working on purpose. |

## Access / VPN

The server is at the internal address `10.10.10.7`, so it is reachable only from
inside the Dogana network. Remote users must be on the VPN first; there is nothing
to configure in the app for that.
