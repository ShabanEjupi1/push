# TLS certificates for the warehouse server

The site is served over HTTPS by **nginx** (see [../SSL_SETUP.md](../SSL_SETUP.md)).
There are two certificate modes, chosen by `NGINX_CERT_MODE` in
[../deploy.bat](../deploy.bat).

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

## Mode 1 — `selfsigned` (default, works today)

`NGINX_CERT_MODE=selfsigned`. deploy.bat auto-generates a self-signed cert for
`10.10.10.7`. HTTPS works, but browsers show a one-time "not trusted" warning
because no authority vouches for it.

## Mode 2 — `cacert` (trusted, no warning)

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
5. In [../deploy.bat](../deploy.bat) set `NGINX_CERT_MODE=cacert` and re-run it.

deploy.bat then imports the CA + issued cert into the keystore, builds
`fullchain.pem` + `privkey.pem` for nginx, and you get trusted HTTPS with no
warning.

## Access / VPN

The server lives on the internal address `10.10.10.7`, so it is only reachable
from inside the Dogana network. Remote users must be on the **VPN** first; there
is nothing to configure in the app for that — it's purely network access.
