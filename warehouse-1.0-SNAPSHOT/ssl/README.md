# TLS certificates for the warehouse server

The site is served over HTTPS by **GlassFish itself** on port 443 — there is no
reverse proxy any more (see [../SSL_SETUP.md](../SSL_SETUP.md)). These files feed
the GlassFish keystore; in the fallback `SERVE_MODE=nginx` layout the same files
feed nginx's PEM pair instead. There are two certificate modes, chosen by
`CERT_MODE` in [../deploy.bat](../deploy.bat).

## Files in this folder

| File | What it is | Committed? |
|---|---|---|
| `dogana-ca.cer` | The **Dogana internal CA** certificate (`dogana-EMAILSUBBMITION-CA`). Public cert, no private key. Used as the trust anchor / chain. | yes |
| `warehouse.p12` | Your server's **private key** (created by `../ssl-tools/request-cert.bat`). | **no – keep secret** |
| `warehouse.csr` | The signing request you submit to the CA. | no |
| `warehouse-server.cer` | The **issued server cert** you download back from the CA. | no |

> `dogana-ca.cer` is **not** a server certificate and cannot serve HTTPS on its
> own — it has no private key and it is a CA cert, not a leaf. Its job is to be
> trusted, and to sign the server cert you request below.

## Mode 1 — `selfsigned` (fallback)

`CERT_MODE=selfsigned`. deploy.bat auto-generates a self-signed cert for
`10.10.10.7`. HTTPS works, but browsers show a one-time "not trusted" warning
because no authority vouches for it.

## Mode 2 — `cacert` (default: trusted, no warning)

Because every domain PC already trusts `dogana-EMAILSUBBMITION-CA`, a cert
**issued by that CA** is trusted automatically. To switch:

1. On the server, run `../ssl-tools/request-cert.bat`.
   → creates `warehouse.p12` (private key) + `warehouse.csr`.
2. Open `http://<ca-server>/certsrv` → Request a certificate → advanced →
   submit the contents of `warehouse.csr`, template **Web Server**.
3. In the *Additional Attributes* box add: `san:ipaddress=10.10.10.7`
   (required because the server is reached by IP, and Java 7 `keytool` can't put
   the SAN in the CSR itself).
4. Download the issued cert (Base-64) and save it here as `warehouse-server.cer`.
5. Re-run [../deploy.bat](../deploy.bat) (`CERT_MODE=cacert` is already the default).

deploy.bat then imports the CA as a trusted entry, imports the private key, and
installs the issued cert over it as the CA reply — leaving one keystore entry
holding key + leaf + chain, which is what the :443 listener serves. Result:
trusted HTTPS with no warning.

> **Do not** save the CA page's *"Download CA certificate"* file as
> `warehouse-server.cer`. It is `dogana-ca.cer` under another name — a CA
> certificate, not yours, with no private key on this server. deploy.bat checks
> the subject and refuses it, but it is the easiest wrong click on that page.

## Access / VPN

The server lives on the internal address `10.10.10.7`, so it is only reachable
from inside the Dogana network. Remote users must be on the **VPN** first; there
is nothing to configure in the app for that — it's purely network access.
